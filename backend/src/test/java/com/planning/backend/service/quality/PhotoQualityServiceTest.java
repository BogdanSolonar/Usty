package com.planning.backend.service.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.planning.backend.dto.date.QualityBreakdown;

// Оцінка якості на згенерованих картинках: змазане гірше за чітке, темне — гірше за нормальне
class PhotoQualityServiceTest {

    private static PhotoQualityService service;

    @BeforeAll
    static void setUp() {
        service = new PhotoQualityService(new FaceDetector());
    }

    @Test
    void blurredPhotoScoresLowerThanSharp() {
        BufferedImage sharp = colorfulScene(1600, 1200);
        PhotoQualityService.Result sharpResult = service.analyze(sharp);
        PhotoQualityService.Result blurredResult = service.analyze(blur(sharp));

        assertThat(sharpResult.breakdown().sharpness()).isGreaterThan(70);
        assertThat(blurredResult.breakdown().sharpness()).isLessThan(40);
        assertThat(blurredResult.score()).isLessThan(sharpResult.score());
        assertThat(blurredResult.verdict()).contains("змазаний");
    }

    @Test
    void darkPhotoGetsLowLightingAndHint() {
        BufferedImage dark = darken(colorfulScene(1600, 1200), 0.2);
        PhotoQualityService.Result result = service.analyze(dark);

        assertThat(result.breakdown().lighting()).isLessThan(45);
        assertThat(result.verdict()).containsIgnoringCase("темнувато");
    }

    @Test
    void scoreIsAlwaysBetween1And100() {
        BufferedImage black = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        PhotoQualityService.Result result = service.analyze(black);
        assertThat(result.score()).isBetween(1, 100);
        assertThat(result.breakdown().resolution()).isLessThan(45);
    }

    @Test
    void peopleScoreRewardsBothOfYou() {
        assertThat(PhotoQualityService.peopleScore(2)).isEqualTo(100);
        assertThat(PhotoQualityService.peopleScore(1)).isLessThan(100);
        assertThat(PhotoQualityService.peopleScore(0)).isLessThan(PhotoQualityService.peopleScore(1));
    }

    @Test
    void verdictMentionsTheBestParts() {
        QualityBreakdown great = new QualityBreakdown(90, 90, 100, 2, 70, 60, 100);
        assertThat(PhotoQualityService.verdict(great, 120, true))
                .isEqualTo("Чудове світло, чіткий кадр, видно вас обох");
    }

    @Test
    void resolutionScale() {
        assertThat(PhotoQualityService.resolutionScore(2000, 1000)).isEqualTo(100);
        assertThat(PhotoQualityService.resolutionScore(640, 480)).isLessThan(45);
    }

    // "Сцена": кольорові фігури + дрібний шум, щоб були і кольори, і деталі
    private static BufferedImage colorfulScene(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random random = new Random(42);
        g.setColor(new Color(90, 140, 200));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 300; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillRect(random.nextInt(w), random.nextInt(h), 10 + random.nextInt(120), 10 + random.nextInt(120));
        }
        g.dispose();
        // дрібна "текстура", як у справжніх фото (трава, тканина, шкіра)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int noise = random.nextInt(41) - 20;
                Color c = new Color(img.getRGB(x, y));
                img.setRGB(x, y, new Color(fit(c.getRed() + noise), fit(c.getGreen() + noise), fit(c.getBlue() + noise)).getRGB());
            }
        }
        return img;
    }

    private static int fit(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static BufferedImage blur(BufferedImage src) {
        BufferedImage small = ImageMetrics.scaledCopy(src, 100);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(small, 0, 0, src.getWidth(), src.getHeight(), null);
        g.dispose();
        return out;
    }

    private static BufferedImage darken(BufferedImage src, double k) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                Color c = new Color(src.getRGB(x, y));
                out.setRGB(x, y, new Color((int) (c.getRed() * k), (int) (c.getGreen() * k), (int) (c.getBlue() * k)).getRGB());
            }
        }
        return out;
    }
}
