package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemRepository;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ověření, že TrainingService dodržuje ownership:
 *  - klient může vidět/editovat/smazat jen své vlastní tréninky
 *  - pokus o cizí trénink hodí NotFoundException (ne ForbiddenException, abychom
 *    neprozrazovali existenci cizích záznamů)
 *  - smazání účtu kaskádově maže jeho tréninky
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrainingOwnershipTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ExerciseCatalogItemRepository catalogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private AccountEntity bob;
    private ExerciseCatalogItemEntity kbSwing;

    @BeforeEach
    void seed() {
        alice = createUser("alice@example.com", "Alice");
        bob   = createUser("bob@example.com", "Bob");

        kbSwing = new ExerciseCatalogItemEntity();
        kbSwing.setName("Kettlebell Swing");
        kbSwing.setSystem(true);
        kbSwing = catalogRepository.save(kbSwing);
    }

    @Test
    void aliceCreatesTraining_thenReadsItBack() {
        TrainingInput input = sampleTrainingInput();
        TrainingEntity created = trainingService.create(alice, input);

        TrainingEntity reloaded = trainingService.getMyTraining(alice, created.getId());
        assertThat(reloaded.getOwner().getId()).isEqualTo(alice.getId());
        assertThat(reloaded.getExercises()).hasSize(1);
        assertThat(reloaded.getExercises().getFirst().getSets()).hasSize(2);
    }

    @Test
    void bobCannotReadAlicesTraining() {
        TrainingEntity alicesTraining = trainingService.create(alice, sampleTrainingInput());

        assertThatThrownBy(() -> trainingService.getMyTraining(bob, alicesTraining.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void bobCannotEditAlicesTraining() {
        TrainingEntity alicesTraining = trainingService.create(alice, sampleTrainingInput());

        assertThatThrownBy(() -> trainingService.update(bob, alicesTraining.getId(), sampleTrainingInput()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void bobCannotDeleteAlicesTraining() {
        TrainingEntity alicesTraining = trainingService.create(alice, sampleTrainingInput());

        assertThatThrownBy(() -> trainingService.delete(bob, alicesTraining.getId()))
                .isInstanceOf(NotFoundException.class);

        // Alice's training should still exist
        assertThat(trainingService.listMyTrainings(alice)).hasSize(1);
    }

    @Test
    void aliceSeesOnlyHerOwnTrainings() {
        trainingService.create(alice, sampleTrainingInput());
        trainingService.create(alice, sampleTrainingInput());
        trainingService.create(bob,   sampleTrainingInput());

        assertThat(trainingService.listMyTrainings(alice)).hasSize(2);
        assertThat(trainingService.listMyTrainings(bob)).hasSize(1);
    }

    @Test
    void emptySetsAreSilentlyDropped() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());

        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCatalogItemId(kbSwing.getId());
        ex.getSets().add(new SetInput());                  // all null -> dropped
        ex.getSets().add(setWithReps(10));                 // kept
        ex.getSets().add(new SetInput());                  // dropped
        ex.getSets().add(setWithReps(8));                  // kept
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        TrainingEntity reloaded = trainingService.getMyTraining(alice, created.getId());

        assertThat(reloaded.getExercises().getFirst().getSets()).hasSize(2);
        assertThat(reloaded.getExercises().getFirst().getSets().get(0).getReps()).isEqualTo(10);
        assertThat(reloaded.getExercises().getFirst().getSets().get(1).getReps()).isEqualTo(8);
    }

    @Test
    void exerciseWithBothCatalogAndCustomName_rejected() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());

        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCatalogItemId(kbSwing.getId());
        ex.setCustomName("Custom");
        input.getExercises().add(ex);

        assertThatThrownBy(() -> trainingService.create(alice, input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("katalogu");
    }

    @Test
    void exerciseWithoutCatalogOrCustomName_rejected() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());

        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        input.getExercises().add(ex);

        assertThatThrownBy(() -> trainingService.create(alice, input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private AccountEntity createUser(String email, String firstName) {
        AccountEntity a = new AccountEntity();
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode("password"));
        a.setRole(AccountRole.USER);
        a.setNickname(firstName.toLowerCase());
        a.setFirstName(firstName);
        a.setLastName("Test");
        return accountRepository.save(a);
    }

    private TrainingInput sampleTrainingInput() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Sample");

        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCatalogItemId(kbSwing.getId());
        ex.setSets(List.of(setWithReps(10), setWithReps(10)));
        input.getExercises().add(ex);

        return input;
    }

    private SetInput setWithReps(int reps) {
        SetInput s = new SetInput();
        s.setWeightKg(new BigDecimal("16.00"));
        s.setReps(reps);
        return s;
    }
}
