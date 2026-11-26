package com.planning.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.dto.EntryRequest;
import com.planning.backend.dto.wishlist.WishlistItemResponse;
import com.planning.backend.entity.User;
import com.planning.backend.entity.WishlistItem;
import com.planning.backend.repository.WishlistItemRepository;
import com.planning.backend.security.CurrentUser;

// Вішліст: у кожного свій. Бачать обоє з пари (інші пари — ні), але додавати/змінювати/видаляти можна лише свої бажання.
// Позначити бажання виконаним (подарунок отримано) може будь-хто з пари.
@Service
public class WishlistService {

    private final WishlistItemRepository items;
    private final CurrentUser currentUser;
    private final ImageStorageService imageStorage;

    public WishlistService(WishlistItemRepository items, CurrentUser currentUser, ImageStorageService imageStorage) {
        this.items = items;
        this.currentUser = currentUser;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> byOwner(Long ownerId) {
        return items.findByCouple_IdAndOwner_IdOrderByFulfilledAscCreatedAtDesc(currentUser.coupleId(), ownerId).stream()
                .map(WishlistItemResponse::from)
                .toList();
    }

    // Власник — завжди той, хто зараз увійшов
    @Transactional
    public WishlistItemResponse create(EntryRequest request) {
        User me = currentUser.get();
        WishlistItem item = new WishlistItem();
        item.setOwner(me);
        item.setCouple(me.getCouple());
        copyFields(request, item);
        return WishlistItemResponse.from(items.save(item));
    }

    @Transactional
    public WishlistItemResponse update(Long id, EntryRequest request) {
        WishlistItem item = findOwnOr403(id);
        String oldImage = item.getImageUrl();
        copyFields(request, item);
        if (oldImage != null && !oldImage.equals(item.getImageUrl())) {
            imageStorage.deleteIfLocal(oldImage);
        }
        return WishlistItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        WishlistItem item = findOwnOr403(id);
        items.delete(item);
        imageStorage.deleteIfLocal(item.getImageUrl());
    }

    @Transactional
    public WishlistItemResponse setFulfilled(Long id, boolean fulfilled) {
        WishlistItem item = findOr404(id);
        item.setFulfilled(fulfilled);
        return WishlistItemResponse.from(item);
    }

    private WishlistItem findOr404(Long id) {
        return items.findByIdAndCouple_Id(id, currentUser.coupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wish " + id + " not found"));
    }

    private WishlistItem findOwnOr403(Long id) {
        WishlistItem item = findOr404(id);
        if (!item.getOwner().getId().equals(currentUser.get().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your wish");
        }
        return item;
    }

    private static void copyFields(EntryRequest request, WishlistItem item) {
        item.setTitle(request.cleanTitle());
        item.setDescription(request.cleanDescription());
        item.setImageUrl(request.cleanImageUrl());
    }
}
