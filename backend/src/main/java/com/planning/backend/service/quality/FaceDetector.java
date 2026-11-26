package com.planning.backend.service.quality;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Пошук облич: OpenCV + YuNet — маленька нейромережа для облич (модель 230 КБ лежить у
// resources/opencv, працює локально без інтернету). Знаходить обличчя анфас і в пів-оберта,
// у тіні та на різній відстані, а хибних спрацювань на візерунках майже не дає.
//
// Якщо native-бібліотека OpenCV не завантажилась (рідкісна ОС) — детектор вимикається,
// і оцінка фото рахується без критерію "люди".
@Component
public class FaceDetector {

    private static final Logger log = LoggerFactory.getLogger(FaceDetector.class);
    private static final String MODEL = "face_detection_yunet_2023mar.onnx";
    private static final int MAX_SIDE = 1280;           // більші фото зменшуємо
    private static final float MIN_CONFIDENCE = 0.75f;  // наскільки мережа має бути впевнена
    private static final double MIN_FACE_FRACTION = 0.025; // обличчя менше 2.5% ширини кадру ігноруємо

    private FaceDetectorYN detector;

    public FaceDetector() {
        try {
            nu.pattern.OpenCV.loadLocally();
            detector = FaceDetectorYN.create(copyModelToDisk().toString(), "", new Size(320, 320),
                    MIN_CONFIDENCE, 0.3f, 50);
            log.info("OpenCV {} face detector (YuNet) ready", Core.VERSION);
        } catch (Throwable e) { // UnsatisfiedLinkError тощо
            log.warn("OpenCV is not available, photos will be scored without face detection: {}", e.toString());
            detector = null;
        }
    }

    public boolean isAvailable() {
        return detector != null;
    }

    // Обличчя в координатах переданого зображення
    public synchronized List<Rectangle> detect(BufferedImage image) {
        if (!isAvailable()) {
            return List.of();
        }
        BufferedImage bgr = toBgr(image);
        Mat input = new Mat(bgr.getHeight(), bgr.getWidth(), CvType.CV_8UC3);
        input.put(0, 0, ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData());

        detector.setInputSize(new Size(bgr.getWidth(), bgr.getHeight()));
        Mat result = new Mat();
        detector.detect(input, result);

        // Кожен рядок результату: x, y, ширина, висота, 5 точок обличчя (очі, ніс, кутики рота), впевненість
        double k = (double) image.getWidth() / bgr.getWidth();
        List<Rectangle> faces = new ArrayList<>();
        for (int row = 0; row < result.rows(); row++) {
            double[] f = new double[15];
            for (int col = 0; col < 15; col++) {
                f[col] = result.get(row, col)[0];
            }
            if (f[2] < bgr.getWidth() * MIN_FACE_FRACTION) {
                continue; // крихітне обличчя десь на фоні — не рахуємо
            }
            faces.add(new Rectangle((int) (f[0] * k), (int) (f[1] * k), (int) (f[2] * k), (int) (f[3] * k)));
        }
        input.release();
        result.release();
        return faces;
    }

    // OpenCV хоче пікселі в порядку B, G, R
    private static BufferedImage toBgr(BufferedImage image) {
        BufferedImage scaled = ImageMetrics.scaledCopy(image, MAX_SIDE);
        BufferedImage bgr = new BufferedImage(scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        bgr.getGraphics().drawImage(scaled, 0, 0, null);
        return bgr;
    }

    // OpenCV читає модель лише з файлу на диску, тому копіюємо її з ресурсів у тимчасову папку
    private static Path copyModelToDisk() throws IOException {
        Path file = Files.createTempFile("yunet-", ".onnx");
        file.toFile().deleteOnExit();
        try (InputStream in = FaceDetector.class.getResourceAsStream("/opencv/" + MODEL)) {
            if (in == null) {
                throw new IOException("Missing resource /opencv/" + MODEL);
            }
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }
}
