package com.planning.backend.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.client.PixabayClient;
import com.planning.backend.client.TranslationClient;
import com.planning.backend.dto.photo.PhotoSuggestion;

// Підбір фото за назвою запису.
//
// Алгоритм:
//   1. QueryChecker відсіює беззмістовний текст ("фывапролдж", "ааааа")
//   2. Українську назву перекладаємо на англійську (Pixabay не знає української)
//   3. З перекладу беремо ключові слова (без "the", "for", "new"...)
//   4. Шукаємо в Pixabay
//   5. Залишаємо лише фото, в тегах яких є хоча б одне ключове слово,
//      і ставимо першими ті, де збігів найбільше
@Service
public class PhotoSearchService {

    private static final int MAX_SUGGESTIONS = 6;

    // Слова, які нічого не кажуть про зміст фото
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "near", "our", "your", "my", "his", "her", "its", "their",
            "let", "lets", "let's", "go", "get", "make", "take", "some", "very", "new", "old", "big", "small",
            "good", "best", "one", "two", "this", "that", "into", "onto", "about", "want", "will", "would");

    // Фото + кількість збігів його тегів із запитом
    private record ScoredHit(PixabayClient.Hit photo, int score) {
    }

    private final PixabayClient pixabay;
    private final TranslationClient translator;

    public PhotoSearchService(PixabayClient pixabay, TranslationClient translator) {
        this.pixabay = pixabay;
        this.translator = translator;
    }

    public List<PhotoSuggestion> search(String query) {
        if (!pixabay.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Pixabay API key is missing: add it to backend/secrets.properties");
        }
        // Фільтр 1: беззмістовний текст навіть не надсилаємо далі
        if (!QueryChecker.isMeaningful(query)) {
            return List.of();
        }

        List<String> keywords = keywords(translator.ukrainianToEnglish(query.trim()));
        if (keywords.isEmpty()) {
            return List.of();
        }

        List<PixabayClient.Hit> hits;
        try {
            hits = pixabay.search(String.join(" ", keywords));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Pixabay request failed");
        }

        // Фільтр 2: лише фото, теги яких збігаються з ключовими словами
        return hits.stream()
                .map(hit -> new ScoredHit(hit, relevance(hit.tags(), keywords)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredHit::score).reversed()) // найбільше збігів — першими
                .limit(MAX_SUGGESTIONS)
                .map(scored -> new PhotoSuggestion(
                        scored.photo().webformatURL(), scored.photo().largeImageURL(),
                        scored.photo().tags(), scored.photo().user()))
                .toList();
    }

    // "Let's go for a ride on ATVs." -> [ride, atvs]
    static List<String> keywords(String english) {
        List<String> result = new ArrayList<>();
        for (String word : english.toLowerCase().split("[^a-z]+")) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word) && !result.contains(word)) {
                result.add(word);
            }
        }
        return result;
    }

    // Скільки ключових слів знайшлося в тегах фото.
    // Порівнюємо за початком слова, щоб "flowers" збігалося з "flower", а "atvs" з "atv".
    static int relevance(String tags, List<String> keywords) {
        List<String> tagWords = Arrays.stream(tags.toLowerCase().split("[^a-z]+"))
                .filter(t -> t.length() >= 3)
                .toList();
        int score = 0;
        for (String keyword : keywords) {
            String stem = keyword.length() > 5 ? keyword.substring(0, keyword.length() - 2) : keyword;
            boolean found = tagWords.stream().anyMatch(tag -> tag.startsWith(stem) || keyword.startsWith(tag));
            if (found) {
                score++;
            }
        }
        return score;
    }
}
