package com.planning.backend.service.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.planning.backend.dto.stats.Motivation;
import com.planning.backend.dto.stats.PlayerStats;
import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.PartnerRole;

class StatsServiceTest {

    private static final UserResponse BODYA = new UserResponse(1L, "Бодя", "Боді", PartnerRole.HE, "#3b82f6", null, true);
    private static final UserResponse MARICHKA = new UserResponse(2L, "Марічка", "Марічки", PartnerRole.SHE, "#ec4899", null, false);

    private static PlayerStats player(UserResponse user, long ideas, long initiated, Integer... photoScores) {
        return StatsService.player(user, ideas, initiated, List.of(photoScores));
    }

    // ---------- формула балів ----------

    @Test
    void photoPointsAreBetween5And15() {
        assertThat(StatsPoints.photo(1)).isEqualTo(5);
        assertThat(StatsPoints.photo(50)).isEqualTo(10);
        assertThat(StatsPoints.photo(74)).isEqualTo(12);
        assertThat(StatsPoints.photo(100)).isEqualTo(15);
    }

    @Test
    void totalCombinesAllActivities() {
        // 3 ідеї (30) + 2 побачення (40) + фото 80 і 40 (13 + 9) = 92
        PlayerStats p = player(BODYA, 3, 2, 80, 40);
        assertThat(p.ideaPoints()).isEqualTo(30);
        assertThat(p.initiativePoints()).isEqualTo(40);
        assertThat(p.photoPoints()).isEqualTo(22);
        assertThat(p.totalPoints()).isEqualTo(92);
        assertThat(p.avgPhotoScore()).isEqualTo(60);
    }

    @Test
    void photosNoLongerOutweighDates() {
        // раніше 10 фото по 80 давали 800 "балів" проти кількох ідей; тепер — співмірно
        Integer[] tenGoodPhotos = Collections.nCopies(10, 80).toArray(Integer[]::new);
        PlayerStats photographer = player(BODYA, 0, 0, tenGoodPhotos);
        PlayerStats planner = player(MARICHKA, 5, 4);
        assertThat(photographer.totalPoints()).isEqualTo(130);
        assertThat(planner.totalPoints()).isEqualTo(130);
    }

    @Test
    void leaderByTotalOrNullOnTie() {
        assertThat(StatsService.leader(List.of(player(BODYA, 1, 0), player(MARICHKA, 2, 0)),
                PlayerStats::totalPoints)).isEqualTo(2L);
        assertThat(StatsService.leader(List.of(player(BODYA, 2, 0), player(MARICHKA, 0, 1)),
                PlayerStats::totalPoints)).isNull();
        assertThat(StatsService.leader(List.of(player(BODYA, 0, 0), player(MARICHKA, 0, 0)),
                PlayerStats::totalPoints)).isNull();
    }

    // ---------- мотивуючі підказки ----------

    @Test
    void tieGivesBalancePhraseToBoth() {
        List<Motivation> m = MotivationGenerator.generate(List.of(player(BODYA, 2, 0), player(MARICHKA, 0, 1)), 7);
        assertThat(m).hasSize(2).allMatch(x -> x.level().equals("TIE") && x.action() == null);
        assertThat(m).allMatch(x -> MotivationGenerator.TIE.stream().anyMatch(p -> p.text().equals(x.headline())));
    }

    @Test
    void emptyStatsGiveStartPhrase() {
        List<Motivation> m = MotivationGenerator.generate(List.of(player(BODYA, 0, 0), player(MARICHKA, 0, 0)), 7);
        assertThat(m).hasSize(2).allMatch(x -> x.level().equals("START"));
    }

    @Test
    void behindGetsCatchUpHintLeaderGetsOwnWarmPhrase() {
        List<Motivation> m = MotivationGenerator.generate(List.of(player(BODYA, 3, 1), player(MARICHKA, 1, 0)), 7);
        Motivation marichka = StatsService.own(m, 2L);
        Motivation bodya = StatsService.own(m, 1L);
        assertThat(marichka.level()).isEqualTo("MEDIUM"); // 30 + 20 = 50 проти 10 -> відрив 40
        assertThat(marichka.action()).isNotNull();
        assertThat(bodya.level()).isEqualTo("LEAD");
        assertThat(bodya.action()).isNull();
    }

