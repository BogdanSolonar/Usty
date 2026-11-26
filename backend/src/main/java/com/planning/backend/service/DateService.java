package com.planning.backend.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.dto.EntryRequest;
import com.planning.backend.dto.date.CompleteRequest;
import com.planning.backend.dto.date.DateResponse;
import com.planning.backend.dto.date.PlanRequest;
import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.DateIdea;
import com.planning.backend.entity.DatePhoto;
import com.planning.backend.entity.DateStatus;
import com.planning.backend.entity.User;
import com.planning.backend.repository.DateRepository;
import com.planning.backend.security.CurrentUser;
import com.planning.backend.service.NotificationService.PlannedDate;

// Побачення: ідея -> "Реалізувати" (дата + примітка, сповіщення в Telegram) -> "Реалізовано" (Історія)
@Service
public class DateService {

    private final DateRepository dates;
    private final CurrentUser currentUser;
    private final ImageStorageService imageStorage;
    private final DatePhotoService photoService;
    private final NotificationService notifications;

    public DateService(DateRepository dates, CurrentUser currentUser, ImageStorageService imageStorage,
            DatePhotoService photoService, NotificationService notifications) {
        this.dates = dates;
        this.currentUser = currentUser;
        this.imageStorage = imageStorage;
        this.photoService = photoService;
        this.notifications = notifications;
    }

    // Активні: спершу заплановані (найближчі першими), потім ідеї (нові першими)
    @Transactional(readOnly = true)
    public List<DateResponse> active() {
        Comparator<DateIdea> planned = Comparator.comparing(d -> d.getStatus() != DateStatus.PLANNED);
        Comparator<DateIdea> byPlannedDate = Comparator.comparing(DateIdea::getPlannedDate,
                Comparator.nullsLast(Comparator.naturalOrder()));
        return dates.findByCouple_IdAndStatusInOrderByCreatedAtDesc(currentUser.coupleId(),
                EnumSet.of(DateStatus.IDEA, DateStatus.PLANNED)).stream()
                .sorted(planned.thenComparing(byPlannedDate)) // сортування стабільне: ідеї лишаються "нові першими"
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DateResponse> history() {
        return dates.findByCouple_IdAndStatusOrderByCompletedAtDescIdDesc(currentUser.coupleId(), DateStatus.COMPLETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DateResponse get(Long id) {
        return toResponse(findOr404(id));
    }

    @Transactional
    public DateResponse create(EntryRequest request) {
        User me = currentUser.get();
        DateIdea date = new DateIdea();
        date.setCreatedBy(me); // автор — той, хто зараз увійшов
        date.setCouple(me.getCouple()); // і побачення належить його парі
        copyFields(request, date);
        return toResponse(dates.save(date));
    }

    @Transactional
    public DateResponse update(Long id, EntryRequest request) {
        DateIdea date = findOr404(id);
        String oldCover = date.getCoverImageUrl();
        copyFields(request, date);
        if (oldCover != null && !oldCover.equals(date.getCoverImageUrl())) {
            imageStorage.deleteIfLocal(oldCover);
        }
        return toResponse(date);
    }

    // Видаляє побачення, його фото з бази (каскадом) і всі файли з диска
    @Transactional
    public void delete(Long id) {
        DateIdea date = findOr404(id);
        List<String> files = date.getPhotos().stream().map(DatePhoto::getPhotoUrl).toList();
        String cover = date.getCoverImageUrl();
        dates.delete(date);
        imageStorage.deleteIfLocal(cover);
        files.forEach(imageStorage::deleteIfLocal);
    }

    // "Реалізувати": обрана дата + примітка, фіксуємо ініціатора і шлемо сповіщення в Telegram
    @Transactional
    public DateResponse plan(Long id, PlanRequest request) {
        DateIdea date = findOr404(id);
        if (date.getStatus() == DateStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Date is already completed");
        }
        if (request.plannedDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planned date is in the past");
        }
        User me = currentUser.get();
        date.setStatus(DateStatus.PLANNED);
        date.setPlannedDate(request.plannedDate());
        date.setPlannedNote(blankToNull(request.note()));
        date.setInitiatedBy(me);

        notifications.datePlanned(
                new PlannedDate(date.getTitle(), date.getPlannedDate(), date.getPlannedNote(), me.getName()),
                notifications.recipients(me.getCoupleId()));
        return toResponse(date);
    }

    // Скасувати план: побачення знову стає просто ідеєю
    @Transactional
    public DateResponse cancelPlan(Long id) {
        DateIdea date = findOr404(id);
        if (date.getStatus() != DateStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Date is not planned");
        }
        date.setStatus(DateStatus.IDEA);
        date.setPlannedDate(null);
        date.setPlannedNote(null);
        date.setInitiatedBy(null);
        return toResponse(date);
    }

    // "Реалізовано": побачення переходить в Історію. Якщо його не планували через календар,
    // ініціатором вважається той, хто натиснув "Реалізовано".
    @Transactional
    public DateResponse complete(Long id, CompleteRequest request) {
        DateIdea date = findOr404(id);
        if (request.completedAt().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completion date is in the future");
        }
        if (date.getInitiatedBy() == null) {
            date.setInitiatedBy(currentUser.get());
        }
        date.setStatus(DateStatus.COMPLETED);
        date.setCompletedAt(request.completedAt());
        return toResponse(date);
    }

    // Чуже (з іншої пари) побачення виглядає так само, як неіснуюче — 404
    DateIdea findOr404(Long id) {
        return dates.findByIdAndCouple_Id(id, currentUser.coupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Date " + id + " not found"));
    }

    private DateResponse toResponse(DateIdea d) {
        return new DateResponse(d.getId(), d.getTitle(), d.getDescription(), d.getCoverImageUrl(), d.getStatus(),
                d.getCreatedAt(), UserResponse.from(d.getCreatedBy()), d.getPlannedDate(), d.getPlannedNote(),
                UserResponse.from(d.getInitiatedBy()), d.getCompletedAt(),
                d.getPhotos().stream().map(photoService::toResponse).toList());
    }

    private static void copyFields(EntryRequest request, DateIdea date) {
        date.setTitle(request.cleanTitle());
        date.setDescription(request.cleanDescription());
        date.setCoverImageUrl(request.cleanImageUrl());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
