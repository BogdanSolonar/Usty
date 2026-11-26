package com.planning.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.planning.backend.dto.admin.AdminOverview;
import com.planning.backend.dto.admin.CoupleView;
import com.planning.backend.dto.admin.CreateCoupleRequest;
import com.planning.backend.service.AdminService;

import jakarta.validation.Valid;

// Адмінка (лише для адміністратора — перевірка в AdminService; решта отримує 403)
@RestController
@RequestMapping("/api/admin/couples")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/admin/couples/overview -> короткі лічильники по системі (пари, входи, Telegram, записи)
    @GetMapping("/overview")
    public AdminOverview overview() {
        return adminService.overview();
    }

    // GET /api/admin/couples -> усі пари з користувачами, статусом Telegram, посиланнями
    @GetMapping
    public List<CoupleView> list() {
        return adminService.list();
    }

    // POST /api/admin/couples {"he": {...}, "she": {...}} -> нова пара з паролями й посиланнями на бота
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoupleView create(@Valid @RequestBody CreateCoupleRequest request) {
        return adminService.create(request);
    }

    // POST /api/admin/couples/3/users/7/reset-password -> новий пароль і нове посилання для користувача 7
    @PostMapping("/{coupleId}/users/{userId}/reset-password")
    public CoupleView resetPassword(@PathVariable Long coupleId, @PathVariable Long userId) {
        return adminService.resetPassword(coupleId, userId);
    }

    // POST /api/admin/couples/3/users/7/invite -> нове посилання на бота (пароль не змінюється)
    @PostMapping("/{coupleId}/users/{userId}/invite")
    public CoupleView newInvite(@PathVariable Long coupleId, @PathVariable Long userId) {
        return adminService.newInvite(coupleId, userId);
    }

    // DELETE /api/admin/couples/3 -> пара разом з обома користувачами і всім їхнім контентом
    @DeleteMapping("/{coupleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long coupleId) {
        adminService.delete(coupleId);
    }
}
