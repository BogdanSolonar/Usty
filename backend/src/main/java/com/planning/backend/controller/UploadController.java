package com.planning.backend.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.planning.backend.dto.photo.RemoteImageRequest;
import com.planning.backend.dto.photo.UploadResponse;
import com.planning.backend.service.ImageStorageService;
import com.planning.backend.service.ImageStorageService.Folder;

import jakarta.validation.Valid;

// Збереження фото для карток (обкладинки). Відповідь: {"url": "/uploads/covers/abc.jpg"}
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorageService imageStorage;

    public UploadController(ImageStorageService imageStorage) {
        this.imageStorage = imageStorage;
    }

    // POST /api/uploads (multipart/form-data, поле "file") -> фото з пристрою
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        return new UploadResponse(imageStorage.saveUpload(file, Folder.COVERS));
    }

    // POST /api/uploads/from-url  {"url": "https://pixabay.com/get/..."} -> копія фото з Pixabay
    @PostMapping("/from-url")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse uploadFromUrl(@Valid @RequestBody RemoteImageRequest request) throws IOException {
        return new UploadResponse(imageStorage.saveFromPixabay(request.url()));
    }
}
