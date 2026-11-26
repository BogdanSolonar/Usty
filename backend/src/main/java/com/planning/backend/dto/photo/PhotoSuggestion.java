package com.planning.backend.dto.photo;

// Одна пропозиція фото для форми створення запису
public record PhotoSuggestion(
        String previewUrl,    // менша версія — для вибору у формі
        String imageUrl,      // велика версія — її сервер скопіює собі, якщо фото оберуть
        String alt,           // теги фото
        String photographer) {
}
