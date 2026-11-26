package com.planning.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.security.RandomSecrets;

// Перехід на іншого Telegram-бота (зараз — @UstyBM_bot).
//
// Новий бот не може першим написати людині, поки вона сама не натисне в ньому «Start».
// Тому при старті кожна прив'язка до ІНШОГО бота (або старіша за колонку telegram_bot) скидається:
// chat_id стирається, а людина отримує новий код запрошення. В адмінці в неї з'являється
// «⏳ Telegram не прив'язано» і посилання t.me/<новий бот>?start=<код>; паролі не змінюються.
// Так само посилання отримує кожен, хто ще жодного разу не прив'язував бота.
//
// Також перевіряємо, що токен справді належить боту з telegram.bot-username.
@Component
@Order(3)
public class TelegramRelink implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramRelink.class);

    private final UserRepository users;
    private final TelegramClient telegram;

    public TelegramRelink(UserRepository users, TelegramClient telegram) {
        this.users = users;
        this.telegram = telegram;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String bot = telegram.botUsername();
        if (bot == null) {
            return; // бот не налаштований — нічого не чіпаємо
        }
        String actual = telegram.actualUsername();
        if (actual != null && !actual.equalsIgnoreCase(bot)) {
            log.error("telegram.bot-token belongs to @{}, but telegram.bot-username is @{}", actual, bot);
        }
        int reset = 0;
        for (User user : users.findAll()) {
            if (user.getTelegramChatId() != null && !bot.equalsIgnoreCase(user.getTelegramBot())) {
                user.linkTelegram(null, null);
                reset++;
            }
            // кожен без прив'язки отримує посилання на поточного бота
            if (user.getTelegramChatId() == null && user.getInviteToken() == null) {
                user.setInviteToken(RandomSecrets.inviteToken());
            }
        }
        if (reset > 0) {
            log.info("Telegram bot is now @{}: {} user(s) must press Start in it again (new invite links in admin)",
                    bot, reset);
        }
    }
}
