package com.planning.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.planning.backend.entity.DateIdea;
import com.planning.backend.entity.DateStatus;

// Доступ до таблиці dates. Усі вибірки — лише в межах однієї пари (couple_id).
public interface DateRepository extends JpaRepository<DateIdea, Long> {

    Optional<DateIdea> findByIdAndCouple_Id(Long id, Long coupleId);

    // Активні (IDEA + PLANNED), нові зверху
    List<DateIdea> findByCouple_IdAndStatusInOrderByCreatedAtDesc(Long coupleId, Collection<DateStatus> statuses);

    // Історія: COMPLETED, найсвіжіші побачення зверху
    List<DateIdea> findByCouple_IdAndStatusOrderByCompletedAtDescIdDesc(Long coupleId, DateStatus status);

    // Усі побачення пари (видалення пари)
    List<DateIdea> findByCouple_Id(Long coupleId);

    // Для статистики: скільки ідей додав користувач
    long countByCreatedBy_Id(Long userId);

    // Для статистики: скільки побачень користувач ініціював (заплановані + реалізовані)
    long countByInitiatedBy_IdAndStatusIn(Long userId, Collection<DateStatus> statuses);
}
