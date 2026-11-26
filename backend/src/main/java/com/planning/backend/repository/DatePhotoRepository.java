package com.planning.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.planning.backend.entity.DatePhoto;

// Доступ до таблиці date_photos
public interface DatePhotoRepository extends JpaRepository<DatePhoto, Long> {

    // Для статистики: оцінки якості (1..100) усіх фото користувача — кожне фото дає свої бали
    @Query("select p.qualityScore from DatePhoto p where p.uploadedBy.id = :userId")
    List<Integer> scoresByUser(Long userId);

    // Адмінка: скільки фото завантажив користувач
    long countByUploadedBy_Id(Long userId);
}
