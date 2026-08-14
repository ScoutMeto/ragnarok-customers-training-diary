package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.catalog.BodyRegion;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemRepository;
import com.ragnarok.ragnarok_customers_training_diary.tag.SystemTag;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CircuitConfigInput;
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
 * ScoutMeto kolo 10: sjednocený CIRCUIT/SUPERSET/COMPLEX, pauzy po kolech,
 * systémové klíče tagů a víceznačné atributy katalogu.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Kolo10FeaturesTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private ExerciseCatalogItemRepository catalogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("kolo10@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("k10");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void circuit_persistsModeComplex() {
        TrainingInput input = base();
        var c = new CircuitConfigInput();
        c.setMode("COMPLEX");
        c.setRounds(3);
        var s = new CircuitConfigInput.StepInput();
        s.setName("KB Clean");
        s.setReps(5);
        c.getSteps().add(s);
        input.getExercises().get(0).setCircuit(c);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getCircuitConfig();
        assertThat(cfg.getMode()).isEqualTo("COMPLEX");
        assertThat(cfg.getSteps()).hasSize(1);
    }

    @Test
    void circuit_defaultsToCircuitModeWhenNotSent() {
        TrainingInput input = base();
        var c = new CircuitConfigInput();
        c.setMode(null);
        c.setRounds(2);
        var s = new CircuitConfigInput.StepInput();
        s.setName("Swing");
        c.getSteps().add(s);
        input.getExercises().get(0).setCircuit(c);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getCircuitConfig().getMode()).isEqualTo("CIRCUIT");
    }

    @Test
    void circuit_restBetweenRoundsCombinesMinutesAndSeconds() {
        TrainingInput input = base();
        var c = new CircuitConfigInput();
        c.setRounds(2);
        c.setRestBetweenRoundsMin(1);
        c.setRestBetweenRoundsSec(30);
        var s = new CircuitConfigInput.StepInput();
        s.setName("Swing");
        c.getSteps().add(s);
        input.getExercises().get(0).setCircuit(c);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getCircuitConfig().getRestBetweenRoundsS()).isEqualTo(90);
    }

    @Test
    void circuit_perRoundRestsPersistFromMinutesAndSeconds() {
        TrainingInput input = base();
        var c = new CircuitConfigInput();
        c.setRounds(2);
        var s = new CircuitConfigInput.StepInput();
        s.setName("Swing");
        c.getSteps().add(s);

        var r0 = new CircuitConfigInput.RoundRestInput();
        r0.setRoundIndex(0);
        r0.setRestMin(1);
        r0.setRestSec(30);
        var r1 = new CircuitConfigInput.RoundRestInput();
        r1.setRoundIndex(1);
        r1.setRestMin(2);
        r1.setRestSec(15);
        c.getRoundRests().add(r0);
        c.getRoundRests().add(r1);
        input.getExercises().get(0).setCircuit(c);

        TrainingEntity created = trainingService.create(alice, input);
        var rests = created.getExercises().get(0).getCircuitConfig().getRoundRests();
        assertThat(rests).hasSize(2);
        assertThat(rests.get(0).getRestSeconds()).isEqualTo(90);
        assertThat(rests.get(1).getRestSeconds()).isEqualTo(135);
    }

    @Test
    void amrap_persistsStepsRoundEntriesAndTimecapFromMinutes() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Kolo 10 AMRAP");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.AMRAP);
        ex.setCustomName("AMRAP");

        var a = new com.ragnarok.ragnarok_customers_training_diary.training.dto.AmrapConfigInput();
        a.setTimecapMin(5);
        a.setTimecapSec(30);
        a.setRoundsCompleted(2);

        var s1 = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .AmrapConfigInput.StepInput();
        s1.setName("KB Swing");
        s1.setReps(20);
        var s2 = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .AmrapConfigInput.StepInput();
        s2.setName("Burpee");
        s2.setReps(10);
        a.getSteps().add(s1);
        a.getSteps().add(s2);

        // druhé (rozjeté) kolo: první cvik jen částečně, druhý nestihnut
        var e1 = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .AmrapConfigInput.RoundEntryInput();
        e1.setRoundIndex(2);
        e1.setStepOrder(0);
        e1.setActualReps(15);
        var e2 = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .AmrapConfigInput.RoundEntryInput();
        e2.setRoundIndex(2);
        e2.setStepOrder(1);
        e2.setSkipped(true);
        a.getRoundEntries().add(e1);
        a.getRoundEntries().add(e2);
        ex.setAmrap(a);
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getAmrapConfig();
        assertThat(cfg.getTimecapSeconds()).isEqualTo(330);
        assertThat(cfg.getSteps()).hasSize(2);
        assertThat(cfg.getSteps().get(1).getName()).isEqualTo("Burpee");
        assertThat(cfg.getRoundEntries()).hasSize(2);
        assertThat(cfg.getRoundEntries().get(0).getActualReps()).isEqualTo(15);
        assertThat(cfg.getRoundEntries().get(1).isSkipped()).isTrue();
    }

    @Test
    void amrap_dropsRoundEntriesForDeletedSteps() {
        // Uživatel v editaci smaže cvik — řádky kol na neexistující cvik nesmí projít.
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Kolo 10 AMRAP cleanup");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.AMRAP);
        ex.setCustomName("AMRAP");

        var a = new com.ragnarok.ragnarok_customers_training_diary.training.dto.AmrapConfigInput();
        a.setTimecapMin(3);
        var s1 = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .AmrapConfigInput.StepInput();
        s1.setName("KB Swing");
        a.getSteps().add(s1);

        var orphan = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .AmrapConfigInput.RoundEntryInput();
        orphan.setRoundIndex(1);
        orphan.setStepOrder(3); // cvik, který už neexistuje
        a.getRoundEntries().add(orphan);
        ex.setAmrap(a);
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getAmrapConfig().getRoundEntries()).isEmpty();
    }

    @Test
    void straightSets_persistsGeneratedRowsAndRestFromMinutes() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Kolo 10 straight sets");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.STRAIGHT_SETS);
        ex.setCustomName("Dřep");

        var ss = new com.ragnarok.ragnarok_customers_training_diary.training.dto.StraightSetsConfigInput();
        ss.setSetCount(3);
        ss.setRepsPerSet(5);
        ss.setRestMin(2);
        ss.setRestSec(0);
        for (int i = 0; i < 3; i++) {
            var r = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                    .StraightSetsConfigInput.RowInput();
            r.setReps(5);
            r.setWeightKg(new java.math.BigDecimal("60.00"));
            r.setRestSeconds(120);
            ss.getRows().add(r);
        }
        ex.setStraightSets(ss);
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getStraightSetsConfig();
        assertThat(cfg.getRestSeconds()).isEqualTo(120);
        assertThat(cfg.getRows()).hasSize(3);
        assertThat(cfg.getRows().get(2).getRowIndex()).isEqualTo(2);
    }

    @Test
    void interval_combinesWorkAndRestFromMinutes() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Kolo 10 interval");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.INTERVAL);
        ex.setCustomName("Veslo");

        var iv = new com.ragnarok.ragnarok_customers_training_diary.training.dto.IntervalConfigInput();
        iv.setRounds(5);
        iv.setWorkMin(1);
        iv.setWorkSec(30);
        iv.setRestMin(0);
        iv.setRestSec(45);
        ex.setInterval(iv);
        input.getExercises().add(ex);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getIntervalConfig();
        assertThat(cfg.getWorkSeconds()).isEqualTo(90);
        assertThat(cfg.getRestSeconds()).isEqualTo(45);
    }

    @Test
    void systemTag_keepsStableKeyWhenRenamed() {
        // Detekce jednotek v UI se řídí klíčem, ne názvem — přejmenování ji nesmí rozbít.
        TrainingTagEntity carry = new TrainingTagEntity();
        carry.setName("Nošení");
        carry.setSystem(true);
        carry.setSystemKey(SystemTag.CARRY);
        carry = tagRepository.save(carry);

        carry.setName("Úplně jiný název");
        carry = tagRepository.save(carry);

        assertThat(carry.getSystemKey()).isEqualTo("CARRY");
    }

    @Test
    void catalogItem_holdsMultipleBodyRegionsAndPatterns() {
        ExerciseCatalogItemEntity item = new ExerciseCatalogItemEntity();
        item.setName("Dřep s výskokem");
        item.setSystem(false);
        item.getBodyRegions().add(BodyRegion.LOWER_BODY);
        item.getBodyRegions().add(BodyRegion.CORE);
        item.getMovementPatterns().add("SQUAT");
        item.getMovementPatterns().add("PLYO");
        item = catalogRepository.save(item);

        var reloaded = catalogRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getBodyRegions()).containsExactlyInAnyOrder(BodyRegion.LOWER_BODY, BodyRegion.CORE);
        assertThat(reloaded.getMovementPatterns()).containsExactlyInAnyOrder("SQUAT", "PLYO");
        assertThat(BodyRegion.LOWER_BODY.getLabel()).isEqualTo("Dolní část těla");
    }

    private TrainingInput base() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Kolo 10 test");
        var ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.CIRCUIT);
        ex.setCustomName("Circuit");
        input.getExercises().add(ex);
        return input;
    }
}
