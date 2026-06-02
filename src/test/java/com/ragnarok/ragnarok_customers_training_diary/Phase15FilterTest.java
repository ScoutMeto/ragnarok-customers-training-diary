package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 14/15 (B8): filtr tréninků v deníku podle tagu / cviku / vlaječky.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase15FilterTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private Long coreTagId;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("filter@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("filt");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);

        TrainingTagEntity core = new TrainingTagEntity();
        core.setName("Core");
        core.setColor("#888");
        core.setSystem(true);
        coreTagId = tagRepository.save(core).getId();

        // Trénink 1: cvik "Plank" s tagem Core, flagged
        TrainingEntity t1 = trainingService.create(alice, training("Plank trénink", "Plank", Set.of(coreTagId)));
        trainingService.toggleFlag(alice, t1.getId());
        // Trénink 2: cvik "Swing", bez tagu, neflagged
        trainingService.create(alice, training("Swing trénink", "Swing", Set.of()));
    }

    @Test
    void filterByTag_returnsMatchingTrainings() {
        var result = trainingService.listMyTrainingsFiltered(alice, coreTagId, null, false);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Plank trénink");
    }

    @Test
    void filterByExerciseName_returnsMatching() {
        var result = trainingService.listMyTrainingsFiltered(alice, null, "Swing", false);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Swing trénink");
    }

    @Test
    void filterFlaggedOnly_returnsFlagged() {
        var result = trainingService.listMyTrainingsFiltered(alice, null, null, true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Plank trénink");
    }

    @Test
    void filterCombined_tagAndFlagged() {
        var result = trainingService.listMyTrainingsFiltered(alice, coreTagId, null, true);
        assertThat(result).hasSize(1);
    }

    @Test
    void filterNoMatch_returnsEmpty() {
        var result = trainingService.listMyTrainingsFiltered(alice, null, "Neexistuje", false);
        assertThat(result).isEmpty();
    }

    @Test
    void listExerciseNames_returnsDistinct() {
        assertThat(trainingService.listMyExerciseNames(alice))
                .containsExactlyInAnyOrder("Plank", "Swing");
    }

    private TrainingInput training(String name, String exerciseName, Set<Long> tagIds) {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName(name);
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName(exerciseName);
        ex.setTagIds(tagIds);
        input.getExercises().add(ex);
        return input;
    }
}
