package com.planning.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.entity.PartnerRole;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;

// Після зміни бота всі мають прив'язатися заново: старі chat_id скидаються, кожен отримує посилання
class TelegramRelinkTest {

    private final UserRepository users = mock(UserRepository.class);
    private final TelegramClient telegram = mock(TelegramClient.class);

    private static User user(Long chatId, String bot, String token) {
        User user = new User(null, "A", null, PartnerRole.HE, null, "hash");
        user.linkTelegram(chatId, bot);
        user.setInviteToken(token);
        return user;
    }

    @Test
    void linksToOtherBotAreResetAndEveryoneGetsAnInvite() {
        User oldBot = user(1L, "DateWithMyGirl_bot", null);
        User beforeColumn = user(2L, null, null);
        User current = user(3L, "UstyBM_bot", null);
        User neverLinked = user(null, null, null);
        User waiting = user(null, null, "kept-token");
        when(users.findAll()).thenReturn(List.of(oldBot, beforeColumn, current, neverLinked, waiting));
        when(telegram.botUsername()).thenReturn("UstyBM_bot");

        new TelegramRelink(users, telegram).run(null);

        assertThat(oldBot.getTelegramChatId()).isNull();
        assertThat(oldBot.getInviteToken()).isNotNull();
        assertThat(beforeColumn.getTelegramChatId()).isNull();
        assertThat(beforeColumn.getInviteToken()).isNotNull();
        assertThat(current.getTelegramChatId()).isEqualTo(3L);
        assertThat(current.getInviteToken()).isNull();
        assertThat(neverLinked.getInviteToken()).isNotNull();
        assertThat(waiting.getInviteToken()).isEqualTo("kept-token");
    }

    @Test
    void nothingChangesWithoutBot() {
        User linked = user(1L, null, null);
        when(users.findAll()).thenReturn(List.of(linked));
        new TelegramRelink(users, telegram).run(null);
        assertThat(linked.getTelegramChatId()).isEqualTo(1L);
    }
}
