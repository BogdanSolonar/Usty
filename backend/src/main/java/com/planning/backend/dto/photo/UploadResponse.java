package com.planning.backend.dto.photo;

// Відповідь після збереження фото: шлях, за яким воно тепер доступне, напр. "/uploads/abc.jpg"
public record UploadResponse(String url) {
}
