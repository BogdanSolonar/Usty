package com.planning.backend.client;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Усе спілкування з Pixabay в одному місці: пошук фото і скачування обраного.
// Тут немає логіки "яке фото підходить" — лише HTTP-запити.
@Component
public class PixabayClient {

    // Лише ті поля з відповіді Pixabay, які нам потрібні (решта ігнорується)
    public record Hit(String tags, String webformatURL, String largeImageURL, String user) {
    }

    record SearchResponse(List<Hit> hits) {
    }

    // Скачане зображення: байти + тип ("image/jpeg")
    public record DownloadedImage(byte[] bytes, String contentType) {
    }

    private final String apiKey;
    private final RestClient http = RestClient.create();

    public PixabayClient(@Value("${pixabay.api-key}") String apiKey) {
        this.apiKey = apiKey.trim();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    // Пошук фото англійськими ключовими словами
    public List<Hit> search(String englishQuery) {
        SearchResponse response = http.get()
                .uri("https://pixabay.com/api/?key={key}&q={q}&lang=en&image_type=photo&safesearch=true&per_page=30",
                        apiKey, englishQuery)
                .retrieve()
                .body(SearchResponse.class);
        return response == null || response.hits() == null ? List.of() : response.hits();
    }

    // Скачує зображення. Дозволяємо лише https://*.pixabay.com, щоб сервер не можна було
    // змусити ходити за довільними адресами (зокрема у вашу локальну мережу).
    public DownloadedImage download(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        boolean pixabay = "https".equals(uri.getScheme()) && host != null
                && (host.equals("pixabay.com") || host.endsWith(".pixabay.com"));
        if (!pixabay) {
            throw new IllegalArgumentException("Only pixabay.com images can be downloaded");
        }
        ResponseEntity<byte[]> response = http.get().uri(uri).retrieve().toEntity(byte[].class);
        MediaType type = response.getHeaders().getContentType();
        String contentType = type == null ? null : type.getType() + "/" + type.getSubtype();
        return new DownloadedImage(response.getBody(), contentType);
    }
}
