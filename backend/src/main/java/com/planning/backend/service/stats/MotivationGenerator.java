package com.planning.backend.service.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

import com.planning.backend.dto.stats.Motivation;
import com.planning.backend.dto.stats.PlayerStats;

// Генератор теплих підказок для статистики.
//
// Кожен з пари отримує СВОЮ фразу (StatsService віддає людині лише її власну).
// Хто позаду за сумою балів — фразу під свій рівень відставання:
//   1–15 балів  SMALL   ("лише один крок до лідерства")
//   16–40       MEDIUM  ("чудовий привід влаштувати сюрприз")
//   41+         BIG     ("одна крута пригода все переверне")
// плюс конкретний крок з реальними цифрами — в активності, де відставання найбільше
// ("Заплануй 2 побачення — і ти попереду").
// Хто попереду — LEAD: тепла фраза без хизування.
// Якщо порівну — обоє отримують фразу про баланс (або про старт, якщо балів ще немає).
//
// Фрази з пулу обираються за "зерном" (день + поточні бали): протягом дня текст стабільний,
// а змінюється щодня й після кожної нової дії — щоб не набридав.
public final class MotivationGenerator {

    static final int SMALL_MAX = 15;
    static final int MEDIUM_MAX = 40;

    // Фраза + найбільший відрив, за якого вона ще правдива ("1 ідея" обганяє лише при відриві < 10)
    record Phrase(String text, long maxGap) {
        Phrase(String text) {
            this(text, Long.MAX_VALUE);
        }
    }

    static final List<Phrase> START = List.of(
            new Phrase("Чистий аркуш — додайте першу ідею й почніть вашу історію ✨"),
            new Phrase("Усе попереду! Перша ідея побачення — найприємніший старт 💫"),
            new Phrase("Тут поки тихо — саме час запланувати перше побачення ❤️"),
            new Phrase("Перший крок завжди найцінніший — хто з вас придумає ідею першим? 😉"));

    static final List<Phrase> TIE = List.of(
            new Phrase("Ідеальний баланс! Ви рухаєтесь пліч-о-пліч ✨"),
            new Phrase("Абсолютна гармонія — час для нового спільного кроку!"),
            new Phrase("Нічия — найромантичніший рахунок. Хто зробить наступний крок? 😉"),
            new Phrase("Ви ідеально синхронні — наступна ідея вирішить, хто поведе далі 💞"),
            new Phrase("Рівно-рівно! Саме час придумати щось разом ✨"));

    static final List<Phrase> SMALL = List.of(
            new Phrase("Тобі залишилось усього нічого — придумай 1 нову ідею побачення, і ти вже попереду!",
                    StatsPoints.IDEA - 1),
            new Phrase("Лише один крок до лідерства — час запланувати щось особливе 💫",
                    StatsPoints.INITIATIVE - 1),
            new Phrase("Одне класне фото з наступної зустрічі легко змінить баланс!", StatsPoints.PHOTO_MAX - 1),
            new Phrase("Ви майже на рівних — маленький сюрприз, і ти попереду 🌷"),
            new Phrase("Відрив зовсім крихітний — одна приємна дрібниця все змінить ✨"));

    static final List<Phrase> MEDIUM = List.of(
            new Phrase("Чудовий привід влаштувати сюрприз цього тижня — одне ініційоване побачення одразу скоротить відрив!"),
            new Phrase("У тебе є всі шанси зробити красивий камбек: додай пару свіжих ідей і зафіксуй момент на фото 📸"),
            new Phrase("Кілька приємних кроків — і ви знову пліч-о-пліч 💞"),
            new Phrase("Саме час для ініціативи: заплануй побачення, про яке ви давно говорили 🚀"),
            new Phrase("Наздогнати — реально! Одна вдала субота, кілька фото — і баланс вирівняється ✨"));

    static final List<Phrase> BIG = List.of(
            new Phrase("Твоя половинка задає високий темп, але одна крута пригода на вихідних усе переверне!"),
            new Phrase("Час для секретного плану: організуй побачення мрії та збери бонусні бали за атмосферу ❤️"),
            new Phrase("Твоя половинка розігналась — гарний привід показати, на що здатне твоє романтичне серце 😉"),
            new Phrase("Великий камбек починається з одного кроку: ідея, план, фото — і відрив тане 💫"),
            new Phrase("Без поспіху: кілька спільних пригод — і ви знову поруч у рейтингу 🌷"));

    // Тому, хто попереду, — тепла фраза без хизування (бачить її лише він сам)
    static final List<Phrase> LEAD = List.of(
            new Phrase("Ти задаєш темп — так тримати! Може, наступну пригоду придумаєте разом? ✨"),
            new Phrase("Чудовий ритм! Поділись натхненням — заплануйте щось удвох 💞"),
            new Phrase("Ти зараз трохи попереду — найкращий момент зробити приємний сюрприз 🌷"),
            new Phrase("Гарна серія! Головне — щоб обом було тепло й весело ❤️"));

