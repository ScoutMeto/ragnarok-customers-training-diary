package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CompositeSetConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
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
 * Phase 12 (A6/A7/A8): tabulka sérií jen pro FREEFORM + composite bez limitu kroků.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase12FeaturesTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("phase12@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("p12");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void sets_persistedForFreeform() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Freeform test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("Dřep");
        ex.getSets().add(set(60, 5));
        ex.getSets().add(set(70, 3));
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getSets()).hasSize(2);
    }

    @Test
    void sets_ignoredForTypedExercise() {
        // I když UI sety pro typovaný cvik skryje, případné poslané sety backend zahodí.
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Superset test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.SUPERSET);
        ex.setCustomName("Superset");
        ex.getSets().add(set(20, 10)); // měl by být ignorován

        var comp = new CompositeSetConfigInput();
        comp.setRounds(4);
        // bez limitu — přidáme 8 kroků (dřív byl strop 6)
        for (int i = 1; i <= 8; i++) {
            var s = new CompositeSetConfigInput.StepInput();
            s.setName("Cvik " + i);
            s.setReps(i);
            comp.getSteps().add(s);
        }
        ex.setComposite(comp);
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        var saved = created.getExercises().get(0);
        assertThat(saved.getSets()).isEmpty();
        assertThat(saved.getCompositeSetConfig()).isNotNull();
        assertThat(saved.getCompositeSetConfig().getSteps()).hasSize(8);
    }

    private SetInput set(int weight, int reps) {
        var s = new SetInput();
        s.setWeightKg(new java.math.BigDecimal(weight));
        s.setReps(reps);
        return s;
    }
}
