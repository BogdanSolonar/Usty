package com.planning.backend.service.quality;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.planning.backend.dto.date.QualityBreakdown;

// Автоматична оцінка якості фото з побачення: бал 1..100 + короткий вердикт.
//
// Критерії (вага у підсумку):
//   чіткість     25%  — дисперсія лапласіана в найрізкіших ділянках (змаз -> мало)
//   люди         25%  — скільки облич знайшов OpenCV (двоє = 100)
//   освітлення   20%  — середня яскравість близька до "середньої", є контраст, мало вигорілого/чорного
//   композиція   15%  — головне (обличчя або найрізкіша ділянка) біля ліній третин
//   локація      15%  — фон: насиченість кольорів і кількість деталей
// Роздільна здатність працює як множник: фото < 1 Мп втрачає до 25% балу.
@Service
public class PhotoQualityService {

    // Результат: бал, деталі і текст для людей
    public record Result(int score, QualityBreakdown breakdown, String verdict) {
    }

    private final FaceDetector faceDetector;

    public PhotoQualityService(FaceDetector faceDetector) {
        this.faceDetector = faceDetector;
        ImageIO.scanForPlugins(); // підключаємо читання WEBP / CMYK-JPEG (TwelveMonkeys)
    }

    public Result analyze(Path file) throws IOException {
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("Unsupported image format");
        }
        return analyze(image);
    }

    Result analyze(BufferedImage original) {
        int resolution = resolutionScore(original.getWidth(), original.getHeight());
        // Далі працюємо зі зменшеною копією (до 1280 px): швидко і для всіх фото однаково
        BufferedImage image = ImageMetrics.scaledCopy(original, 1280);

        // Обличчя. Фото з телефона часто лежать "на боці" (поворот записаний лише в EXIF),
        // тож якщо облич не видно — пробуємо повернути кадр на 90° в обидва боки.
        List<Rectangle> faces = faceDetector.detect(image);
        if (faces.isEmpty() && faceDetector.isAvailable()) {
            for (int quarterTurns : new int[] {1, 3}) {
                BufferedImage rotated = rotate(image, quarterTurns);
                List<Rectangle> found = faceDetector.detect(rotated);
                if (!found.isEmpty()) {
                    image = rotated;
                    faces = found;
                    break;
                }
            }
        }

        ImageMetrics.Pixels px = ImageMetrics.downscale(image, 640);
        double[][] grid = ImageMetrics.laplacianVarianceGrid(px, 4);
        ImageMetrics.Exposure exposure = ImageMetrics.exposure(px);

        int sharpness = sharpnessScore(ImageMetrics.sharpness(grid));
        int lighting = lightingScore(exposure);
        int location = locationScore(ImageMetrics.colorfulness(px), ImageMetrics.entropy(px));
        int people = faceDetector.isAvailable() ? peopleScore(faces.size()) : 60; // без OpenCV — нейтрально
        int composition = compositionScore(faces, image.getWidth(), image.getHeight(), grid);

        double weighted = 0.25 * sharpness + 0.25 * people + 0.20 * lighting + 0.15 * composition + 0.15 * location;
        int score = Math.max(1, ImageMetrics.clamp(weighted * (0.75 + 0.25 * resolution / 100.0)));

        QualityBreakdown breakdown = new QualityBreakdown(sharpness, lighting, people, faces.size(), composition,
                location, resolution);
        return new Result(score, breakdown, verdict(breakdown, exposure.mean(), faceDetector.isAvailable()));
    }

    // ---------- Перетворення сирих вимірів у бали 0..100 ----------

    // Логарифмічна шкала: дисперсія 30 і менше = 0 (сильний змаз), 1500 і більше = 100 (дуже чітко).
    // (Підібрано на реальних фото: чіткі кадри дають 600..10000, розмиті — 10..70.)
    static int sharpnessScore(double laplacianVariance) {
        double v = Math.max(1, laplacianVariance);
        return ImageMetrics.clamp((Math.log(v) - Math.log(30)) / (Math.log(1500) - Math.log(30)) * 100);
    }

    static int lightingScore(ImageMetrics.Exposure e) {
        double exposure = 100 - Math.abs(e.mean() - 120) / 95 * 100; // ідеал — середня яскравість ~120
        double contrast = e.stdDev() / 55 * 100;                     // пласке сіре фото -> мало
        // вигоріле небо / провали в чорне; трохи — нормально (захід сонця, силуети)
        double clippedPenalty = Math.min(40, Math.max(0, e.clipped() - 0.05) * 100);
        return ImageMetrics.clamp(0.55 * Math.max(0, exposure) + 0.45 * Math.min(100, contrast) - clippedPenalty);
    }

    static int locationScore(double colorfulness, double entropy) {
        double colors = Math.min(100, colorfulness / 55 * 100);
        double details = Math.max(0, Math.min(100, (entropy - 4) / 3.5 * 100));
        return ImageMetrics.clamp(0.6 * colors + 0.4 * details);
    }

    static int peopleScore(int faces) {
        return switch (faces) {
            case 0 -> 20;
            case 1 -> 65;
            case 2 -> 100; // ви обоє в кадрі — те, що треба
            default -> 85;  // компанія
        };
    }

    static int compositionScore(List<Rectangle> faces, int width, int height, double[][] grid) {
        double x, y;
        double penalty = 0;
        if (faces.isEmpty()) {
            double[] c = ImageMetrics.sharpestCellCenter(grid);
            x = c[0];
            y = c[1];
        } else {
            // центр групи облич
            Rectangle all = new Rectangle(faces.get(0));
            faces.forEach(all::add);
            x = all.getCenterX() / width;
            y = all.getCenterY() / height;
            double area = faces.stream().mapToDouble(f -> f.getWidth() * f.getHeight()).sum() / ((double) width * height);
            if (area < 0.004) {
                penalty += 20; // люди ледь помітні, дуже далеко
            }
            boolean cutOff = all.x <= 2 || all.y <= 2 || all.getMaxX() >= width - 2 || all.getMaxY() >= height - 2;
            if (cutOff) {
                penalty += 15; // обличчя "обрізані" краєм кадру
            }
        }
        double distance = ImageMetrics.distanceToPowerPoint(x, y); // 0 = ідеально, ~0.33 = в кутку
        return ImageMetrics.clamp(100 - distance / 0.25 * 60 - penalty);
    }

    // 2 Мп і більше = 100; 1 Мп ≈ 71; 0.3 Мп ≈ 39
    static int resolutionScore(int width, int height) {
        double megapixels = (double) width * height / 1_000_000;
        return ImageMetrics.clamp(Math.sqrt(megapixels / 2) * 100);
    }

    // ---------- Вердикт ----------

    static String verdict(QualityBreakdown b, double meanBrightness, boolean facesChecked) {
        List<String> good = new ArrayList<>();
        List<String> bad = new ArrayList<>();

        if (b.lighting() >= 75) {
            good.add("чудове світло");
        } else if (b.lighting() < 45) {
            bad.add(meanBrightness < 90 ? "темнувато" : meanBrightness > 170 ? "пересвічено" : "пласке світло");
        }
        if (b.sharpness() >= 75) {
            good.add("чіткий кадр");
        } else if (b.sharpness() < 40) {
            bad.add("кадр змазаний");
        }
        if (facesChecked) {
            switch (b.faces()) {
                case 0 -> bad.add("облич не видно");
                case 1 -> good.add("видно одне обличчя");
                case 2 -> good.add("видно вас обох");
                default -> good.add("у кадрі ціла компанія");
            }
        }
        if (b.composition() >= 80) {
            good.add("гарна композиція");
        }
        if (b.location() >= 75) {
            good.add("яскраві кольори");
        } else if (b.location() < 35) {
            bad.add("бляклі кольори");
        }
        if (b.resolution() < 45) {
            bad.add("низька роздільна здатність");
        }

        List<String> parts = new ArrayList<>(good.subList(0, Math.min(3, good.size())));
        parts.addAll(bad.subList(0, Math.min(2, bad.size())));
        if (parts.isEmpty()) {
            return "Звичайний кадр";
        }
        String text = String.join(", ", parts);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static BufferedImage rotate(BufferedImage src, int quarterTurns) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getRGB(x, y);
                if (quarterTurns == 1) {
                    dst.setRGB(h - 1 - y, x, p); // 90° за годинниковою
                } else {
                    dst.setRGB(y, w - 1 - x, p); // 90° проти годинникової
                }
            }
        }
        return dst;
    }
}
