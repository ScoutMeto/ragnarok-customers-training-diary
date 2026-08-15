package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingCommentService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.EmomConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for GROUP visibility trainings (Phase 2 corrected scope):
 *  - admin creates group training
 *  - all clients see today's group trainings via listGroupTrainingsForDay
 *  - listMyTrainings ignores group trainings (owner=NULL)
 *  - PRIVATE/GROUP filter is consistent
 *  - clients can comment on group trainings (PRIVATE rules unchanged)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupTrainingTest {

    @Autowired private TrainingService trainingService;
    @Autowired private TrainingCommentService commentService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ExerciseTypeConfigToInputMapper toInputMapper;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private MockMvc mockMvc;

    private AccountEntity alice;
    private AccountEntity bob;
    private AccountEntity trainer;

    @BeforeEach
    void seed() {
        alice   = createUser("alice@example.com", "Alice", AccountRole.USER);
        bob     = createUser("bob@example.com",   "Bob",   AccountRole.USER);
        trainer = createUser("coach@example.com", "Coach", AccountRole.ADMIN);
    }

    @Test
    void adminCreatesGroupTraining_setsVisibilityAndCreator() {
        TrainingEntity group = trainingService.createGroup(trainer, sampleInputForDate(LocalDate.now()));

        assertThat(group.getVisibility()).isEqualTo(TrainingVisibility.GROUP);
        assertThat(group.getOwner()).isNull();
        assertThat(group.getCreatedBy().getId()).isEqualTo(trainer.getId());
    }

    @Test
    void groupTrainingsForDay_returnsOnlyMatchingDate() {
        LocalDate today = LocalDate.now();
        trainingService.createGroup(trainer, sampleInputForDate(today.minusDays(1)));
        trainingService.createGroup(trainer, sampleInputForDate(today));
        trainingService.createGroup(trainer, sampleInputForDate(today));
        trainingService.createGroup(trainer, sampleInputForDate(today.plusDays(1)));

        List<TrainingEntity> result = trainingService.listGroupTrainingsForDay(today);
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getTrainingDate().equals(today));
        assertThat(result).allMatch(t -> t.getVisibility() == TrainingVisibility.GROUP);
    }

    @Test
    void listMyTrainings_excludesGroup() {
        // Alice has 1 private + admin created 1 group on same day
        trainingService.create(alice, sampleInputForDate(LocalDate.now()));
        trainingService.createGroup(trainer, sampleInputForDate(LocalDate.now()));

        List<TrainingEntity> alicePrivate = trainingService.listMyTrainings(alice);
        assertThat(alicePrivate).hasSize(1);
        assertThat(alicePrivate).allMatch(t -> t.getVisibility() == TrainingVisibility.PRIVATE);
    }

    @Test
    void listTrainingsOf_admin_returnsOnlyPrivate() {
        trainingService.create(alice, sampleInputForDate(LocalDate.now()));
        trainingService.create(alice, sampleInputForDate(LocalDate.now().minusDays(1)));
        trainingService.createGroup(trainer, sampleInputForDate(LocalDate.now()));

        // Admin pohled na Alice — neměl by vidět group jako její
        assertThat(trainingService.listTrainingsOf(alice.getId())).hasSize(2);
    }

    @Test
    void clientCanCommentGroupTraining() {
        TrainingEntity group = trainingService.createGroup(trainer, sampleInputForDate(LocalDate.now()));

        var comment = commentService.addComment(alice, group.getId(), "Skvělá lekce!");
        assertThat(comment.getAuthor().getId()).isEqualTo(alice.getId());
        assertThat(comment.getTraining().getId()).isEqualTo(group.getId());

        // Bob (taky klient) taky smí
        commentService.addComment(bob, group.getId(), "Souhlasím");
        assertThat(commentService.listForTraining(group.getId())).hasSize(2);
    }

    @Test
    void updateGroup_changesFields() {
        TrainingEntity group = trainingService.createGroup(trainer, sampleInputForDate(LocalDate.now()));

        TrainingInput updated = sampleInputForDate(LocalDate.now());
        updated.setName("Upravený název");
        trainingService.updateGroup(group.getId(), updated);

        TrainingEntity reloaded = trainingService.getAnyTraining(group.getId());
        assertThat(reloaded.getName()).isEqualTo("Upravený název");
        assertThat(reloaded.getVisibility()).isEqualTo(TrainingVisibility.GROUP);
    }

    @Test
    void updateGroup_onPrivateTraining_rejected() {
        TrainingEntity privateTraining = trainingService.create(alice, sampleInputForDate(LocalDate.now()));

        assertThatThrownBy(() -> trainingService.updateGroup(privateTraining.getId(), sampleInputForDate(LocalDate.now())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("není skupinový");
    }

    @Test
    void deleteGroup_onPrivateTraining_rejected() {
        TrainingEntity privateTraining = trainingService.create(alice, sampleInputForDate(LocalDate.now()));

        assertThatThrownBy(() -> trainingService.deleteGroup(privateTraining.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("není skupinový");
    }

    @Test
    void copyGroupToPrivate_copiesConfigEquipmentAndTags() {
        // sestav skupinový trénink s EMOM cvikem + náčiním + systémovým tagem
        TrainingTagEntity systemTag = new TrainingTagEntity();
        systemTag.setName("síla");
        systemTag.setSystem(true);
        systemTag = tagRepository.save(systemTag);

        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Skupina A");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.EMOM);
        ex.setCustomName("KB clean");
        ex.setEquipmentName("kettlebell");
        ex.setEquipmentWeightKg(new BigDecimal("24.0"));
        ex.setEquipmentCount(2);
        ex.setEquipmentSecondWeightKg(new BigDecimal("16.0"));
        ex.getTagIds().add(systemTag.getId());
        EmomConfigInput emom = new EmomConfigInput();
        emom.setTotalMinutes(10);
        emom.setIntervalSeconds(60);
        emom.setDefaultReps(5);
        ex.setEmom(emom);
        input.getExercises().add(ex);

        TrainingEntity group = trainingService.createGroup(trainer, input);

        // alice si ho zkopíruje do osobního deníku
        TrainingEntity copy = trainingService.copyGroupToPrivate(alice, group.getId(), toInputMapper);

        assertThat(copy.getVisibility()).isEqualTo(TrainingVisibility.PRIVATE);
        assertThat(copy.getOwner().getId()).isEqualTo(alice.getId());
        assertThat(copy.getName()).contains("(kopie)");

        var copiedEx = copy.getExercises().get(0);
        // per-type config přenesen
        assertThat(copiedEx.getEmomConfig()).isNotNull();
        assertThat(copiedEx.getEmomConfig().getTotalMinutes()).isEqualTo(10);
        // náčiní přeneseno (A14) — to byl chybějící kus před Phase 16
        assertThat(copiedEx.getEquipmentName()).isEqualTo("kettlebell");
        assertThat(copiedEx.getEquipmentWeightKg()).isEqualByComparingTo("24.0");
        assertThat(copiedEx.getEquipmentCount()).isEqualTo(2);
        assertThat(copiedEx.getEquipmentSecondWeightKg()).isEqualByComparingTo("16.0");
        // per-exercise tagy přeneseny (A2)
        assertThat(copiedEx.getTags()).extracting(TrainingTagEntity::getId).contains(systemTag.getId());
    }

    @Test
    void detailPageRendersGroupTrainingWithEmomTimerConfig() throws Exception {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("EMOM detail");

        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.EMOM);
        ex.setCustomName("KB clean");
        EmomConfigInput emom = new EmomConfigInput();
        emom.setTotalMinutes(10);
        emom.setIntervalSeconds(60);
        emom.setDefaultReps(5);
        ex.setEmom(emom);
        input.getExercises().add(ex);

        TrainingEntity group = trainingService.createGroup(trainer, input);

        mockMvc.perform(get("/diary/{id}", group.getId()).with(user(alice)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("window.__timerConfigs")))
                .andExpect(content().string(containsString("\"emom\"")));
    }
    // -----------------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------------

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

    private TrainingInput sampleInputForDate(LocalDate date) {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(date);
        input.setName("Sample");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("KB swing");
        input.getExercises().add(ex);
        return input;
    }
}
