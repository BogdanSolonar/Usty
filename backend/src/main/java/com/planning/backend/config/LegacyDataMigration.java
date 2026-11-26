package com.planning.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Одноразове перенесення даних зі старої спільної таблиці items (версія 2)
// у нові окремі таблиці dates / wishlist_items / dreams.
//
// Старі записи не мали автора, тому автором ставимо адміна (Бодю), а парою — його пару.
// Після перенесення стара таблиця перейменовується в items_legacy (як резервна копія),
// тож при наступних запусках цей код нічого не робить.
@Component
@Order(2) // після CoupleSetup: потрібні Бодя і його пара
public class LegacyDataMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataMigration.class);

    private final JdbcTemplate jdbc;

    public LegacyDataMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Boolean exists = jdbc.queryForObject("select to_regclass('public.items') is not null", Boolean.class);
        if (!Boolean.TRUE.equals(exists)) {
            return;
        }
        Long author = jdbc.queryForObject("select id from users where is_admin order by id limit 1", Long.class);
        Long couple = jdbc.queryForObject("select couple_id from users where id = ?", Long.class, author);

        int dates = jdbc.update("""
                insert into dates (title, description, cover_image_url, created_by_user_id, couple_id, created_at, status)
                select title, description, image_url, ?, ?, created_at, 'IDEA' from items where category = 'DATE_IDEA'
                order by id""", author, couple);
        int wishes = jdbc.update("""
                insert into wishlist_items (title, description, image_url, owner_user_id, couple_id, created_at, is_fulfilled)
                select title, description, image_url, ?, ?, created_at, false from items where category = 'WISHLIST'
                order by id""", author, couple);
        int dreams = jdbc.update("""
                insert into dreams (title, description, image_url, created_by_user_id, couple_id, created_at, is_achieved)
                select title, description, image_url, ?, ?, created_at, false from items where category = 'DREAM'
                order by id""", author, couple);

        jdbc.execute("alter table items rename to items_legacy");
        log.info("Migrated old items: {} dates, {} wishes, {} dreams (old table kept as items_legacy)",
                dates, wishes, dreams);
    }
}
