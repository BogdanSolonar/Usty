package com.planning.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.planning.backend.entity.Dream;

// Доступ до таблиці dreams. Усі вибірки — лише в межах однієї пари (couple_id).
public interface DreamRepository extends JpaRepository<Dream, Long> {

    Optional<Dream> findByIdAndCouple_Id(Long id, Long coupleId);

    // Усі мрії пари: спершу ще не здійснені, всередині — нові зверху
    List<Dream> findByCouple_IdOrderByAchievedAscCreatedAtDesc(Long coupleId);

    // Мрії одного автора
    List<Dream> findByCouple_IdAndCreatedBy_IdOrderByAchievedAscCreatedAtDesc(Long coupleId, Long authorId);

    // Усі мрії пари (видалення пари)
    List<Dream> findByCouple_Id(Long coupleId);

    // Адмінка: скільки мрій додав користувач
    long countByCreatedBy_Id(Long userId);
}
