package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingCommentEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingCommentService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
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
 * Phase 2 feature tests:
 *  - admin can view + comment on any client's training
 *  - regular user cannot see other users' trainings (regression)
 *  - regular user cannot comment on other users' trainings
 *  - soft delete anonymizes + disables login
 *
 * Group training tests jsou v {@link GroupTrainingTest} (V6+).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase2FeaturesTest {

    @Autowired private TrainingService trainingService;
    @Autowired private TrainingCommentService commentService;
    @Autowired private AccountService accountService;
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
    // Admin training bypass
    // -----------------------------------------------------------------------------

    @Test
    void adminCanReadAnyClientTraining() {
        TrainingEntity alicesTraining = trainingService.create(alice, sampleInput());

        TrainingEntity loaded = trainingService.getAnyTraining(alicesTraining.getId());
        assertThat(loaded.getOwner().getId()).isEqualTo(alice.getId());
    }

    @Test
    void adminCanListClientTrainings() {
        trainingService.create(alice, sampleInput());
        trainingService.create(alice, sampleInput());
        trainingService.create(bob,   sampleInput());

        assertThat(trainingService.listTrainingsOf(alice.getId())).hasSize(2);
        assertThat(trainingService.listTrainingsOf(bob.getId())).hasSize(1);
    }

    // -----------------------------------------------------------------------------
    // Komentáře
    // -----------------------------------------------------------------------------

    @Test
    void ownerCanCommentOwnTraining() {
        TrainingEntity training = trainingService.create(alice, sampleInput());

        TrainingCommentEntity comment = commentService.addComment(alice, training.getId(), "Cítil jsem se super.");
        assertThat(comment.getAuthor().getId()).isEqualTo(alice.getId());
        assertThat(comment.getText()).isEqualTo("Cítil jsem se super.");
    }

    @Test
    void adminCanCommentAnyTraining() {
        TrainingEntity alicesTraining = trainingService.create(alice, sampleInput());

        TrainingCommentEntity comment = commentService.addComment(trainer, alicesTraining.getId(), "Hezký progres!");
        assertThat(comment.getAuthor().getRole()).isEqualTo(AccountRole.ADMIN);
    }

    @Test
    void bobCannotCommentAlicesTraining() {
        TrainingEntity alicesTraining = trainingService.create(alice, sampleInput());

        assertThatThrownBy(() -> commentService.addComment(bob, alicesTraining.getId(), "Hej"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void emptyCommentTextRejected() {
        TrainingEntity training = trainingService.create(alice, sampleInput());

        assertThatThrownBy(() -> commentService.addComment(alice, training.getId(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteCommentAllowedForAuthorAndAdmin() {
        TrainingEntity training = trainingService.create(alice, sampleInput());
        TrainingCommentEntity aliceComment = commentService.addComment(alice, training.getId(), "ahoj");

        // Bob (cizí user) nesmí
        assertThatThrownBy(() -> commentService.deleteComment(bob, aliceComment.getId()))
                .isInstanceOf(ForbiddenException.class);

        // Autor smí
        commentService.deleteComment(alice, aliceComment.getId());

        // Admin smí na čerstvý komentář
        TrainingCommentEntity another = commentService.addComment(alice, training.getId(), "ahoj2");
        commentService.deleteComment(trainer, another.getId());
        assertThat(commentService.listForTraining(training.getId())).isEmpty();
    }

    // -----------------------------------------------------------------------------
    // Soft delete
    // -----------------------------------------------------------------------------

    @Test
    void softDeleteAnonymizesAndDisables() {
        accountService.softDelete(bob.getId());

        AccountEntity reloaded = accountRepository.findById(bob.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloaded.getEmail()).startsWith("deleted-" + bob.getId() + "-");
        assertThat(reloaded.getFirstName()).isEqualTo("Smazaný");
        assertThat(reloaded.isEnabled()).isFalse();  // soft-deleted nesmí login
    }

    @Test
    void softDeletedAccountNotInActiveList() {
        accountService.softDelete(bob.getId());

        List<AccountEntity> active = accountService.listActiveAccounts();
        assertThat(active).noneMatch(a -> a.getId().equals(bob.getId()));
        assertThat(active).anyMatch(a -> a.getId().equals(alice.getId()));
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
