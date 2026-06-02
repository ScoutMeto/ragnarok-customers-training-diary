package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.analysis.AnalysisService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smoke testy AnalysisService — ověříme, že JPQL queries fungují
 * a vrací rozumná data (queries nevyhazují exceptions, aggregation počítá
 * správně pro known seed dat).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalysisSmokeTest {

    @Autowired private AnalysisService analysisService;
    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private LocalDate today;

    @BeforeEach
    void setup() {
        alice = createUser("analysis-alice@example.com", "Alice", AccountRole.USER);
        today = LocalDate.now();

        // Vytvořit tréninky s různými RPE, difficulty, váhami
        createTraining(today.minusDays(2), (short) 7, 24, 10, 24, 8);
        createTraining(today.minusDays(1), (short) 8, 28, 8, 28, 8);
        createTraining(today,              (short) 6, 20, 12, 20, 10);
    }

    @Test
    void totalVolumeByDay_aggregatesPerDate() {
        var points = analysisService.totalVolumeByDay(alice, today.minusDays(7), today);
        assertThat(points).hasSize(3);
        assertThat(points.get(2).date()).isEqualTo(today);
        // Třetí den: 20×12 + 20×10 = 440
        assertThat(points.get(2).value().intValueExact()).isEqualTo(440);
    }

    @Test
    void rpeTrend_returnsAvgPerDay() {
        var points = analysisService.rpeTrend(alice, today.minusDays(7), today);
        assertThat(points).hasSize(3);
    }

    @Test
    void frequencyHeatmap_returnsCountsPerDay() {
        var map = analysisService.frequencyHeatmap(alice, today.minusDays(7), today);
        assertThat(map).hasSize(3);
        assertThat(map.get(today)).isEqualTo(1);
    }

    @Test
    void setsPerBodyRegion_groupsCorrectly() {
        // Naše testovací data nemají catalogItem (jsou custom), tedy result je empty
        var result = analysisService.setsPerBodyRegion(alice, today.minusDays(7), today);
        assertThat(result).isNotNull();
    }

    @Test
    void adminOverview_returnsKpis() {
        var overview = analysisService.adminOverview(today.minusDays(7), today);
        // Phase 9 (D1-D3): jen activeClients + totalRecords + topActiveClients
        assertThat(overview.totalRecords()).isGreaterThanOrEqualTo(3);
        assertThat(overview.activeClients()).isGreaterThanOrEqualTo(1);
        assertThat(overview.topActiveClients()).isNotNull();
    }

    @Test
    void listLoggedExerciseNames_returnsDistinctNames() {
        var names = analysisService.listLoggedExerciseNames(alice);
        assertThat(names).containsExactly("KB swing");
    }

    @Test
    void exerciseStats_aggregatesAllMetricsForExercise() {
        // 3 tréninky × 2 sety "KB swing":
        //  -2d: 24×10 + 24×8 = 432, reps 18
        //  -1d: 28×8  + 28×8 = 448, reps 16
        //   0d: 20×12 + 20×10= 440, reps 22
        var stats = analysisService.exerciseStats(alice, "KB swing", today.minusDays(7), today);
        assertThat(stats.totalVolumeKg().intValueExact()).isEqualTo(1320);
        assertThat(stats.totalSets()).isEqualTo(6);
        assertThat(stats.totalReps()).isEqualTo(56);
        assertThat(stats.maxReps()).isEqualTo(12);
        assertThat(stats.maxWeightKg().intValueExact()).isEqualTo(28);
    }

    @Test
    void exerciseStats_unknownExercise_returnsZero() {
        var stats = analysisService.exerciseStats(alice, "Neexistuje", today.minusDays(7), today);
        assertThat(stats.totalSets()).isZero();
        assertThat(stats.totalReps()).isZero();
    }

    @Test
    void statsByExerciseTags_aggregatesByTag_noDoubleCounting() {
        // Vytvoř tag "Core" a přiřaď ho cviku v novém tréninku (2 sety: 10×10, 10×10)
        var coreTag = new com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity();
        coreTag.setName("Core");
        coreTag.setColor("#888");
        coreTag.setSystem(true);
        coreTag = tagRepository.save(coreTag);

        var input = new TrainingInput();
        input.setTrainingDate(today);
        input.setName("Tag test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("Plank rotace");
        ex.setTagIds(java.util.Set.of(coreTag.getId()));
        var s = new SetInput();
        s.setWeightKg(new java.math.BigDecimal(10));
        s.setReps(10);
        ex.getSets().add(s);
        input.getExercises().add(ex);
        trainingService.create(alice, input);

        var stats = analysisService.statsByExerciseTags(
                alice, java.util.List.of(coreTag.getId()), today.minusDays(7), today);
        // 1 set: 10×10 = 100, 1 série, 10 reps
        assertThat(stats.totalVolumeKg().intValueExact()).isEqualTo(100);
        assertThat(stats.totalSets()).isEqualTo(1);
        assertThat(stats.totalReps()).isEqualTo(10);
    }

    @Test
    void statsByExerciseTags_emptyTagList_returnsZero() {
        var stats = analysisService.statsByExerciseTags(alice, java.util.List.of(), today.minusDays(7), today);
        assertThat(stats.totalSets()).isZero();
    }

    @Autowired
    private com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository tagRepository;

    // ---------- helpers ----------

    private AccountEntity createUser(String email, String firstName, AccountRole role) {
        AccountEntity a = new AccountEntity();
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode("password"));
        a.setRole(role);
        a.setNickname(firstName.toLowerCase());
        a.setFirstName(firstName);
        a.setLastName("Test");
        return accountRepository.save(a);
    }

    private TrainingEntity createTraining(LocalDate date, Short rpe,
                                           int w1, int r1, int w2, int r2) {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(date);
        input.setName("Test " + date);
        input.setRpe(rpe);

        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("KB swing");
        SetInput s1 = new SetInput();
        s1.setWeightKg(new BigDecimal(w1));
        s1.setReps(r1);
        SetInput s2 = new SetInput();
        s2.setWeightKg(new BigDecimal(w2));
        s2.setReps(r2);
        ex.getSets().add(s1);
        ex.getSets().add(s2);
        input.getExercises().add(ex);

        return trainingService.create(alice, input);
    }
}
