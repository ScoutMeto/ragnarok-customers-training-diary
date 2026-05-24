package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.coach.CoachPlanEntity;
import com.ragnarok.ragnarok_customers_training_diary.coach.CoachPlanService;
import com.ragnarok.ragnarok_customers_training_diary.coach.MarkdownRenderer;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingVisibility;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for Phase 8 — TrainingTemplate + CoachPlan.
 *
 * <ul>
 *   <li>Admin vytvoří šablonu (TEMPLATE visibility, owner=NULL).</li>
 *   <li>Admin přiřadí šablonu klientovi → vznikne PRIVATE trénink se sourceTemplate FK.</li>
 *   <li>CoachPlan CRUD + autorizace klienta vs cizí plán.</li>
 *   <li>MarkdownRenderer renderuje a escapuje HTML.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase8FeaturesTest {

    @Autowired private TrainingService trainingService;
    @Autowired private CoachPlanService coachPlanService;
    @Autowired private MarkdownRenderer markdownRenderer;
    @Autowired private ExerciseTypeConfigToInputMapper toInputMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private AccountEntity bob;
    private AccountEntity trainer;

    @BeforeEach
    void seed() {
        alice   = createUser("alice@example.com", "Alice", AccountRole.USER);
        bob     = createUser("bob@example.com",   "Bob",   AccountRole.USER);
        trainer = createUser("coach@example.com", "Coach", AccountRole.ADMIN);
    }

    // -----------------------------------------------------------------------------
    // TrainingTemplate
    // -----------------------------------------------------------------------------

    @Test
    void adminCreatesTemplate_setsVisibilityTemplate() {
        TrainingEntity template = trainingService.createTemplate(trainer, sampleInputWithSets());

        assertThat(template.getVisibility()).isEqualTo(TrainingVisibility.TEMPLATE);
        assertThat(template.getOwner()).isNull();
        assertThat(template.getCreatedBy().getId()).isEqualTo(trainer.getId());
    }

    @Test
    void listAllTemplates_returnsOnlyTemplates() {
        trainingService.createTemplate(trainer, sampleInputWithSets());
        trainingService.createTemplate(trainer, sampleInputWithSets());
        // group + private nemají být v listu šablon
        trainingService.createGroup(trainer, sampleInputWithSets());
        trainingService.create(alice, sampleInputWithSets());

        List<TrainingEntity> templates = trainingService.listAllTemplates();
        assertThat(templates).hasSize(2);
        assertThat(templates).allMatch(t -> t.getVisibility() == TrainingVisibility.TEMPLATE);
    }

    @Test
    void assignTemplateToClient_createsPrivateTrainingWithSourceTemplateFk() {
        TrainingEntity template = trainingService.createTemplate(trainer, sampleInputWithSets());

        LocalDate date = LocalDate.now().plusDays(1);
        TrainingEntity assigned = trainingService.assignTemplateToClient(
                template.getId(), alice, date, toInputMapper);

        assertThat(assigned.getVisibility()).isEqualTo(TrainingVisibility.PRIVATE);
        assertThat(assigned.getOwner().getId()).isEqualTo(alice.getId());
        assertThat(assigned.getTrainingDate()).isEqualTo(date);
        assertThat(assigned.getSourceTemplate()).isNotNull();
        assertThat(assigned.getSourceTemplate().getId()).isEqualTo(template.getId());
        // cviky se zkopírovaly
        assertThat(assigned.getExercises()).hasSize(template.getExercises().size());
    }

    @Test
    void listTrainingsFromTemplate_findsAssignments() {
        TrainingEntity template = trainingService.createTemplate(trainer, sampleInputWithSets());
        trainingService.assignTemplateToClient(template.getId(), alice, LocalDate.now(), toInputMapper);
        trainingService.assignTemplateToClient(template.getId(), bob,   LocalDate.now(), toInputMapper);

        List<TrainingEntity> assigned = trainingService.listTrainingsFromTemplate(template.getId());
        assertThat(assigned).hasSize(2);
        assertThat(assigned).allMatch(t -> t.getVisibility() == TrainingVisibility.PRIVATE);
    }

    @Test
    void getTemplate_onPrivateTraining_rejected() {
        TrainingEntity privateTraining = trainingService.create(alice, sampleInputWithSets());

        assertThatThrownBy(() -> trainingService.getTemplate(privateTraining.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("není šablona");
    }

    // -----------------------------------------------------------------------------
    // CoachPlan
    // -----------------------------------------------------------------------------

    @Test
    void createCoachPlan_storesFields() {
        CoachPlanEntity plan = coachPlanService.create(
                trainer, alice.getId(),
                "Letní příprava",
                "# Cíl\n3x týdně KB swing",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));

        assertThat(plan.getId()).isNotNull();
        assertThat(plan.getClient().getId()).isEqualTo(alice.getId());
        assertThat(plan.getAuthor().getId()).isEqualTo(trainer.getId());
        assertThat(plan.getTitle()).isEqualTo("Letní příprava");
        assertThat(plan.getBodyMarkdown()).contains("KB swing");
        assertThat(plan.getValidFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void listForClient_ordersByValidFromDesc() {
        coachPlanService.create(trainer, alice.getId(), "Starý", "body", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30));
        coachPlanService.create(trainer, alice.getId(), "Nový",  "body", LocalDate.of(2026, 1, 1), null);

        List<CoachPlanEntity> plans = coachPlanService.listForClient(alice.getId());
        assertThat(plans).hasSize(2);
        assertThat(plans.get(0).getTitle()).isEqualTo("Nový");
        assertThat(plans.get(1).getTitle()).isEqualTo("Starý");
    }

    @Test
    void findActiveForClient_returnsCurrentlyValid() {
        coachPlanService.create(trainer, alice.getId(), "Past",   "body", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30));
        coachPlanService.create(trainer, alice.getId(), "Active", "body", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        Optional<CoachPlanEntity> active = coachPlanService.findActiveForClient(alice.getId(), LocalDate.of(2026, 5, 24));
        assertThat(active).isPresent();
        assertThat(active.get().getTitle()).isEqualTo("Active");
    }

    @Test
    void getForClientOrAdmin_clientCannotReadOtherClientsPlan() {
        CoachPlanEntity alicePlan = coachPlanService.create(
                trainer, alice.getId(), "Alice plán", "body", LocalDate.now(), null);

        // Bob (jiný klient) nesmí číst Alicin plán
        assertThatThrownBy(() -> coachPlanService.getForClientOrAdmin(bob, alicePlan.getId()))
                .isInstanceOf(ForbiddenException.class);

        // Alice ano, admin ano
        assertThat(coachPlanService.getForClientOrAdmin(alice, alicePlan.getId()).getId())
                .isEqualTo(alicePlan.getId());
        assertThat(coachPlanService.getForClientOrAdmin(trainer, alicePlan.getId()).getId())
                .isEqualTo(alicePlan.getId());
    }

    @Test
    void updateCoachPlan_changesFields() {
        CoachPlanEntity plan = coachPlanService.create(
                trainer, alice.getId(), "Původní", "body", LocalDate.now(), null);

        CoachPlanEntity updated = coachPlanService.update(
                plan.getId(), "Aktualizovaný", "**nová** body", LocalDate.now(), LocalDate.now().plusMonths(2));

        assertThat(updated.getTitle()).isEqualTo("Aktualizovaný");
        assertThat(updated.getBodyMarkdown()).contains("**nová**");
        assertThat(updated.getValidTo()).isNotNull();
    }

    @Test
    void deleteCoachPlan_removesIt() {
        CoachPlanEntity plan = coachPlanService.create(
                trainer, alice.getId(), "Smaž mě", "body", LocalDate.now(), null);

        coachPlanService.delete(plan.getId());
        assertThat(coachPlanService.listForClient(alice.getId())).isEmpty();
    }

    // -----------------------------------------------------------------------------
    // MarkdownRenderer
    // -----------------------------------------------------------------------------

    @Test
    void markdownRenderer_rendersBasicMarkdown() {
        String html = markdownRenderer.renderToHtml("# Nadpis\n\nText *kurzíva* a **bold**.");
        assertThat(html).contains("<h1>Nadpis</h1>");
        assertThat(html).contains("<em>kurzíva</em>");
        assertThat(html).contains("<strong>bold</strong>");
    }

    @Test
    void markdownRenderer_escapesRawHtml() {
        String html = markdownRenderer.renderToHtml("Hello <script>alert('xss')</script>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void markdownRenderer_handlesNullAndBlank() {
        assertThat(markdownRenderer.renderToHtml(null)).isEqualTo("");
        assertThat(markdownRenderer.renderToHtml("")).isEqualTo("");
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

    private TrainingInput sampleInputWithSets() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Šablona KB");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("KB swing");
        SetInput set = new SetInput();
        set.setWeightKg(new BigDecimal("16.0"));
        set.setReps(20);
        ex.getSets().add(set);
        input.getExercises().add(ex);
        return input;
    }
}
