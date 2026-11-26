package com.planning.backend.dto.wishlist;

import java.time.Instant;

import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.WishlistItem;

public record WishlistItemResponse(
        Long id,
        String title,
        String description,
        String imageUrl,
        UserResponse owner,
        Instant createdAt,
        boolean fulfilled,
        Instant fulfilledAt) {

    public static WishlistItemResponse from(WishlistItem item) {
        return new WishlistItemResponse(item.getId(), item.getTitle(), item.getDescription(), item.getImageUrl(),
                UserResponse.from(item.getOwner()), item.getCreatedAt(), item.isFulfilled(), item.getFulfilledAt());
    }
}
