package com.planning.backend.service.telegram;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.security.PasswordVault;

// Запрошення через Telegram-бота.
//
// Кожен новий користувач отримує свій одноразовий код (users.invite_token) і посилання
// https://t.me/<бот>?start=<код>. Людина відкриває посилання -> Telegram показує кнопку "Start" ->
// бот отримує повідомлення "/start <код>" разом із chat_id цієї людини.
// За кодом знаходимо РІВНО ОДНОГО користувача — тому чужий пароль видати неможливо:
// хлопцеве посилання знає лише пароль хлопця, дівчине — лише дівчини.
@Service
public class InviteService {

    // Що відповісти в чат; deliveredUserId != null -> після успішної відправки позначити пароль доставленим
    public record Reply(String text, Long deliveredUserId) {
    }

    private final UserRepository users;
    private final PasswordVault vault;
    private final TelegramClient telegram;

    public InviteService(UserRepository users, PasswordVault vault, TelegramClient telegram) {
        this.users = users;
        this.vault = vault;
        this.telegram = telegram;
    }

    // Посилання-запрошення; null — якщо коду вже немає або бот не налаштований / Telegram недоступний
    public String inviteLink(User user) {
        if (user.getInviteToken() == null) {
            return null;
        }
        String bot = telegram.botUsername();
        return bot == null ? null : "https://t.me/" + bot + "?start=" + user.getInviteToken();
    }

    // Обробка вхідного повідомлення боту
    @Transactional
    public Reply handle(long chatId, String text) {
        if (text == null || !text.startsWith("/start")) {
            return new Reply("Привіт! Я бот Usty 💞\n\nЩоб отримати пароль, відкрийте посилання-запрошення, "
                    + "яке надіслав адміністратор, і натисніть «Start».", null);
        }
        String token = text.substring("/start".length()).trim();
        Optional<User> found = token.isEmpty() ? Optional.empty() : users.findByInviteToken(token);
        if (found.isEmpty()) {
            return new Reply("Це посилання недійсне або вже використане 😕\n"
                    + "Попросіть адміністратора створити для вас нове.", null);
        }
        User user = found.get();
        user.linkTelegram(chatId, telegram.botUsername()); // сюди ж приходитимуть сповіщення про побачення пари
        String password = vault.decrypt(user.getPendingPassword());
        if (password == null) {
            // пароль уже доставлено раніше (або людина вже входила) — лише прив'язуємо Telegram
            user.setInviteToken(null);
            return new Reply("Привіт, " + user.getName() + "! Telegram прив'язано ✅\n"
                    + "Сюди приходитимуть сповіщення про ваші побачення.\n\n"
                    + "Пароль уже було видано раніше. Якщо ви його забули — попросіть адміністратора створити новий.",
                    null);
        }
        return new Reply("Привіт, " + user.getName() + "! 💞\n\n"
                + "Твій пароль для входу в Usty:\n" + password + "\n\n"
                + "Введи його на сторінці входу — і ви з половинкою вже разом у додатку.\n"
                + "Сюди ж приходитимуть сповіщення про ваші побачення.", user.getId());
    }

    // Telegram підтвердив відправку — тепер можна стерти і код, і зашифровану копію пароля
    @Transactional
    public void markDelivered(Long userId) {
        users.findById(userId).ifPresent(user -> {
            user.setInviteToken(null);
            user.setPendingPassword(null);
        });
    }
}
