package com.planning.backend.dto.photo;

import jakarta.validation.constraints.NotBlank;

// Запит "скопіюй собі фото з Pixabay за цим посиланням"
public record RemoteImageRequest(@NotBlank String url) {
}
