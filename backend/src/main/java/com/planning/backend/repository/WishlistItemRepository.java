package com.planning.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.planning.backend.entity.WishlistItem;

// Доступ до таблиці wishlist_items. Усі вибірки — лише в межах однієї пари (couple_id).
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByIdAndCouple_Id(Long id, Long coupleId);

    // Вішліст однієї людини: спершу невиконані, всередині — нові зверху
    List<WishlistItem> findByCouple_IdAndOwner_IdOrderByFulfilledAscCreatedAtDesc(Long coupleId, Long ownerId);

    // Усі бажання пари (видалення пари)
    List<WishlistItem> findByCouple_Id(Long coupleId);

    // Адмінка: скільки бажань додав користувач
    long countByOwner_Id(Long userId);
}
