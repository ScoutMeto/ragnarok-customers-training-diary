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
 * Phase 11 (A2): per-exercise tagy zaměření.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase11ExerciseTagsTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private Long coreTagId;
    private Long fullbodyTagId;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("a2@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("a2");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);

        // V testovacím H2 nejsou seed tagy z V20 (Flyway neběží), vytvoříme je
        coreTagId = createSystemTag("Core");
        fullbodyTagId = createSystemTag("Fullbody");
    }

    @Test
    void create_assignsPerExerciseTags() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("S tagy");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("Plank");
        ex.setTagIds(Set.of(coreTagId, fullbodyTagId));
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);

        var savedEx = created.getExercises().get(0);
        assertThat(savedEx.getTags()).hasSize(2);
        assertThat(savedEx.getTags()).extracting(TrainingTagEntity::getName)
                .containsExactlyInAnyOrder("Core", "Fullbody");
    }

    @Test
    void update_replacesPerExerciseTags() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Upravím tagy");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("Cvik");
        ex.setTagIds(Set.of(coreTagId));
        input.getExercises().add(ex);
        TrainingEntity created = trainingService.create(alice, input);

        // edit: změň tagy na fullbody
        TrainingInput edit = new TrainingInput();
        edit.setTrainingDate(LocalDate.now());
        edit.setName("Upravím tagy");
        TrainingExerciseInput ex2 = new TrainingExerciseInput();
        ex2.setType(TrainingExerciseType.FREEFORM);
        ex2.setCustomName("Cvik");
        ex2.setTagIds(Set.of(fullbodyTagId));
        edit.getExercises().add(ex2);

        TrainingEntity updated = trainingService.update(alice, created.getId(), edit);
        var savedEx = updated.getExercises().get(0);
        assertThat(savedEx.getTags()).extracting(TrainingTagEntity::getName).containsExactly("Fullbody");
    }

    @Test
    void create_withoutTags_emptyTagSet() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Bez tagů");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("Cvik");
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getTags()).isEmpty();
    }

    private Long createSystemTag(String name) {
        TrainingTagEntity tag = new TrainingTagEntity();
        tag.setName(name);
        tag.setColor("#888888");
        tag.setSystem(true);
        return tagRepository.save(tag).getId();
    }
}
