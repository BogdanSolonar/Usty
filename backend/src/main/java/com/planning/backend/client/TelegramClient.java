package com.planning.backend.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

// Telegram Bot API (https://api.telegram.org/bot<токен>/<метод>):
//   sendMessage — надіслати текст у чат (пароль, сповіщення про побачення)
//   getUpdates  — забрати нові повідомлення боту (long polling, див. service/telegram/TelegramPoller)
//   getMe       — перевірити, що токен належить саме боту з telegram.bot-username
// Токен і ім'я бота (UstyBM_bot) лежать у backend/secrets.properties.
@Component
public class TelegramClient {

    // Нове повідомлення боту: від кого (chat_id) і текст
    public record Update(long updateId, Long chatId, String text) {
    }

    private final String botToken;
    private final Long defaultChatId;
    private final RestClient http;
    private final String configuredUsername;
    private volatile String fetchedUsername;

    public TelegramClient(@Value("${telegram.bot-token}") String botToken,
            @Value("${telegram.chat-id}") String defaultChatId,
            @Value("${telegram.bot-username:}") String botUsername) {
        this.botToken = botToken.trim();
        this.configuredUsername = botUsername.trim().replaceFirst("^@", "");
        this.defaultChatId = defaultChatId.isBlank() ? null : Long.parseLong(defaultChatId.trim());
        // long polling тримає запит до 25 с — таймаут читання має бути більшим
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(40));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return !botToken.isEmpty();
    }

    // Старий спільний чат із secrets.properties (telegram.chat-id) — запасний адресат для пари адміна
    public Optional<Long> defaultChatId() {
        return Optional.ofNullable(defaultChatId);
    }

    public void sendMessage(long chatId, String text) {
        http.post()
                .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
    }

    // Чекає до timeoutSeconds на нові повідомлення; offset = id останнього обробленого + 1
    // (так Telegram розуміє, що попередні вже оброблені, і більше їх не віддає)
    public List<Update> getUpdates(long offset, int timeoutSeconds) {
        JsonNode body = http.get()
                .uri("https://api.telegram.org/bot{token}/getUpdates?offset={offset}&timeout={timeout}"
                        + "&allowed_updates=[\"message\"]", botToken, offset, timeoutSeconds)
                .retrieve()
                .body(JsonNode.class);
        List<Update> updates = new ArrayList<>();
        if (body == null) {
            return updates;
        }
        for (JsonNode u : body.path("result")) {
            JsonNode message = u.path("message");
            Long chatId = message.path("chat").has("id") ? message.path("chat").path("id").asLong() : null;
            String text = message.path("text").isString() ? message.path("text").asString() : null;
            updates.add(new Update(u.path("update_id").asLong(), chatId, text));
        }
        return updates;
    }

    // Ім'я бота (без @) для посилань t.me/<ім'я>?start=<код>: з telegram.bot-username,
    // а якщо там порожньо — через getMe. null — бот не налаштований або Telegram недоступний.
    public String botUsername() {
        if (!isEnabled()) {
            return null;
        }
        return configuredUsername.isEmpty() ? actualUsername() : configuredUsername;
    }

    // Справжнє ім'я бота, якому належить токен (getMe; запитується один раз)
    public String actualUsername() {
        if (fetchedUsername == null && isEnabled()) {
            try {
                JsonNode me = http.get().uri("https://api.telegram.org/bot{token}/getMe", botToken)
                        .retrieve().body(JsonNode.class);
                String name = me == null ? null : me.path("result").path("username").asString(null);
                fetchedUsername = name == null || name.isBlank() ? null : name;
            } catch (RuntimeException e) {
                return null;
            }
        }
        return fetchedUsername;
    }
}
