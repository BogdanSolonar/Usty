package com.planning.backend.dto.date;

// Деталі оцінки фото, кожен показник 0..100 (faces — кількість знайдених облич).
// Зберігається в date_photos.quality_breakdown як JSON.
public record QualityBreakdown(
        int sharpness,     // чіткість (немає змазу)
        int lighting,      // освітлення / експозиція
        int people,        // люди в кадрі (2 обличчя = 100)
        int faces,
        int composition,   // розташування головного в кадрі
        int location,      // фон і кольори (насиченість, деталі)
        int resolution) {  // роздільна здатність
}
