package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.KbSportConfigInput;
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
 * Phase 13 (A12): KB sport time.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase13KbSportTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("kbsport@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("kbs");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void create_summaryOnly_convertsTimeToSeconds() {
        TrainingInput input = base();
        var kb = new KbSportConfigInput();
        kb.setTotalMinutes(10);
        kb.setTotalSeconds(0);
        kb.setTotalReps(80);
        input.getExercises().get(0).setKbSport(kb);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getKbSportConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getTotalSeconds()).isEqualTo(600);
        assertThat(cfg.getTotalReps()).isEqualTo(80);
        assertThat(cfg.getSplitIntervalSeconds()).isNull();
        assertThat(cfg.getIntervals()).isEmpty();
    }

    @Test
    void create_withSubMinuteTime_2m30s() {
        TrainingInput input = base();
        var kb = new KbSportConfigInput();
        kb.setTotalMinutes(2);
        kb.setTotalSeconds(30);
        input.getExercises().get(0).setKbSport(kb);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getKbSportConfig().getTotalSeconds()).isEqualTo(150);
    }

    @Test
    void create_withIntervals_andSides() {
        TrainingInput input = base();
        var kb = new KbSportConfigInput();
        kb.setTotalMinutes(2);
        kb.setUnilateral(true);
        kb.setSplitIntervalSeconds(60);
        var i1 = new KbSportConfigInput.IntervalInput();
        i1.setIntervalIndex(0); i1.setReps(20); i1.setSide("L");
        var i2 = new KbSportConfigInput.IntervalInput();
        i2.setIntervalIndex(1); i2.setReps(18); i2.setSide("P");
        var iEmpty = new KbSportConfigInput.IntervalInput(); // prázdný se přeskočí
        kb.getIntervals().add(i1);
        kb.getIntervals().add(i2);
        kb.getIntervals().add(iEmpty);
        input.getExercises().get(0).setKbSport(kb);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getKbSportConfig();
        assertThat(cfg.isUnilateral()).isTrue();
        assertThat(cfg.getSplitIntervalSeconds()).isEqualTo(60);
        assertThat(cfg.getIntervals()).hasSize(2);
        assertThat(cfg.getIntervals().get(0).getSide()).isEqualTo("L");
        assertThat(cfg.getIntervals().get(1).getReps()).isEqualTo(18);
    }

    @Test
    void changeTypeAway_clearsKbSport() {
        TrainingInput input = base();
        var kb = new KbSportConfigInput();
        kb.setTotalMinutes(10);
        input.getExercises().get(0).setKbSport(kb);
        TrainingEntity created = trainingService.create(alice, input);

        TrainingInput edit = base();
        edit.getExercises().get(0).setType(TrainingExerciseType.FREEFORM);
        TrainingEntity updated = trainingService.update(alice, created.getId(), edit);
        assertThat(updated.getExercises().get(0).getKbSportConfig()).isNull();
    }

    private TrainingInput base() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("KB sport test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.KB_SPORT_TIME);
        ex.setCustomName("KB Snatch");
        input.getExercises().add(ex);
        return input;
    }
}
