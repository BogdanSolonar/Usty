package com.planning.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.dto.EntryRequest;
import com.planning.backend.dto.dream.DreamResponse;
import com.planning.backend.entity.Dream;
import com.planning.backend.entity.User;
import com.planning.backend.repository.DreamRepository;
import com.planning.backend.security.CurrentUser;

// Мрії пари: видно всі (або лише одного автора); інші пари їх не бачать. Змінювати й видаляти може автор,
// позначити "здійснилось" — будь-хто з пари.
@Service
public class DreamService {

    private final DreamRepository dreams;
    private final CurrentUser currentUser;
    private final ImageStorageService imageStorage;

    public DreamService(DreamRepository dreams, CurrentUser currentUser, ImageStorageService imageStorage) {
        this.dreams = dreams;
        this.currentUser = currentUser;
        this.imageStorage = imageStorage;
    }

    // authorId = null -> усі мрії пари
    @Transactional(readOnly = true)
    public List<DreamResponse> list(Long authorId) {
        Long coupleId = currentUser.coupleId();
        List<Dream> found = authorId == null
                ? dreams.findByCouple_IdOrderByAchievedAscCreatedAtDesc(coupleId)
                : dreams.findByCouple_IdAndCreatedBy_IdOrderByAchievedAscCreatedAtDesc(coupleId, authorId);
        return found.stream().map(DreamResponse::from).toList();
    }

    @Transactional
    public DreamResponse create(EntryRequest request) {
        User me = currentUser.get();
        Dream dream = new Dream();
        dream.setCreatedBy(me);
        dream.setCouple(me.getCouple());
        copyFields(request, dream);
        return DreamResponse.from(dreams.save(dream));
    }

    @Transactional
    public DreamResponse update(Long id, EntryRequest request) {
        Dream dream = findOwnOr403(id);
        String oldImage = dream.getImageUrl();
        copyFields(request, dream);
        if (oldImage != null && !oldImage.equals(dream.getImageUrl())) {
            imageStorage.deleteIfLocal(oldImage);
        }
        return DreamResponse.from(dream);
    }

    @Transactional
    public void delete(Long id) {
        Dream dream = findOwnOr403(id);
        dreams.delete(dream);
        imageStorage.deleteIfLocal(dream.getImageUrl());
    }

    @Transactional
    public DreamResponse setAchieved(Long id, boolean achieved) {
        Dream dream = findOr404(id);
        dream.setAchieved(achieved);
        return DreamResponse.from(dream);
    }

    private Dream findOr404(Long id) {
        return dreams.findByIdAndCouple_Id(id, currentUser.coupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dream " + id + " not found"));
    }

    private Dream findOwnOr403(Long id) {
        Dream dream = findOr404(id);
        if (!dream.getCreatedBy().getId().equals(currentUser.get().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can change this dream");
        }
        return dream;
    }

    private static void copyFields(EntryRequest request, Dream dream) {
        dream.setTitle(request.cleanTitle());
        dream.setDescription(request.cleanDescription());
        dream.setImageUrl(request.cleanImageUrl());
    }
}
