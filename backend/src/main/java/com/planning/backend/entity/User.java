package com.planning.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

// Таблиця users: кожен користувач належить рівно одній парі (couple_id).
// Пароль зберігається як BCrypt-хеш. Поки людина ще не отримала свій пароль
// (через Telegram-бота або від адміна), у pending_password лежить його ЗАШИФРОВАНА копія —
// вона стирається, щойно пароль доставлено ботом або людина вперше увійшла.
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nullable лише в схемі — щоб Hibernate міг додати колонку до старої таблиці; у коді завжди заповнене
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    @Column(nullable = false)
    private String name;

    // "чий?": "Боді", "Марічки" — для підписів "Вішліст Боді"
    @Column(name = "name_genitive")
    private String nameGenitive;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PartnerRole role;

    @Column(length = 20)
    private String color;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "avatar_url", length = 2000)
    private String avatarUrl;

    // доступ до адмінки (керування парами)
    @Column(name = "is_admin", nullable = false, columnDefinition = "boolean not null default false")
    private boolean admin;

    // Telegram: куди слати пароль і сповіщення (з'являється після /start у боті)
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    // до якого бота прив'язано chat_id (напр. UstyBM_bot). Інший бот у налаштуваннях -> прив'язка застаріла,
    // людині треба натиснути Start у новому боті (див. config/TelegramRelink)
    @Column(name = "telegram_bot", length = 64)
    private String telegramBot;

    // одноразовий код із посилання-запрошення t.me/<бот>?start=<код>
    @Column(name = "invite_token", unique = true, length = 64)
    private String inviteToken;

    // зашифрований пароль, ще не доставлений власнику (див. security/PasswordVault)
    @Column(name = "pending_password", length = 500)
    private String pendingPassword;

    // статистика адмінки: останній вхід і остання дія (вхід або будь-яка зміна даних — security/ActivityTracker)
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "created_at")
    private Instant createdAt;

    protected User() {
        // потрібен JPA
    }

    public User(Couple couple, String name, String nameGenitive, PartnerRole role, String color, String passwordHash) {
        this.couple = couple;
        this.name = name;
        this.nameGenitive = nameGenitive;
        this.role = role;
        this.color = color;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Couple getCouple() {
        return couple;
    }

    public Long getCoupleId() {
        return couple.getId();
    }

    public String getName() {
        return name;
    }

    public String getNameGenitive() {
        return nameGenitive == null || nameGenitive.isBlank() ? name : nameGenitive;
    }

    public PartnerRole getRole() {
        return role;
    }

    public String getColor() {
        return color == null ? role.getDefaultColor() : color;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getTelegramBot() {
        return telegramBot;
    }

    // Прив'язати Telegram-чат (null, null — відв'язати)
    public void linkTelegram(Long chatId, String bot) {
        this.telegramChatId = chatId;
        this.telegramBot = chatId == null ? null : bot;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public String getPendingPassword() {
        return pendingPassword;
    }

    public void setPendingPassword(String pendingPassword) {
        this.pendingPassword = pendingPassword;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    // успішний вхід — це водночас і активність
    public void recordLogin(Instant at) {
        this.lastLoginAt = at;
        this.lastActivityAt = at;
    }
}
