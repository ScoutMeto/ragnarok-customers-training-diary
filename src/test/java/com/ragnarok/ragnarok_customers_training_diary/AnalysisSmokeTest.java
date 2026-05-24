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
        assertThat(overview.totalPrivateTrainings()).isGreaterThanOrEqualTo(3);
        assertThat(overview.activeClients()).isGreaterThanOrEqualTo(1);
        assertThat(overview.gymTotalVolumeKg()).isNotNull();
    }

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
