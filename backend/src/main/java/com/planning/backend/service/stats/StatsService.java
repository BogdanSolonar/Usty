package com.planning.backend.service.stats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.planning.backend.dto.stats.Motivation;
import com.planning.backend.dto.stats.PlayerStats;
import com.planning.backend.dto.stats.StatsCategory;
import com.planning.backend.dto.stats.StatsCategory.Score;
import com.planning.backend.dto.stats.StatsResponse;
import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.DateStatus;
import com.planning.backend.entity.User;
import com.planning.backend.repository.DatePhotoRepository;
import com.planning.backend.repository.DateRepository;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.security.CurrentUser;

import static com.planning.backend.service.stats.MotivationGenerator.plural;

// Статистика пари: бали за три активності (формула — StatsPoints), хто попереду за кожною і за сумою,
// та тепла підказка (MotivationGenerator).
//
// Приватність: генератор складає підказки для обох, але у відповідь потрапляє ЛИШЕ підказка
// поточного користувача (з токена). Фраза партнера з сервера не виходить узагалі —
// її неможливо побачити ні в інтерфейсі, ні в мережевих запитах браузера.
@Service
public class StatsService {

    private final UserRepository users;
    private final DateRepository dates;
    private final DatePhotoRepository photos;
    private final CurrentUser currentUser;

    public StatsService(UserRepository users, DateRepository dates, DatePhotoRepository photos,
            CurrentUser currentUser) {
        this.users = users;
        this.dates = dates;
        this.photos = photos;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        User me = currentUser.get();
        List<PlayerStats> players = new ArrayList<>();
        for (User user : users.findByCouple_IdOrderByIdAsc(me.getCoupleId())) {
            long ideas = dates.countByCreatedBy_Id(user.getId());
            long initiated = dates.countByInitiatedBy_IdAndStatusIn(user.getId(),
                    EnumSet.of(DateStatus.PLANNED, DateStatus.COMPLETED));
            List<Integer> scores = photos.scoresByUser(user.getId());
            players.add(player(UserResponse.from(user), ideas, initiated, scores));
        }

        List<StatsCategory> categories = List.of(
                category("ideas", "Ідеї побачень", players, PlayerStats::ideaPoints,
                        p -> p.ideas() + " " + plural(p.ideas(), "ідея", "ідеї", "ідей")),
                category("initiative", "Ініціатива", players, PlayerStats::initiativePoints,
                        p -> p.initiated() + " " + plural(p.initiated(), "побачення", "побачення", "побачень")),
                category("photos", "Спогади на фото", players, PlayerStats::photoPoints,
                        p -> p.photos() == 0 ? "0 фото" : p.photos() + " фото, якість " + p.avgPhotoScore()));

        long gap = players.size() == 2
                ? Math.abs(players.get(0).totalPoints() - players.get(1).totalPoints())
                : 0;
        // зерно для вибору фраз: змінюється щодня і після кожної нової дії
        long seed = LocalDate.now().toEpochDay() * 31
                + players.stream().mapToLong(PlayerStats::totalPoints).sum();

        return new StatsResponse(players, categories, leader(players, PlayerStats::totalPoints), gap,
                own(MotivationGenerator.generate(players, seed), me.getId()));
    }

    // Лише підказка цього користувача (null — якщо для нього нічого немає)
    static Motivation own(List<Motivation> motivations, Long userId) {
        return motivations.stream().filter(m -> m.userId().equals(userId)).findFirst().orElse(null);
    }

    static PlayerStats player(UserResponse user, long ideas, long initiated, List<Integer> photoScores) {
        long photoCount = photoScores.size();
        int avg = photoCount == 0
                ? 0
                : (int) Math.round(photoScores.stream().mapToInt(Integer::intValue).average().orElse(0));
        long ideaPoints = ideas * StatsPoints.IDEA;
        long initiativePoints = initiated * StatsPoints.INITIATIVE;
        long photoPoints = StatsPoints.photos(photoScores);
        return new PlayerStats(user, ideas, initiated, photoCount, avg,
                ideaPoints, initiativePoints, photoPoints, ideaPoints + initiativePoints + photoPoints);
    }

    private static StatsCategory category(String key, String title, List<PlayerStats> players,
            ToLongFunction<PlayerStats> points, Function<PlayerStats, String> label) {
        List<Score> scores = players.stream()
                .map(p -> new Score(p.user().id(), points.applyAsLong(p), label.apply(p)))
                .toList();
        return new StatsCategory(key, title, scores, leader(players, points));
    }

    // id того, в кого значення найбільше; null — якщо порівну (або в обох нуль)
    static Long leader(List<PlayerStats> players, ToLongFunction<PlayerStats> value) {
        PlayerStats best = null;
        boolean tie = false;
        for (PlayerStats p : players) {
            if (best == null || value.applyAsLong(p) > value.applyAsLong(best)) {
                best = p;
                tie = false;
            } else if (value.applyAsLong(p) == value.applyAsLong(best)) {
                tie = true;
            }
        }
        return best == null || tie ? null : best.user().id();
    }
}
