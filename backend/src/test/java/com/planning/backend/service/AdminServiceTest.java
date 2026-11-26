package com.planning.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.client.TelegramClient;
import com.planning.backend.dto.admin.CreateCoupleRequest;
import com.planning.backend.dto.admin.PartnerRequest;
import com.planning.backend.entity.Couple;
import com.planning.backend.entity.PartnerRole;
import com.planning.backend.entity.User;
import com.planning.backend.repository.CoupleRepository;
import com.planning.backend.repository.DatePhotoRepository;
import com.planning.backend.repository.DateRepository;
import com.planning.backend.repository.DreamRepository;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.repository.WishlistItemRepository;
import com.planning.backend.security.CurrentUser;
import com.planning.backend.security.PasswordVault;
import com.planning.backend.service.telegram.InviteService;

// Захист адмінки на бекенді: не-адмін отримує 403 ще до будь-якої роботи з базою
class AdminServiceTest {

    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final CoupleRepository couples = mock(CoupleRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AdminService admin = new AdminService(currentUser, couples, users, mock(DateRepository.class),
            mock(WishlistItemRepository.class), mock(DreamRepository.class), mock(DatePhotoRepository.class),
            mock(PasswordEncoder.class),
            mock(PasswordVault.class), mock(InviteService.class), mock(TelegramClient.class),
            mock(ImageStorageService.class));

    private static HttpStatusCode status(Throwable e) {
        return ((ResponseStatusException) e).getStatusCode();
    }

    @Test
    void nonAdminIsForbiddenEverywhere() {
        when(currentUser.requireAdmin())
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only"));
        PartnerRequest p = new PartnerRequest("A", null, null, null);

        assertThatThrownBy(admin::list).satisfies(e -> status(e).isSameCodeAs(HttpStatus.FORBIDDEN));
        assertThatThrownBy(admin::overview).satisfies(e -> status(e).isSameCodeAs(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> admin.newInvite(1L, 1L)).satisfies(e -> status(e).isSameCodeAs(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> admin.create(new CreateCoupleRequest(p, p)))
                .satisfies(e -> status(e).isSameCodeAs(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> admin.resetPassword(1L, 1L))
                .satisfies(e -> status(e).isSameCodeAs(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> admin.delete(1L)).satisfies(e -> status(e).isSameCodeAs(HttpStatus.FORBIDDEN));
        verifyNoInteractions(couples, users);
    }

    @Test
    void adminsOwnCoupleCannotBeDeleted() {
        User adminUser = new User(null, "Бодя", "Боді", PartnerRole.HE, null, "hash");
        adminUser.setAdmin(true);
        when(currentUser.requireAdmin()).thenReturn(adminUser);
        when(couples.findById(1L)).thenReturn(Optional.of(new Couple()));
        when(users.findByCouple_IdOrderByIdAsc(1L)).thenReturn(List.of(adminUser));

        assertThatThrownBy(() -> admin.delete(1L)).satisfies(e -> status(e).isSameCodeAs(HttpStatus.CONFLICT));
    }

    @Test
    void partnersNeedDifferentPasswords() {
        when(currentUser.requireAdmin()).thenReturn(null);
        PartnerRequest he = new PartnerRequest("Андрій", null, null, "same-pass");
        PartnerRequest she = new PartnerRequest("Олена", null, null, "same-pass");
        assertThatThrownBy(() -> admin.create(new CreateCoupleRequest(he, she)))
                .satisfies(e -> status(e).isSameCodeAs(HttpStatus.BAD_REQUEST));
    }
}
