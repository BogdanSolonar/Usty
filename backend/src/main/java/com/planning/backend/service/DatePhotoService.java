package com.planning.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.dto.date.DatePhotoResponse;
import com.planning.backend.dto.date.QualityBreakdown;
import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.DateIdea;
import com.planning.backend.entity.DatePhoto;
import com.planning.backend.entity.DateStatus;
import com.planning.backend.entity.User;
import com.planning.backend.repository.DatePhotoRepository;
import com.planning.backend.repository.DateRepository;
import com.planning.backend.security.CurrentUser;
import com.planning.backend.service.ImageStorageService.Folder;
import com.planning.backend.service.quality.PhotoQualityService;

import tools.jackson.databind.json.JsonMapper;

// Фото з проведених побачень: збереження, автоматична оцінка якості, видалення
@Service
public class DatePhotoService {

    private final DateRepository dates;
    private final DatePhotoRepository photos;
    private final CurrentUser currentUser;
    private final ImageStorageService imageStorage;
    private final PhotoQualityService quality;
    private final JsonMapper json;

    public DatePhotoService(DateRepository dates, DatePhotoRepository photos, CurrentUser currentUser,
            ImageStorageService imageStorage, PhotoQualityService quality, JsonMapper json) {
        this.dates = dates;
        this.photos = photos;
        this.currentUser = currentUser;
        this.imageStorage = imageStorage;
        this.quality = quality;
        this.json = json;
    }

    // Додає фото до реалізованого побачення. Кожне фото одразу оцінюється (бал + вердикт),
    // а в uploaded_by записується, хто з пари його завантажив.
    @Transactional
    public List<DatePhotoResponse> add(Long dateId, List<MultipartFile> files) throws IOException {
        DateIdea date = dates.findByIdAndCouple_Id(dateId, currentUser.coupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Date " + dateId + " not found"));
        if (date.getStatus() != DateStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Photos can be added only to a completed date");
        }
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files");
        }
        User me = currentUser.get();
        List<DatePhotoResponse> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = imageStorage.saveUpload(file, Folder.DATE_PHOTOS);
            PhotoQualityService.Result result;
            try {
                result = quality.analyze(imageStorage.toLocalFile(url));
            } catch (IOException e) {
                imageStorage.deleteIfLocal(url); // файл не читається як зображення — не зберігаємо
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image " + file.getOriginalFilename());
            }
            DatePhoto photo = new DatePhoto();
            photo.setDate(date);
            photo.setUploadedBy(me);
            photo.setPhotoUrl(url);
            photo.setQualityScore(result.score());
            photo.setQualityBreakdown(json.writeValueAsString(result.breakdown()));
            photo.setQualityVerdict(result.verdict());
            date.getPhotos().add(photo);
            saved.add(toResponse(photos.save(photo)));
        }
        return saved;
    }

    // Видалити фото може лише той, хто його завантажив
    @Transactional
    public void delete(Long dateId, Long photoId) {
        DatePhoto photo = photos.findById(photoId)
                .filter(p -> p.getDate().getId().equals(dateId))
                .filter(p -> p.getDate().getCouple().getId().equals(currentUser.coupleId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        if (!photo.getUploadedBy().getId().equals(currentUser.get().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the uploader can delete this photo");
        }
        photo.getDate().getPhotos().remove(photo); // orphanRemoval видалить рядок з date_photos
        imageStorage.deleteIfLocal(photo.getPhotoUrl());
    }

    DatePhotoResponse toResponse(DatePhoto p) {
        QualityBreakdown breakdown = p.getQualityBreakdown() == null ? null
                : json.readValue(p.getQualityBreakdown(), QualityBreakdown.class);
        return new DatePhotoResponse(p.getId(), p.getPhotoUrl(), UserResponse.from(p.getUploadedBy()),
                p.getUploadedAt(), p.getQualityScore(), p.getQualityVerdict(), breakdown);
    }
}
