package com.planning.backend.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.client.PixabayClient;
import com.planning.backend.client.PixabayClient.DownloadedImage;

// Зберігає фото у папку на диску (backend/uploads, у Docker — том uploads-data) і видаляє їх.
// У базу записується лише короткий шлях: "/uploads/<підпапка>/<випадкове-ім'я>.jpg"
//
//   uploads/covers/       — фото карток (побачення, бажання, мрії)
//   uploads/avatars/      — аватарки
//   uploads/date-photos/  — фото з проведених побачень
@Service
public class ImageStorageService {

    public static final String URL_PREFIX = "/uploads/";
    private static final int MAX_BYTES = 10 * 1024 * 1024; // 10 МБ

    // Підпапки для різних видів фото
    public enum Folder {
        COVERS("covers"), AVATARS("avatars"), DATE_PHOTOS("date-photos");

        private final String dir;

        Folder(String dir) {
            this.dir = dir;
        }
    }

    // Дозволені типи файлів і розширення, з яким їх зберігаємо
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path root;
    private final PixabayClient pixabay;

    public ImageStorageService(@Value("${uploads.dir}") String dir, PixabayClient pixabay) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.pixabay = pixabay;
        for (Folder folder : Folder.values()) {
            Files.createDirectories(root.resolve(folder.dir));
        }
    }

    public Path getRoot() {
        return root;
    }

    // Фото, завантажене з пристрою користувача
    public String saveUpload(MultipartFile file, Folder folder) throws IOException {
        if (file.isEmpty() || !EXTENSIONS.containsKey(file.getContentType())) {
            throw badRequest("Only JPG, PNG, WEBP or GIF images are allowed");
        }
        try (InputStream content = file.getInputStream()) {
            return save(content, file.getContentType(), folder);
        }
    }

    // Фото, обране з пропозицій Pixabay. Правила Pixabay забороняють постійно
    // показувати їхні фото за прямими посиланнями, тому зберігаємо копію собі.
    public String saveFromPixabay(String url) throws IOException {
        DownloadedImage image;
        try {
            image = pixabay.download(url);
        } catch (IllegalArgumentException e) {
            throw badRequest(e.getMessage());
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not download the image");
        }
        if (image.bytes() == null || image.bytes().length > MAX_BYTES || !EXTENSIONS.containsKey(image.contentType())) {
            throw badRequest("The link is not a supported image");
        }
        return save(new ByteArrayInputStream(image.bytes()), image.contentType(), Folder.COVERS);
    }

    // Шлях на диску для нашого "/uploads/..." (null — якщо посилання зовнішнє або підозріле)
    public Path toLocalFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return null;
        }
        Path file = root.resolve(imageUrl.substring(URL_PREFIX.length())).normalize();
        return file.startsWith(root) ? file : null; // захист від "/uploads/../../щось"
    }

    // Видаляє файл, якщо imageUrl вказує на наш /uploads/ (зовнішні посилання не чіпаємо)
    public void deleteIfLocal(String imageUrl) {
        Path file = toLocalFile(imageUrl);
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Не критично: запис уже видалено, файл просто залишиться на диску
        }
    }

    // Зберігає файл під випадковим ім'ям і повертає шлях для бази
    private String save(InputStream content, String contentType, Folder folder) throws IOException {
        String fileName = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Files.copy(content, root.resolve(folder.dir).resolve(fileName));
        return URL_PREFIX + folder.dir + "/" + fileName;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