    // Приватність: кожен отримує лише СВОЮ підказку, чужа у відповідь не потрапляє
    @Test
    void ownMotivationOnly() {
        List<Motivation> m = MotivationGenerator.generate(List.of(player(BODYA, 3, 1), player(MARICHKA, 1, 0)), 7);
        assertThat(StatsService.own(m, 1L).userId()).isEqualTo(1L);
        assertThat(StatsService.own(m, 2L).userId()).isEqualTo(2L);
        assertThat(StatsService.own(m, 99L)).isNull();
        assertThat(StatsService.own(m, 1L).headline()).isNotEqualTo(StatsService.own(m, 2L).headline());
    }

    @Test
    void levelsByGap() {
        assertThat(MotivationGenerator.level(1)).isEqualTo("SMALL");
        assertThat(MotivationGenerator.level(15)).isEqualTo("SMALL");
        assertThat(MotivationGenerator.level(16)).isEqualTo("MEDIUM");
        assertThat(MotivationGenerator.level(40)).isEqualTo("MEDIUM");
        assertThat(MotivationGenerator.level(41)).isEqualTo("BIG");
    }

    @Test
    void smallGapPhrasesAreAlwaysTrue() {
        // відрив 12: "1 нова ідея (+10) — і ти попереду" було б неправдою, тож ця фраза не випадає
        for (long seed = 0; seed < 200; seed++) {
            String h = MotivationGenerator.pick(MotivationGenerator.SMALL, 12, seed, 2L);
            assertThat(h).doesNotContain("1 нову ідею");
        }
        // а при відриві 5 вона можлива
        assertThat(LongStream.range(0, 200)
                .mapToObj(seed -> MotivationGenerator.pick(MotivationGenerator.SMALL, 5, seed, 2L))
                .anyMatch(h -> h.contains("1 нову ідею"))).isTrue();
    }

    @Test
    void actionUsesRealNumbersOfBiggestGap() {
        // Марічка позаду лише в ідеях: 5 ідей (50) проти 1 (10), відрив 40 -> треба 5 ідей, щоб обігнати
        PlayerStats bodya = player(BODYA, 5, 0);
        PlayerStats marichka = player(MARICHKA, 1, 0);
        Motivation m = MotivationGenerator.generate(List.of(bodya, marichka), 3).get(0);
        assertThat(m.userId()).isEqualTo(2L);
        assertThat(m.action()).startsWith("💡").contains("5 ідей");

        // позаду в побаченнях: 0 проти 2 (40) -> 3 побачення (60) обганяють
        Motivation m2 = MotivationGenerator.generate(List.of(player(BODYA, 0, 0), player(MARICHKA, 0, 2)), 3).get(0);
        assertThat(m2.userId()).isEqualTo(1L);
        assertThat(m2.action()).startsWith("🚀").contains("3 побачення");
    }

    @Test
    void phrasePoolsAreRichEnough() {
        assertThat(MotivationGenerator.TIE).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.SMALL).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.MEDIUM).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.BIG).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.LEAD).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.IDEAS_ACTION).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.INITIATIVE_ACTION).hasSizeGreaterThanOrEqualTo(4);
        assertThat(MotivationGenerator.PHOTOS_ACTION).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void ukrainianPlurals() {
        assertThat(MotivationGenerator.plural(1, "ідея", "ідеї", "ідей")).isEqualTo("ідея");
        assertThat(MotivationGenerator.plural(3, "ідея", "ідеї", "ідей")).isEqualTo("ідеї");
        assertThat(MotivationGenerator.plural(5, "ідея", "ідеї", "ідей")).isEqualTo("ідей");
        assertThat(MotivationGenerator.plural(11, "ідея", "ідеї", "ідей")).isEqualTo("ідей");
        assertThat(MotivationGenerator.plural(21, "ідея", "ідеї", "ідей")).isEqualTo("ідея");
    }
}
