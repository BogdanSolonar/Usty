package com.planning.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.planning.backend.entity.Couple;
import com.planning.backend.entity.PartnerRole;
import com.planning.backend.entity.User;
import com.planning.backend.repository.CoupleRepository;
import com.planning.backend.repository.UserRepository;

// Підготовка бази при старті (Hibernate уже додав нові таблиці й колонки):
//
//   1. Порожня база -> створюємо першу пару: Бодя (адмін) і Марічка.
//      Паролі беруться з secrets.properties і одразу хешуються BCrypt.
//   2. База попередньої версії (одна пара без таблиці couples, ролі в колонці users.code) ->
//      створюємо для них пару, переносимо роль / ім'я "чий?" / колір / адмінство, стару колонку code прибираємо.
//   3. Записи без пари (dates, wishlist_items, dreams) -> отримують couple_id свого автора.
//   4. Користувачі, які діяли до появи статистики в адмінці (last_activity_at порожній), ->
//      остання активність = час їхнього найсвіжішого запису (побачення, бажання, мрія, фото).
//
// Усі кроки безпечно запускати щоразу: якщо робити нічого — нічого не змінюється.
@Component
@Order(1)
public class CoupleSetup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CoupleSetup.class);

    private final UserRepository users;
    private final CoupleRepository couples;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final String bodyaPassword;
    private final String marichkaPassword;

    public CoupleSetup(UserRepository users, CoupleRepository couples, PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc,
            @Value("${users.bodya.password}") String bodyaPassword,
            @Value("${users.marichka.password}") String marichkaPassword) {
        this.users = users;
        this.couples = couples;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.bodyaPassword = bodyaPassword;
        this.marichkaPassword = marichkaPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            seedFirstCouple();
        } else {
            migrateSingleCouple();
        }
        backfillCoupleIds();
        backfillLastActivity();
    }

    private void seedFirstCouple() {
        if (bodyaPassword.isBlank() || marichkaPassword.isBlank()) {
            throw new IllegalStateException(
                    "Set users.bodya.password and users.marichka.password in backend/secrets.properties");
        }
        if (bodyaPassword.equals(marichkaPassword)) {
            // вхід лише за паролем: за однаковими паролями не зрозуміти, хто входить
            throw new IllegalStateException("Passwords of the two users must be different");
        }
        Couple couple = couples.save(new Couple());
        User bodya = new User(couple, "Бодя", "Боді", PartnerRole.HE, PartnerRole.HE.getDefaultColor(),
                passwordEncoder.encode(bodyaPassword));
        bodya.setAdmin(true);
        users.save(bodya);
        users.save(new User(couple, "Марічка", "Марічки", PartnerRole.SHE, PartnerRole.SHE.getDefaultColor(),
                passwordEncoder.encode(marichkaPassword)));
        log.info("Created the first couple: Bodya (admin) and Marichka");
    }

    // Користувачі без пари — це дані попередньої версії, де пара була одна на всю систему
    private void migrateSingleCouple() {
        Integer orphans = jdbc.queryForObject("select count(*) from users where couple_id is null", Integer.class);
        if (orphans == null || orphans == 0) {
            return;
        }
        Long coupleId = couples.save(new Couple()).getId();
        if (hasColumn("users", "code")) {
            jdbc.update("""
                    update users set
                        couple_id = ?,
                        role = case code when 'MARICHKA' then 'SHE' else 'HE' end,
                        name_genitive = case code when 'BODYA' then 'Боді' when 'MARICHKA' then 'Марічки' end,
                        color = case code when 'MARICHKA' then ? else ? end,
                        is_admin = (code = 'BODYA')
                    where couple_id is null""",
                    coupleId, PartnerRole.SHE.getDefaultColor(), PartnerRole.HE.getDefaultColor());
            // усе з колонки code вже перенесено в role / name_genitive / is_admin
            jdbc.execute("alter table users drop column code");
        } else {
            jdbc.update("update users set couple_id = ?, role = coalesce(role, 'HE') where couple_id is null", coupleId);
        }
        log.info("Moved {} existing users into couple #{}", orphans, coupleId);
    }

    private void backfillCoupleIds() {
        int dates = jdbc.update("""
                update dates d set couple_id = u.couple_id from users u
                where d.created_by_user_id = u.id and d.couple_id is null""");
        int wishes = jdbc.update("""
                update wishlist_items w set couple_id = u.couple_id from users u
                where w.owner_user_id = u.id and w.couple_id is null""");
        int dreams = jdbc.update("""
                update dreams r set couple_id = u.couple_id from users u
                where r.created_by_user_id = u.id and r.couple_id is null""");
        if (dates + wishes + dreams > 0) {
            log.info("Linked existing records to couples: {} dates, {} wishes, {} dreams", dates, wishes, dreams);
        }
    }

    private void backfillLastActivity() {
        int updated = jdbc.update("""
                update users u set last_activity_at = x.at
                from (select uid, max(at) as at from (
                          select created_by_user_id as uid, created_at as at from dates
                          union all select owner_user_id, created_at from wishlist_items
                          union all select created_by_user_id, created_at from dreams
                          union all select uploaded_by_user_id, uploaded_at from date_photos) a
                      group by uid) x
                where u.id = x.uid and u.last_activity_at is null""");
        if (updated > 0) {
            log.info("Estimated last activity for {} existing users from their records", updated);
        }
    }

    private boolean hasColumn(String table, String column) {
        Boolean exists = jdbc.queryForObject("""
                select exists(select 1 from information_schema.columns
                              where table_schema = 'public' and table_name = ? and column_name = ?)""",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }
}
