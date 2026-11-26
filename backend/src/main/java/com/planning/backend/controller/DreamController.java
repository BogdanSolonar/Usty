package com.planning.backend.controller;

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

import com.planning.backend.dto.EntryRequest;
import com.planning.backend.dto.ToggleRequest;
import com.planning.backend.dto.dream.DreamResponse;
import com.planning.backend.service.DreamService;

import jakarta.validation.Valid;

// Мрії пари: усі разом або окремо кожного
@RestController
@RequestMapping("/api/dreams")
public class DreamController {

    private final DreamService dreamService;

    public DreamController(DreamService dreamService) {
        this.dreamService = dreamService;
    }

    // GET /api/dreams -> усі мрії пари;  GET /api/dreams?author=1 -> лише мрії користувача 1
    @GetMapping
    public List<DreamResponse> list(@RequestParam(required = false) Long author) {
        return dreamService.list(author);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DreamResponse create(@Valid @RequestBody EntryRequest request) {
        return dreamService.create(request);
    }

    @PutMapping("/{id}")
    public DreamResponse update(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        return dreamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        dreamService.delete(id);
    }

    // PUT /api/dreams/5/achieved {"value": true} -> мрія здійснилась
    @PutMapping("/{id}/achieved")
    public DreamResponse setAchieved(@PathVariable Long id, @Valid @RequestBody ToggleRequest request) {
        return dreamService.setAchieved(id, request.value());
    }
}
