package com.planning.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.planning.backend.dto.EntryRequest;
import com.planning.backend.dto.ToggleRequest;
import com.planning.backend.dto.wishlist.WishlistItemResponse;
import com.planning.backend.service.WishlistService;

import jakarta.validation.Valid;

// Вішліст кожного з пари
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // GET /api/wishlist?owner=2 -> бажання користувача 2 (лише якщо він із твоєї пари)
    @GetMapping
    public List<WishlistItemResponse> byOwner(@RequestParam Long owner) {
        return wishlistService.byOwner(owner);
    }

    // POST /api/wishlist -> нове бажання (власник = той, хто увійшов)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistItemResponse create(@Valid @RequestBody EntryRequest request) {
        return wishlistService.create(request);
    }

    @PutMapping("/{id}")
    public WishlistItemResponse update(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        return wishlistService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        wishlistService.delete(id);
    }

    // PUT /api/wishlist/5/fulfilled {"value": true} -> бажання виконано
    @PutMapping("/{id}/fulfilled")
    public WishlistItemResponse setFulfilled(@PathVariable Long id, @Valid @RequestBody ToggleRequest request) {
        return wishlistService.setFulfilled(id, request.value());
    }
}
