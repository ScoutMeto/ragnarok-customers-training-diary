package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.IntervalConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 13 (A11): Intervalový typ provedení.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase13IntervalTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("interval@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("iv");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void create_timeBasedInterval() {
        TrainingInput input = base();
        var iv = new IntervalConfigInput();
        iv.setRounds(8);
        iv.setWorkSeconds(30);
        iv.setRestSeconds(60);
        input.getExercises().get(0).setInterval(iv);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getIntervalConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getRounds()).isEqualTo(8);
        assertThat(cfg.getWorkSeconds()).isEqualTo(30);
        assertThat(cfg.getWorkReps()).isNull();
        assertThat(cfg.getRestSeconds()).isEqualTo(60);
    }

    @Test
    void create_repsBasedInterval() {
        TrainingInput input = base();
        var iv = new IntervalConfigInput();
        iv.setRounds(5);
        iv.setWorkReps(12);
        iv.setRestSeconds(90);
        iv.setWeightKg(new java.math.BigDecimal("24.0"));
        input.getExercises().get(0).setInterval(iv);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getIntervalConfig();
        assertThat(cfg.getWorkReps()).isEqualTo(12);
        assertThat(cfg.getWorkSeconds()).isNull();
        assertThat(cfg.getWeightKg()).isEqualByComparingTo("24.0");
    }

    @Test
    void changeTypeAwayFromInterval_clearsConfig() {
        TrainingInput input = base();
        var iv = new IntervalConfigInput();
        iv.setRounds(8);
        iv.setWorkSeconds(30);
        iv.setRestSeconds(60);
        input.getExercises().get(0).setInterval(iv);
        TrainingEntity created = trainingService.create(alice, input);

        // edit → změň typ na FREEFORM
        TrainingInput edit = base();
        edit.getExercises().get(0).setType(TrainingExerciseType.FREEFORM);
        TrainingEntity updated = trainingService.update(alice, created.getId(), edit);
        assertThat(updated.getExercises().get(0).getIntervalConfig()).isNull();
    }

    private TrainingInput base() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Interval test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.INTERVAL);
        ex.setCustomName("KB swing intervaly");
        input.getExercises().add(ex);
        return input;
    }
}
