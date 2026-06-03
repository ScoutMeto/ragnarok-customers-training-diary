package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CircuitConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CircuitRoundLogInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CompositeSetConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import java.util.List;
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

    @Test
    void circuitRoundLog_savesReplacesAndChecksOwnership() {
        // circuit se 2 kroky, 3 koly
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Circuit log test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.CIRCUIT);
        ex.setCustomName("Circuit");
        var c = new CircuitConfigInput();
        c.setRounds(3);
        var s1 = new CircuitConfigInput.StepInput(); s1.setName("KB swing"); s1.setReps(15);
        var s2 = new CircuitConfigInput.StepInput(); s2.setName("Burpee"); s2.setReps(10);
        c.getSteps().add(s1); c.getSteps().add(s2);
        ex.setCircuit(c);
        input.getExercises().add(ex);
        TrainingEntity created = trainingService.create(alice, input);
        Long exId = created.getExercises().get(0).getId();

        // záznam: kolo1/krok0 vyřazen; kolo2/krok1 nahrazen + skutečné reps
        var log = new CircuitRoundLogInput();
        var e1 = new CircuitRoundLogInput.EntryInput();
        e1.setRoundIndex(1); e1.setStepOrder(0); e1.setSkipped(true);
        var e2 = new CircuitRoundLogInput.EntryInput();
        e2.setRoundIndex(2); e2.setStepOrder(1); e2.setSubstituteName("Sit-up"); e2.setActualReps(8);
        var e3empty = new CircuitRoundLogInput.EntryInput(); // bez dat → ignorováno
        e3empty.setRoundIndex(3); e3empty.setStepOrder(0);
        log.getEntries().addAll(List.of(e1, e2, e3empty));

        trainingService.saveCircuitRoundLog(alice, exId, log.getEntries());

        var cfg = trainingService.getMyTraining(alice, created.getId())
                .getExercises().get(0).getCircuitConfig();
        assertThat(cfg.getRoundEntries()).hasSize(2); // prázdný se neuložil
        var skipped = cfg.getRoundEntries().stream()
                .filter(x -> x.getRoundIndex() == 1 && x.getStepOrder() == 0).findFirst().orElseThrow();
        assertThat(skipped.isSkipped()).isTrue();
        var sub = cfg.getRoundEntries().stream()
                .filter(x -> x.getRoundIndex() == 2 && x.getStepOrder() == 1).findFirst().orElseThrow();
        assertThat(sub.getSubstituteName()).isEqualTo("Sit-up");
        assertThat(sub.getActualReps()).isEqualTo(8);

        // replace: druhé uložení s jediným záznamem nahradí předchozí
        var log2 = new CircuitRoundLogInput();
        var only = new CircuitRoundLogInput.EntryInput();
        only.setRoundIndex(3); only.setStepOrder(1); only.setActualReps(12);
        log2.getEntries().add(only);
        trainingService.saveCircuitRoundLog(alice, exId, log2.getEntries());
        var cfg2 = trainingService.getMyTraining(alice, created.getId())
                .getExercises().get(0).getCircuitConfig();
        assertThat(cfg2.getRoundEntries()).hasSize(1);
        assertThat(cfg2.getRoundEntries().get(0).getActualReps()).isEqualTo(12);

        // ownership: cizí uživatel nesmí
        AccountEntity mallory = new AccountEntity();
        mallory.setEmail("mallory@example.cz");
        mallory.setPasswordHash(passwordEncoder.encode("x"));
        mallory.setRole(AccountRole.USER);
        mallory.setNickname("m"); mallory.setFirstName("M"); mallory.setLastName("M");
        mallory.setEmailConfirmed(true);
        mallory = accountRepository.save(mallory);
        AccountEntity finalMallory = mallory;
        assertThatThrownBy(() -> trainingService.saveCircuitRoundLog(finalMallory, exId, log2.getEntries()))
                .isInstanceOf(ForbiddenException.class);
    }

    private SetInput set(int weight, int reps) {
        var s = new SetInput();
        s.setWeightKg(new java.math.BigDecimal(weight));
        s.setReps(reps);
        return s;
    }
}
