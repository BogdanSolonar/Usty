package com.planning.backend.service.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.entity.PartnerRole;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.security.PasswordVault;

// Бот видає кожному лише ЙОГО пароль: за кодом із /start знаходиться рівно один користувач
class InviteServiceTest {

    private final PasswordVault vault = new PasswordVault(new SecretKeySpec(new byte[32], "HmacSHA256"));
    private final UserRepository users = mock(UserRepository.class);
    private final TelegramClient telegram = mock(TelegramClient.class);
    private InviteService invites;

    private User he;
    private User she;

    @BeforeEach
    void setUp() {
        invites = new InviteService(users, vault, telegram);
        when(telegram.botUsername()).thenReturn("UstyBM_bot");
        he = user("Андрій", PartnerRole.HE, "he-token", "andrii-pass-1");
        she = user("Олена", PartnerRole.SHE, "she-token", "olena-pass-2");
        Map<String, User> byToken = Map.of("he-token", he, "she-token", she);
        when(users.findByInviteToken(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(byToken.get(inv.getArgument(0, String.class))));
        when(users.findById(1L)).thenReturn(Optional.of(he));
        when(users.findById(2L)).thenReturn(Optional.of(she));
    }

    private User user(String name, PartnerRole role, String token, String password) {
        User user = new User(null, name, null, role, null, "hash");
        user.setInviteToken(token);
        user.setPendingPassword(vault.encrypt(password));
        return user;
    }

    @Test
    void eachLinkGivesOnlyItsOwnersPassword() {
        InviteService.Reply heReply = invites.handle(111L, "/start he-token");
        InviteService.Reply sheReply = invites.handle(222L, "/start she-token");

        assertThat(heReply.text()).contains("andrii-pass-1").doesNotContain("olena-pass-2");
        assertThat(sheReply.text()).contains("olena-pass-2").doesNotContain("andrii-pass-1");
        assertThat(he.getTelegramChatId()).isEqualTo(111L);
        assertThat(she.getTelegramChatId()).isEqualTo(222L);
        assertThat(he.getTelegramBot()).isEqualTo("UstyBM_bot");
    }

    @Test
    void unknownOrMissingTokenRevealsNothing() {
        assertThat(invites.handle(5L, "/start wrong").text()).contains("недійсне").doesNotContain("pass");
        assertThat(invites.handle(5L, "/start").text()).contains("недійсне");
        assertThat(invites.handle(5L, "привіт").text()).contains("посилання-запрошення");
        assertThat(invites.handle(5L, null).deliveredUserId()).isNull();
    }

    @Test
    void afterDeliveryTokenAndPendingPasswordAreErased() {
        when(users.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(he));
        InviteService.Reply reply = invites.handle(111L, "/start he-token");
        invites.markDelivered(reply.deliveredUserId());

        assertThat(he.getInviteToken()).isNull();
        assertThat(he.getPendingPassword()).isNull();
        // повторне натискання того самого посилання вже нічого не видасть
        when(users.findByInviteToken("he-token")).thenReturn(Optional.empty());
        assertThat(invites.handle(111L, "/start he-token").text()).contains("недійсне");
    }

    @Test
    void alreadyDeliveredPasswordOnlyLinksTelegram() {
        she.setPendingPassword(null); // людина вже входила
        InviteService.Reply reply = invites.handle(222L, "/start she-token");
        assertThat(reply.deliveredUserId()).isNull();
        assertThat(reply.text()).contains("прив'язано").contains("уже було видано");
        assertThat(she.getTelegramChatId()).isEqualTo(222L);
        assertThat(she.getInviteToken()).isNull();
    }
}
