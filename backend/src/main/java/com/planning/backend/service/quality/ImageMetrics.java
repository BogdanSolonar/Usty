package com.planning.backend.service.quality;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

// "Технічні" вимірювання зображення — проста математика над пікселями, без AI.
// Кожен метод повертає сире число; у бали 0..100 їх переводить PhotoQualityService.
final class ImageMetrics {

    private ImageMetrics() {
    }

    // Зменшене зображення у форматі RGB + яскравість (0..255) кожного пікселя
    record Pixels(int width, int height, int[] rgb, double[] luma) {
    }

    // Зменшуємо до maxSide по довшій стороні: аналіз швидкий і не залежить від мегапікселів
    static Pixels downscale(BufferedImage source, int maxSide) {
        BufferedImage small = scaledCopy(source, maxSide);
        int w = small.getWidth(), h = small.getHeight();
        int[] rgb = small.getRGB(0, 0, w, h, null, 0, w);
        double[] luma = new double[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int p = rgb[i];
            luma[i] = 0.299 * ((p >> 16) & 0xff) + 0.587 * ((p >> 8) & 0xff) + 0.114 * (p & 0xff);
        }
        return new Pixels(w, h, rgb, luma);
    }

    // Копія зображення в RGB, зменшена до maxSide по довшій стороні (більші не збільшуємо)
    static BufferedImage scaledCopy(BufferedImage source, int maxSide) {
        double k = Math.min(1.0, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
        int w = Math.max(1, (int) Math.round(source.getWidth() * k));
        int h = Math.max(1, (int) Math.round(source.getHeight() * k));
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return copy;
    }

    // ---------- Різкість ----------

    // Дисперсія лапласіана (наскільки різко змінюється яскравість між сусідніми пікселями)
    // у кожній клітинці сітки cells x cells. Розмите фото -> малі значення.
    static double[][] laplacianVarianceGrid(Pixels px, int cells) {
        int w = px.width(), h = px.height();
        double[][] sum = new double[cells][cells];
        double[][] sumSq = new double[cells][cells];
        int[][] count = new int[cells][cells];
        double[] l = px.luma();
        for (int y = 1; y < h - 1; y++) {
            int cy = Math.min(cells - 1, y * cells / h);
            for (int x = 1; x < w - 1; x++) {
                int i = y * w + x;
                double lap = l[i - 1] + l[i + 1] + l[i - w] + l[i + w] - 4 * l[i];
                int cx = Math.min(cells - 1, x * cells / w);
                sum[cy][cx] += lap;
                sumSq[cy][cx] += lap * lap;
                count[cy][cx]++;
            }
        }
        double[][] variance = new double[cells][cells];
        for (int cy = 0; cy < cells; cy++) {
            for (int cx = 0; cx < cells; cx++) {
                int n = Math.max(1, count[cy][cx]);
                double mean = sum[cy][cx] / n;
                variance[cy][cx] = sumSq[cy][cx] / n - mean * mean;
            }
        }
        return variance;
    }

    // Різкість = середнє 3 найрізкіших клітинок. Так фото з розмитим фоном (боке),
    // але чіткими людьми, не вважається змазаним.
    static double sharpness(double[][] grid) {
        double[] all = Arrays.stream(grid).flatMapToDouble(Arrays::stream).sorted().toArray();
        int top = Math.min(3, all.length);
        double s = 0;
        for (int i = all.length - top; i < all.length; i++) {
            s += all[i];
        }
        return s / top;
    }

    // ---------- Освітлення ----------

    record Exposure(double mean, double stdDev, double clipped) {
    }

    // Середня яскравість, контраст (стандартне відхилення) і частка "вигорілих"/чорних пікселів
    static Exposure exposure(Pixels px) {
        double[] l = px.luma();
        double sum = 0, sumSq = 0;
        int clipped = 0;
        for (double v : l) {
            sum += v;
            sumSq += v * v;
            if (v < 8 || v > 247) {
                clipped++;
            }
        }
        double mean = sum / l.length;
        double std = Math.sqrt(Math.max(0, sumSq / l.length - mean * mean));
        return new Exposure(mean, std, (double) clipped / l.length);
    }

    // ---------- Кольори і деталі фону ----------

    // Метрика "колоритності" Хаслера і Зюсстрюнка (2003): 0 = сіре, ~50 = помірно, 80+ = дуже яскраве
    static double colorfulness(Pixels px) {
        int[] rgb = px.rgb();
        double sRg = 0, sRg2 = 0, sYb = 0, sYb2 = 0;
        for (int p : rgb) {
            int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
            double rg = r - g;
            double yb = 0.5 * (r + g) - b;
            sRg += rg;
            sRg2 += rg * rg;
            sYb += yb;
            sYb2 += yb * yb;
        }
        int n = rgb.length;
        double mRg = sRg / n, mYb = sYb / n;
        double stdRg = Math.sqrt(Math.max(0, sRg2 / n - mRg * mRg));
        double stdYb = Math.sqrt(Math.max(0, sYb2 / n - mYb * mYb));
        return Math.sqrt(stdRg * stdRg + stdYb * stdYb) + 0.3 * Math.sqrt(mRg * mRg + mYb * mYb);
    }

    // Ентропія гістограми яскравості в бітах (0..8): скільки "різноманіття" в кадрі.
    // Однотонна стіна ~3-4, насичена деталями сцена 7+.
    static double entropy(Pixels px) {
        int[] hist = new int[256];
        for (double v : px.luma()) {
            hist[(int) Math.max(0, Math.min(255, v))]++;
        }
        double n = px.luma().length, h = 0;
        for (int c : hist) {
            if (c > 0) {
                double p = c / n;
                h -= p * (Math.log(p) / Math.log(2));
            }
        }
        return h;
    }

    // ---------- Композиція ----------

    // Точки, куди за правилом третин варто ставити головне (+ трохи вище центру — для портретів удвох)
    private static final double[][] POWER_POINTS = {
            {1 / 3.0, 1 / 3.0}, {2 / 3.0, 1 / 3.0}, {1 / 3.0, 2 / 3.0}, {2 / 3.0, 2 / 3.0}, {0.5, 0.42}};

    // Відстань (у частках кадру) від головного об'єкта до найближчої "сильної точки"
    static double distanceToPowerPoint(double x, double y) {
        double best = Double.MAX_VALUE;
        for (double[] p : POWER_POINTS) {
            best = Math.min(best, Math.hypot(x - p[0], y - p[1]));
        }
        return best;
    }

    // Центр найрізкішої клітинки — коли облич немає, вважаємо головним об'єктом її
    static double[] sharpestCellCenter(double[][] grid) {
        int cells = grid.length, bx = 0, by = 0;
        for (int y = 0; y < cells; y++) {
            for (int x = 0; x < cells; x++) {
                if (grid[y][x] > grid[by][bx]) {
                    bx = x;
                    by = y;
                }
            }
        }
        return new double[] {(bx + 0.5) / cells, (by + 0.5) / cells};
    }

    static int clamp(double value) {
        return (int) Math.round(Math.max(0, Math.min(100, value)));
    }
}
