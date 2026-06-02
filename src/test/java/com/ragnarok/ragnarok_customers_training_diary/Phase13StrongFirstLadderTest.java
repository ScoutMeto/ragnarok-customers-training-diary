package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.StrongFirstLadderConfigInput;
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
 * Phase 13 (A10): StrongFirst žebřík.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase13StrongFirstLadderTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("sfl@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("sfl");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void create_storesLadderConfig() {
        TrainingInput input = base();
        var sfl = new StrongFirstLadderConfigInput();
        sfl.setLadderHeight(5);
        sfl.setCycles(3);
        sfl.setRestSeconds(120);
        sfl.setWeightKg(new java.math.BigDecimal("24.0"));
        sfl.setUnilateral(true);
        input.getExercises().get(0).setStrongFirstLadder(sfl);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getStrongFirstLadderConfig();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getLadderHeight()).isEqualTo(5);
        assertThat(cfg.getCycles()).isEqualTo(3);
        assertThat(cfg.getRestSeconds()).isEqualTo(120);
        assertThat(cfg.getWeightKg()).isEqualByComparingTo("24.0");
        assertThat(cfg.isUnilateral()).isTrue();
    }

    @Test
    void create_defaultsCyclesToOne() {
        TrainingInput input = base();
        var sfl = new StrongFirstLadderConfigInput();
        sfl.setLadderHeight(4);
        // cycles null → default 1
        input.getExercises().get(0).setStrongFirstLadder(sfl);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getStrongFirstLadderConfig().getCycles()).isEqualTo(1);
    }

    @Test
    void editRoundtrip_preservesLadder() {
        TrainingInput input = base();
        var sfl = new StrongFirstLadderConfigInput();
        sfl.setLadderHeight(5);
        sfl.setCycles(2);
        input.getExercises().get(0).setStrongFirstLadder(sfl);
        TrainingEntity created = trainingService.create(alice, input);

        // re-save beze změny configu (simuluje edit) — config zůstane
        TrainingInput edit = base();
        var sfl2 = new StrongFirstLadderConfigInput();
        sfl2.setLadderHeight(6);
        sfl2.setCycles(2);
        edit.getExercises().get(0).setStrongFirstLadder(sfl2);
        TrainingEntity updated = trainingService.update(alice, created.getId(), edit);
        assertThat(updated.getExercises().get(0).getStrongFirstLadderConfig().getLadderHeight()).isEqualTo(6);
    }

    private TrainingInput base() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("SF ladder test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.STRONGFIRST_LADDER);
        ex.setCustomName("Clean");
        input.getExercises().add(ex);
        return input;
    }
}
