package com.planning.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;

// Сповіщення в Telegram про заплановане побачення — обом з пари, хто прив'язав бота.
// Надсилання працює у фоні (@Async): користувач не чекає на Telegram, а якщо Telegram недоступний —
// побачення все одно збережене, помилка лише пишеться в лог.
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy (EEEE)", Locale.forLanguageTag("uk"));

    // Усе, що потрібно для тексту повідомлення (без сутностей JPA — їх не можна читати в іншому потоці)
    public record PlannedDate(String title, LocalDate date, String note, String initiator) {
    }

    private final TelegramClient telegram;
    private final UserRepository users;

    public NotificationService(TelegramClient telegram, UserRepository users) {
        this.telegram = telegram;
        this.users = users;
    }

    // Кому слати: чати обох з пари, які натиснули /start у боті.
    // Поки ніхто з пари адміна не прив'язав бота — старий спільний чат із secrets.properties.
    public List<Long> recipients(Long coupleId) {
        List<User> couple = users.findByCouple_IdOrderByIdAsc(coupleId);
        List<Long> chats = couple.stream().map(User::getTelegramChatId).filter(Objects::nonNull).distinct().toList();
        if (chats.isEmpty() && couple.stream().anyMatch(User::isAdmin)) {
            return telegram.defaultChatId().map(List::of).orElse(List.of());
        }
        return chats;
    }

    @Async
    public void datePlanned(PlannedDate planned, List<Long> chatIds) {
        if (!telegram.isEnabled() || chatIds.isEmpty()) {
            log.info("No Telegram recipients for this couple: skipping notification");
            return;
        }
        String text = messageText(planned);
        for (Long chatId : chatIds) {
            try {
                telegram.sendMessage(chatId, text);
            } catch (RuntimeException e) {
                log.warn("Telegram notification to chat {} failed: {}", chatId, e.getMessage());
            }
        }
    }

    static String messageText(PlannedDate planned) {
        String note = planned.note() == null || planned.note().isBlank() ? "—" : planned.note();
        return "❤ Заплановано побачення!\n\n"
                + "💫 Побачення: " + planned.title() + "\n"
                + "📅 Дата: " + DATE_FORMAT.format(planned.date()) + "\n"
                + "💭 Примітка: " + note + "\n"
                + "🙋 Ініціатор: " + planned.initiator();
    }
}
