package com.planning.backend.service.telegram;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.client.TelegramClient.Update;

// Як бекенд "чує" бота: long polling.
//
// Окремий фоновий потік постійно питає Telegram "чи є нові повідомлення?" (getUpdates, чекає до 25 с).
// Нове повідомлення -> InviteService вирішує, що відповісти -> sendMessage у той самий чат.
// Публічна адреса чи HTTPS (як для webhook) не потрібні — працює й на локальному комп'ютері.
// Якщо бекенд був вимкнений, Telegram зберігає повідомлення до 24 годин, і вони обробляться після запуску.
//
// Вимкнути: telegram.polling-enabled=false (або порожній telegram.bot-token).
// Опитувати одного бота може лише ОДНА програма одночасно, інакше Telegram відповідає 409 Conflict.
@Component
public class TelegramPoller implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramPoller.class);
    private static final int LONG_POLL_SECONDS = 25;

    private final TelegramClient telegram;
    private final InviteService invites;
    private final boolean enabled;
    private volatile boolean running;
    private Thread thread;

    public TelegramPoller(TelegramClient telegram, InviteService invites,
            @Value("${telegram.polling-enabled:true}") boolean pollingEnabled) {
        this.telegram = telegram;
        this.invites = invites;
        this.enabled = pollingEnabled && telegram.isEnabled();
    }

    @Override
    public void start() {
        if (!enabled) {
            log.info("Telegram bot polling is off (no bot token or telegram.polling-enabled=false)");
            return;
        }
        running = true;
        thread = new Thread(this::loop, "telegram-poller");
        thread.setDaemon(true);
        thread.start();
        log.info("Telegram bot polling started");
    }

    @Override
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        long offset = 0;
        while (running) {
            try {
                List<Update> updates = telegram.getUpdates(offset, LONG_POLL_SECONDS);
                for (Update update : updates) {
                    offset = update.updateId() + 1; // це повідомлення оброблено — Telegram більше його не дасть
                    process(update);
                }
            } catch (HttpClientErrorException.Conflict e) {
                log.warn("Another program is polling this Telegram bot (409 Conflict); retrying in 30 s");
                pause(30_000);
            } catch (RuntimeException e) {
                if (running) {
                    log.warn("Telegram polling failed: {}; retrying in 5 s", e.getMessage());
                    pause(5_000);
                }
            }
        }
    }

    private void process(Update update) {
        if (update.chatId() == null) {
            return; // не повідомлення в чаті (службове оновлення)
        }
        try {
            InviteService.Reply reply = invites.handle(update.chatId(), update.text());
            telegram.sendMessage(update.chatId(), reply.text());
            if (reply.deliveredUserId() != null) {
                invites.markDelivered(reply.deliveredUserId());
            }
        } catch (RuntimeException e) {
            // пароль не надіслано -> код запрошення лишається дійсним, можна натиснути /start ще раз
            log.warn("Could not answer Telegram chat {}: {}", update.chatId(), e.getMessage());
        }
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
