package com.planning.backend.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.planning.backend.dto.EntryRequest;
import com.planning.backend.dto.date.CompleteRequest;
import com.planning.backend.dto.date.DatePhotoResponse;
import com.planning.backend.dto.date.DateResponse;
import com.planning.backend.dto.date.PlanRequest;
import com.planning.backend.service.DatePhotoService;
import com.planning.backend.service.DateService;

import jakarta.validation.Valid;

// Побачення: ідеї, планування, реалізація, фото
@RestController
@RequestMapping("/api/dates")
public class DateController {

    private final DateService dateService;
    private final DatePhotoService photoService;

    public DateController(DateService dateService, DatePhotoService photoService) {
        this.dateService = dateService;
        this.photoService = photoService;
    }

    // GET /api/dates              -> активні (ідеї + заплановані)
    // GET /api/dates?view=history -> реалізовані (Історія)
    @GetMapping
    public List<DateResponse> list(@RequestParam(defaultValue = "active") String view) {
        return "history".equals(view) ? dateService.history() : dateService.active();
    }

    @GetMapping("/{id}")
    public DateResponse get(@PathVariable Long id) {
        return dateService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DateResponse create(@Valid @RequestBody EntryRequest request) {
        return dateService.create(request);
    }

    @PutMapping("/{id}")
    public DateResponse update(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        return dateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        dateService.delete(id);
    }

    // POST /api/dates/5/plan {"plannedDate": "2026-12-01", "note": "..."} -> PLANNED + сповіщення в Telegram
    @PostMapping("/{id}/plan")
    public DateResponse plan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return dateService.plan(id, request);
    }

    // DELETE /api/dates/5/plan -> скасувати план (знову IDEA)
    @DeleteMapping("/{id}/plan")
    public DateResponse cancelPlan(@PathVariable Long id) {
        return dateService.cancelPlan(id);
    }

    // POST /api/dates/5/complete {"completedAt": "2026-12-01"} -> COMPLETED (переходить в Історію)
    @PostMapping("/{id}/complete")
    public DateResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest request) {
        return dateService.complete(id, request);
    }

    // POST /api/dates/5/photos (multipart, одне або кілька полів "files") -> збережені фото з оцінками
    @PostMapping("/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<DatePhotoResponse> addPhotos(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files)
            throws IOException {
        return photoService.add(id, files);
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        photoService.delete(id, photoId);
    }
}
