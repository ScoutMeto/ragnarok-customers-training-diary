package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
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
 * Smoke testy pro Phase 14: vlaječka tréninku (B8) + korunka cviku (A16).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase14FeaturesTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private AccountEntity bob;

    @BeforeEach
    void seed() {
        alice = createUser("alice14@example.cz", "Alice");
        bob = createUser("bob14@example.cz", "Bob");
    }

    @Test
    void toggleFlag_flipsState() {
        TrainingEntity t = trainingService.create(alice, sampleInput());
        assertThat(t.isFlagged()).isFalse();

        boolean after1 = trainingService.toggleFlag(alice, t.getId());
        assertThat(after1).isTrue();
        boolean after2 = trainingService.toggleFlag(alice, t.getId());
        assertThat(after2).isFalse();
    }

    @Test
    void toggleStar_flipsState_andListsStarred() {
        TrainingEntity t = trainingService.create(alice, sampleInput());
        Long exId = t.getExercises().get(0).getId();

        assertThat(trainingService.listStarredExercises(alice)).isEmpty();

        boolean starred = trainingService.toggleStar(alice, exId);
        assertThat(starred).isTrue();
        assertThat(trainingService.listStarredExercises(alice)).hasSize(1);

        trainingService.toggleStar(alice, exId);
        assertThat(trainingService.listStarredExercises(alice)).isEmpty();
    }

    @Test
    void toggleStar_onOtherUsersExercise_forbidden() {
        TrainingEntity aliceTraining = trainingService.create(alice, sampleInput());
        Long exId = aliceTraining.getExercises().get(0).getId();

        assertThatThrownBy(() -> trainingService.toggleStar(bob, exId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void starredExercises_onlyOwnPrivate() {
        TrainingEntity aliceT = trainingService.create(alice, sampleInput());
        trainingService.toggleStar(alice, aliceT.getExercises().get(0).getId());

        // Bob nemá žádné označené
        assertThat(trainingService.listStarredExercises(bob)).isEmpty();
        assertThat(trainingService.listStarredExercises(alice)).hasSize(1);
    }

    // helpers
    private AccountEntity createUser(String email, String firstName) {
        AccountEntity a = new AccountEntity();
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode("password"));
        a.setRole(AccountRole.USER);
        a.setNickname(firstName.toLowerCase());
        a.setFirstName(firstName);
        a.setLastName("Test");
        a.setEmailConfirmed(true);
        return accountRepository.save(a);
    }

    private TrainingInput sampleInput() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Sample");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("KB swing");
        input.getExercises().add(ex);
        return input;
    }
}
