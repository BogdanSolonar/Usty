package com.planning.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.planning.backend.dto.photo.PhotoSuggestion;
import com.planning.backend.service.PhotoSearchService;

// Підбір фото за назвою. Фронтенд звертається сюди, а не напряму до Pixabay,
// щоб API-ключ залишався на сервері й не був видимий у браузері.
@RestController
public class PhotoController {

    private final PhotoSearchService photoSearch;

    public PhotoController(PhotoSearchService photoSearch) {
        this.photoSearch = photoSearch;
    }

    // GET /api/photos?query=пікнік у парку  -> до 6 пропозицій або порожній список
    @GetMapping("/api/photos")
    public List<PhotoSuggestion> search(@RequestParam String query) {
        return photoSearch.search(query);
    }
}