    // Конкретні кроки за активностями; {n} — скільки дій треба, щоб вийти вперед, {what} — "2 ідеї"
    static final List<String> IDEAS_ACTION = List.of(
            "💡 Додай ще {what} — і ти попереду за загальним рахунком.",
            "💡 Мозковий штурм: {what} у список — і баланс на твоєму боці.",
            "💡 Згадай, куди давно хотіли піти разом: {what} — і ти вже лідер.",
            "💡 Запиши {what}, навіть найсміливіші — і рахунок зміниться на твою користь.");

    static final List<String> INITIATIVE_ACTION = List.of(
            "🚀 Заплануй {what} — і ти вже попереду.",
            "🚀 Натисни «Реалізувати» на улюбленій ідеї: {what} — і відрив зникне.",
            "🚀 Сюрприз цього тижня? {what} — і лідерство твоє.",
            "🚀 Візьми ініціативу: {what} з тебе — і ви помінялися місцями.");

    static final List<String> PHOTOS_ACTION = List.of(
            "📸 Близько {what} з наступної зустрічі — і ти попереду.",
            "📸 Зафіксуй момент: приблизно {what} — і баланс зміниться.",
            "📸 Світло, усмішки, ви обоє в кадрі — {what}, і ти лідер.",
            "📸 Не ховай телефон на побаченні: {what} — і рахунок на твоєму боці.");

    private MotivationGenerator() {
    }

    public static List<Motivation> generate(List<PlayerStats> players, long seed) {
        if (players.size() != 2) {
            return List.of();
        }
        PlayerStats a = players.get(0), b = players.get(1);
        long gap = Math.abs(a.totalPoints() - b.totalPoints());

        if (gap == 0) {
            boolean start = a.totalPoints() == 0;
            List<Phrase> pool = start ? START : TIE;
            String level = start ? "START" : "TIE";
            return List.of(
                    new Motivation(a.user().id(), level, pick(pool, gap, seed, a.user().id()), null),
                    new Motivation(b.user().id(), level, pick(pool, gap, seed, b.user().id()), null));
        }

        PlayerStats behind = a.totalPoints() < b.totalPoints() ? a : b;
        PlayerStats ahead = behind == a ? b : a;
        String level = level(gap);
        List<Phrase> pool = switch (level) {
            case "SMALL" -> SMALL;
            case "MEDIUM" -> MEDIUM;
            default -> BIG;
        };
        String headline = pick(pool, gap, seed, behind.user().id());
        String action = action(behind, ahead, gap, seed);
        return List.of(
                new Motivation(behind.user().id(), level, headline, action),
                new Motivation(ahead.user().id(), "LEAD", pick(LEAD, gap, seed, ahead.user().id()), null));
    }

    static String level(long gap) {
        if (gap <= SMALL_MAX) {
            return "SMALL";
        }
        return gap <= MEDIUM_MAX ? "MEDIUM" : "BIG";
    }

    // Крок в активності, де відставання найбільше: скільки дій потрібно, щоб обігнати за сумою
    static String action(PlayerStats behind, PlayerStats ahead, long gap, long seed) {
        record Area(String key, ToLongFunction<PlayerStats> points, int perAction) {
        }
        int photoPerAction = behind.photos() > 0
                ? StatsPoints.photo(behind.avgPhotoScore())
                : StatsPoints.photo(50); // ще немає фото — рахуємо як для середнього (10 балів)
        List<Area> areas = new ArrayList<>(List.of(
                new Area("ideas", PlayerStats::ideaPoints, StatsPoints.IDEA),
                new Area("initiative", PlayerStats::initiativePoints, StatsPoints.INITIATIVE),
                new Area("photos", PlayerStats::photoPoints, photoPerAction)));
        // найбільший відрив; за рівного — та активність, де потрібно менше дій
        Area area = areas.stream()
                .max(Comparator.<Area>comparingLong(x -> x.points().applyAsLong(ahead) - x.points().applyAsLong(behind))
                        .thenComparingInt(Area::perAction))
                .orElseThrow();

        long n = (gap + area.perAction()) / area.perAction(); // ceil((gap + 1) / perAction) — щоб саме обігнати
        String what = switch (area.key()) {
            case "ideas" -> n + " " + plural(n, "ідею", "ідеї", "ідей");
            case "initiative" -> n + " побачення";
            default -> n + " фото";
        };
        List<String> pool = switch (area.key()) {
            case "ideas" -> IDEAS_ACTION;
            case "initiative" -> INITIATIVE_ACTION;
            default -> PHOTOS_ACTION;
        };
        return pool.get(index(pool.size(), seed, behind.user().id(), area.key())).replace("{what}", what);
    }

    // Випадкова, але стабільна для того самого зерна фраза, правдива для цього відриву
    static String pick(List<Phrase> pool, long gap, long seed, Long userId) {
        List<Phrase> fitting = pool.stream().filter(p -> gap <= p.maxGap()).toList();
        return fitting.get(index(fitting.size(), seed, userId, pool.get(0).text())).text();
    }

    private static int index(int size, Object... parts) {
        return Math.floorMod(Objects.hash(parts), size);
    }

    // Українські форми множини: 1 ідея, 2 ідеї, 5 ідей, 21 ідея, 11 ідей
    public static String plural(long n, String one, String few, String many) {
        long mod100 = n % 100, mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return many;
        }
        if (mod10 == 1) {
            return one;
        }
        return mod10 >= 2 && mod10 <= 4 ? few : many;
    }
}
