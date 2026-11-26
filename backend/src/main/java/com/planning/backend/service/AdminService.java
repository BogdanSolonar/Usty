package com.planning.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.dto.admin.AdminOverview;
import com.planning.backend.dto.admin.CoupleView;
import com.planning.backend.dto.admin.CreateCoupleRequest;
import com.planning.backend.dto.admin.MemberCounts;
import com.planning.backend.dto.admin.MemberView;
import com.planning.backend.dto.admin.PartnerRequest;
import com.planning.backend.entity.Couple;
import com.planning.backend.entity.DateIdea;
import com.planning.backend.entity.DatePhoto;
import com.planning.backend.entity.Dream;
import com.planning.backend.entity.PartnerRole;
import com.planning.backend.entity.User;
import com.planning.backend.entity.WishlistItem;
import com.planning.backend.repository.CoupleRepository;
import com.planning.backend.repository.DatePhotoRepository;
import com.planning.backend.repository.DateRepository;
import com.planning.backend.repository.DreamRepository;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.repository.WishlistItemRepository;
import com.planning.backend.security.CurrentUser;
import com.planning.backend.security.PasswordVault;
import com.planning.backend.security.RandomSecrets;
import com.planning.backend.service.telegram.InviteService;

// Адмінка: пари та їхні користувачі. КОЖЕН метод спершу перевіряє, що запит робить адмін
// (currentUser.requireAdmin() -> 403 для всіх інших, навіть якщо вони знають адресу API).
//
// Нова пара: двоє користувачів, у кожного свій пароль (BCrypt-хеш для входу +
// тимчасова зашифрована копія для доставки) і свій одноразовий код запрошення в Telegram-бот.
@Service
public class AdminService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final CurrentUser currentUser;
    private final CoupleRepository couples;
    private final UserRepository users;
    private final DateRepository dates;
    private final WishlistItemRepository wishes;
    private final DreamRepository dreams;
    private final DatePhotoRepository photos;
    private final PasswordEncoder passwordEncoder;
    private final PasswordVault vault;
    private final InviteService invites;
    private final TelegramClient telegram;
    private final ImageStorageService imageStorage;

    public AdminService(CurrentUser currentUser, CoupleRepository couples, UserRepository users,
            DateRepository dates, WishlistItemRepository wishes, DreamRepository dreams, DatePhotoRepository photos,
            PasswordEncoder passwordEncoder, PasswordVault vault, InviteService invites, TelegramClient telegram,
            ImageStorageService imageStorage) {
        this.currentUser = currentUser;
        this.couples = couples;
        this.users = users;
        this.dates = dates;
        this.wishes = wishes;
        this.dreams = dreams;
        this.photos = photos;
        this.passwordEncoder = passwordEncoder;
        this.vault = vault;
        this.invites = invites;
        this.telegram = telegram;
        this.imageStorage = imageStorage;
    }

    // Усі пари з користувачами
    @Transactional(readOnly = true)
    public List<CoupleView> list() {
        currentUser.requireAdmin();
        return couples.findAllByOrderByIdAsc().stream().map(this::toView).toList();
    }

    // Короткі лічильники по всій системі (блок над списком пар)
    @Transactional(readOnly = true)
    public AdminOverview overview() {
        currentUser.requireAdmin();
        List<User> all = users.findAllByOrderByIdAsc();
        long activeCouples = all.stream()
                .collect(Collectors.groupingBy(User::getCoupleId))
                .values().stream()
                .filter(members -> members.stream().allMatch(AdminService::everLoggedIn))
                .count();
        return new AdminOverview(couples.count(), activeCouples, all.size(),
                all.stream().filter(AdminService::everLoggedIn).count(),
                all.stream().filter(u -> u.getTelegramChatId() != null).count(),
                dates.count(), wishes.count(), dreams.count(), photos.count());
    }

    @Transactional
    public CoupleView create(CreateCoupleRequest request) {
        currentUser.requireAdmin();
        String hePassword = passwordOrGenerated(request.he());
        String shePassword = passwordOrGenerated(request.she());
        if (hePassword.equals(shePassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Паролі хлопця і дівчини мають відрізнятися");
        }
        ensureUnused(hePassword);
        ensureUnused(shePassword);

        Couple couple = couples.save(new Couple());
        newMember(couple, request.he(), PartnerRole.HE, hePassword);
        newMember(couple, request.she(), PartnerRole.SHE, shePassword);
        return toView(couple);
    }

    // Новий пароль і нове посилання для одного з пари (якщо пароль загубили або посилання потрапило не туди).
    // Старий пароль і старе посилання одразу перестають діяти.
    @Transactional
    public CoupleView resetPassword(Long coupleId, Long userId) {
        currentUser.requireAdmin();
        User user = users.findByIdAndCouple_Id(userId, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String password = RandomSecrets.password();
        ensureUnused(password);
        setPassword(user, password);
        return toView(user.getCouple());
    }

    // Лише нове посилання на бота, пароль не змінюється (напр. щоб уже активний користувач
    // прив'язав Telegram для сповіщень). Бот тоді лише прив'яже чат — пароля він не знає.
    @Transactional
    public CoupleView newInvite(Long coupleId, Long userId) {
        currentUser.requireAdmin();
        User user = users.findByIdAndCouple_Id(userId, coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setInviteToken(RandomSecrets.inviteToken());
        return toView(user.getCouple());
    }

    // Видалення ЛИШЕ парою: обоє користувачів + усі побачення (з фото), бажання, мрії, аватарки.
    // Пару, в якій є адмін, видалити не можна — інакше не лишиться нікого з доступом до адмінки.
    @Transactional
    public void delete(Long coupleId) {
        currentUser.requireAdmin();
        Couple couple = couples.findById(coupleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Couple not found"));
        List<User> members = users.findByCouple_IdOrderByIdAsc(coupleId);
        if (members.stream().anyMatch(User::isAdmin)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пару адміністратора видалити не можна");
        }

        List<String> files = new ArrayList<>();
        List<DateIdea> coupleDates = dates.findByCouple_Id(coupleId);
        for (DateIdea date : coupleDates) {
            files.add(date.getCoverImageUrl());
            date.getPhotos().stream().map(DatePhoto::getPhotoUrl).forEach(files::add);
        }
        List<WishlistItem> coupleWishes = wishes.findByCouple_Id(coupleId);
        List<Dream> coupleDreams = dreams.findByCouple_Id(coupleId);
        Stream.concat(coupleWishes.stream().map(WishlistItem::getImageUrl), coupleDreams.stream().map(Dream::getImageUrl))
                .forEach(files::add);
        members.stream().map(User::getAvatarUrl).forEach(files::add);

        // порядок важливий: спершу те, що посилається на користувачів, потім самі користувачі й пара
        dates.deleteAll(coupleDates); // фото з побачень видаляються каскадом
        wishes.deleteAll(coupleWishes);
        dreams.deleteAll(coupleDreams);
        users.deleteAll(members);
        couples.delete(couple);

        // файли з диска — лише коли видалення з бази точно збережене
        List<String> toDelete = files.stream().filter(Objects::nonNull).toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                toDelete.forEach(imageStorage::deleteIfLocal);
            }
        });
    }

    private void newMember(Couple couple, PartnerRequest form, PartnerRole role, String password) {
        String color = form.color() == null || form.color().isBlank() ? role.getDefaultColor() : form.color();
        String genitive = form.nameGenitive() == null || form.nameGenitive().isBlank() ? null
                : form.nameGenitive().trim();
        User user = new User(couple, form.name().trim(), genitive, role, color, "");
        setPassword(user, password);
        users.save(user);
    }

    // Хеш — для входу; зашифрована копія і новий код запрошення — для доставки ботом або показу адміну
    private void setPassword(User user, String password) {
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPendingPassword(vault.encrypt(password));
        user.setInviteToken(RandomSecrets.inviteToken());
    }

    private static String passwordOrGenerated(PartnerRequest form) {
        String password = form.password() == null ? "" : form.password().trim();
        if (password.isEmpty()) {
            return RandomSecrets.password();
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пароль має бути щонайменше з " + MIN_PASSWORD_LENGTH + " символів");
        }
        return password;
    }

    // Вхід лише за паролем: якщо такий пароль уже в когось є, сервер не зрозуміє, хто входить
    private void ensureUnused(String password) {
        for (User existing : users.findAllByOrderByIdAsc()) {
            if (passwordEncoder.matches(password, existing.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Такий пароль уже використовується — оберіть інший");
            }
        }
    }

    private CoupleView toView(Couple couple) {
        List<User> members = users.findByCouple_IdOrderByIdAsc(couple.getId());
        List<MemberView> views = members.stream().map(this::toView).toList();
        return new CoupleView(couple.getId(), couple.getCreatedAt(), views,
                members.stream().noneMatch(User::isAdmin), telegram.botUsername() != null);
    }

    private MemberView toView(User user) {
        return new MemberView(user.getId(), user.getName(), user.getNameGenitive(), user.getRole(), user.getColor(),
                user.getAvatarUrl(), user.isAdmin(), user.getTelegramChatId() != null, invites.inviteLink(user),
                vault.decrypt(user.getPendingPassword()), user.getCreatedAt(), everLoggedIn(user),
                user.getLastLoginAt(), user.getLastActivityAt(),
                new MemberCounts(dates.countByCreatedBy_Id(user.getId()), wishes.countByOwner_Id(user.getId()),
                        dreams.countByCreatedBy_Id(user.getId()), photos.countByUploadedBy_Id(user.getId())));
    }

    // Входив хоча б раз. Хто діяв ще до появи цієї статистики, має лише last_activity_at (див. CoupleSetup).
    private static boolean everLoggedIn(User user) {
        return user.getLastLoginAt() != null || user.getLastActivityAt() != null;
    }
}
