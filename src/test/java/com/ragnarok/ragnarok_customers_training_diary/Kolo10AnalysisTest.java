package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.analysis.AnalysisService;
import com.ragnarok.ragnarok_customers_training_diary.analysis.PerformanceAnalysisService;
import com.ragnarok.ragnarok_customers_training_diary.tag.SystemTag;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.AmrapConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CardioConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CircuitConfigInput;
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
 * ScoutMeto kolo 10: výkonnostní analytika — charakter provedení rozhoduje o tom,
 * které metriky se počítají, a pět oblastí musí sedět na správné cviky.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Kolo10AnalysisTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AnalysisService analysisService;
    @Autowired private PerformanceAnalysisService performance;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private TrainingTagEntity carryTag;
    private TrainingTagEntity isometryTag;
    private TrainingTagEntity bodyweightTag;
    private TrainingTagEntity upperBodyTag;
    private TrainingTagEntity pushTag;
    private TrainingTagEntity jumpsTag;

    private static final LocalDate FROM = LocalDate.now().minusDays(7);
    private static final LocalDate TO = LocalDate.now().plusDays(1);

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("kolo10analysis@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("k10a");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);

        carryTag = systemTag("Nošení", SystemTag.CARRY);
        isometryTag = systemTag("Izometrie", SystemTag.ISOMETRY);
        bodyweightTag = systemTag("Vlastní váha", SystemTag.BODYWEIGHT);
        upperBodyTag = systemTag("Horní část těla", SystemTag.UPPER_BODY);
        pushTag = systemTag("Tlak", SystemTag.PUSH);
        jumpsTag = systemTag("Skoky, výskoky", SystemTag.JUMPS);
    }

    private TrainingTagEntity systemTag(String name, String key) {
        TrainingTagEntity tag = new TrainingTagEntity();
        tag.setName(name);
        tag.setSystem(true);
        tag.setSystemKey(key);
        return tagRepository.save(tag);
    }

    @Test
    void repetitiveExercise_countsSetsRepsAndLiftedWeight() {
        TrainingInput in = training("Silový");
        var ex = exercise(TrainingExerciseType.FREEFORM, "Dřep");
        ex.setEquipmentWeightKg(new BigDecimal("60"));
        ex.getSets().add(set(null, 5));
        ex.getSets().add(set(null, 5));
        in.getExercises().add(ex);
        trainingService.create(alice, in);

        var s = performance.exerciseSummary(alice, "Dřep", FROM, TO);
        assertThat(s.character()).isEqualTo("REPETITIVE");
        assertThat(s.sets()).isEqualTo(2);
        assertThat(s.reps()).isEqualTo(10);
        // váha náčiní se použije, když ji set nemá vlastní
        assertThat(s.liftedKg()).isEqualByComparingTo(new BigDecimal("600"));
    }

    @Test
    void carryExercise_countsMetersAndWeightRange() {
        TrainingInput in = training("Nošení");
        var ex = exercise(TrainingExerciseType.FREEFORM, "Farmer's walk");
        ex.setSetUnit("METERS");
        ex.getTagIds().add(carryTag.getId());
        ex.getSets().add(set(new BigDecimal("24"), 40));
        ex.getSets().add(set(new BigDecimal("32"), 30));
        in.getExercises().add(ex);
        trainingService.create(alice, in);

        var s = performance.exerciseSummary(alice, "Farmer's walk", FROM, TO);
        assertThat(s.character()).isEqualTo("CARRY");
        assertThat(s.meters()).isEqualTo(70);
        assertThat(s.reps()).isZero();   // Nošení se nepočítá na opakování
        assertThat(s.minWeightKg()).isEqualByComparingTo(new BigDecimal("24"));
        assertThat(s.maxWeightKg()).isEqualByComparingTo(new BigDecimal("32"));
    }

    @Test
    void isometryExercise_countsOnlySeconds() {
        TrainingInput in = training("Výdrže");
        var ex = exercise(TrainingExerciseType.FREEFORM, "Plank");
        ex.setSetUnit("SECONDS");
        ex.getTagIds().add(isometryTag.getId());
        ex.getSets().add(set(null, 60));
        ex.getSets().add(set(null, 45));
        in.getExercises().add(ex);
        trainingService.create(alice, in);

        var s = performance.exerciseSummary(alice, "Plank", FROM, TO);
        assertThat(s.character()).isEqualTo("ISOMETRY");
        assertThat(s.seconds()).isEqualTo(105);
        assertThat(s.reps()).isZero();
        assertThat(s.meters()).isZero();
    }

    @Test
    void areas_splitExternalWeightBodyweightAndCardio() {
        TrainingInput in = training("Smíšený");

        var barbell = exercise(TrainingExerciseType.FREEFORM, "Bench");
        barbell.setEquipmentWeightKg(new BigDecimal("50"));
        barbell.getSets().add(set(null, 10));
        in.getExercises().add(barbell);

        var pullup = exercise(TrainingExerciseType.FREEFORM, "Shyb");
        pullup.getTagIds().add(bodyweightTag.getId());
        pullup.getSets().add(set(null, 8));
        in.getExercises().add(pullup);

        var run = exercise(TrainingExerciseType.CARDIO, "Běh");
        var cardio = new com.ragnarok.ragnarok_customers_training_diary.training.dto.CardioConfigInput();
        cardio.setElapsedMin(30);
        run.setCardio(cardio);
        in.getExercises().add(run);

        trainingService.create(alice, in);

        List<PerformanceAnalysisService.AreaPoint> areas =
                performance.performanceAreas(alice, FROM, TO);
        assertThat(areas).hasSize(1);
        var a = areas.get(0);
        assertThat(a.externalWeight()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(a.bodyweightReps()).isEqualTo(8);
        assertThat(a.longCardioSeconds()).isEqualTo(1800);
    }

    @Test
    void distributions_groupByCharacterAndLoadKind() {
        TrainingInput in = training("Rozpad");
        var kb = exercise(TrainingExerciseType.FREEFORM, "Swing");
        kb.setEquipmentWeightKg(new BigDecimal("24"));
        kb.getSets().add(set(null, 20));
        in.getExercises().add(kb);

        var carry = exercise(TrainingExerciseType.FREEFORM, "Carry");
        carry.setSetUnit("METERS");
        carry.getTagIds().add(carryTag.getId());
        carry.setEquipmentWeightKg(new BigDecimal("32"));
        carry.getSets().add(set(null, 50));
        in.getExercises().add(carry);
        trainingService.create(alice, in);

        var d = performance.distributions(alice, FROM, TO);
        assertThat(d.byCharacter()).containsEntry("Repetitivní provedení", 1L);
        assertThat(d.byCharacter()).containsEntry("Nošení", 1L);
        assertThat(d.byLoadKind()).containsEntry("Náčiní", 2L);
        assertThat(d.repetitiveReps()).isEqualTo(20);
        assertThat(d.carryMeters()).isEqualTo(50);
    }


    @Test
    void circuitStepTags_feedCharacterBodyRegionAndMovementPatternAnalysis() {
        TrainingInput in = training("Circuit tagy");
        var ex = exercise(TrainingExerciseType.CIRCUIT, "Circuit");
        var circuit = new CircuitConfigInput();
        circuit.setRounds(2);

        var step = new CircuitConfigInput.StepInput();
        step.setName("Wall sit jump hold");
        step.setReps(30);
        step.setRepUnit("SECONDS");
        step.setJumpHeightCm(new BigDecimal("45.5"));
        step.getTagIds().add(isometryTag.getId());
        step.getTagIds().add(upperBodyTag.getId());
        step.getTagIds().add(pushTag.getId());
        step.getTagIds().add(jumpsTag.getId());
        circuit.getSteps().add(step);
        ex.setCircuit(circuit);
        in.getExercises().add(ex);

        var saved = trainingService.create(alice, in);

        assertThat(saved.getExercises().get(0).getCircuitConfig().getSteps().get(0).getJumpHeightCm())
                .isEqualByComparingTo(new BigDecimal("45.5"));
        assertThat(performance.distributions(alice, FROM, TO).byCharacter())
                .containsEntry("Izometrie", 1L);
        assertThat(valueOf(analysisService.setsPerBodyRegion(alice, FROM, TO), "Horní část těla"))
                .isEqualByComparingTo(new BigDecimal("2"));
        assertThat(valueOf(analysisService.setsPerMovementPattern(alice, FROM, TO), "Tlak"))
                .isEqualByComparingTo(new BigDecimal("2"));
    }

    @Test
    void tagStats_includeCircuitAndAmrapStepTags() {
        TrainingInput circuitTraining = training("Tag stats circuit");
        var circuitExercise = exercise(TrainingExerciseType.CIRCUIT, "Circuit block");
        var circuit = new CircuitConfigInput();
        circuit.setRounds(2);
        var circuitStep = new CircuitConfigInput.StepInput();
        circuitStep.setOrderIndex(0);
        circuitStep.setName("Tagged circuit push");
        circuitStep.setReps(5);
        circuitStep.setWeightKg(new BigDecimal("10"));
        circuitStep.getTagIds().add(pushTag.getId());
        circuit.getSteps().add(circuitStep);
        circuitExercise.setCircuit(circuit);
        circuitTraining.getExercises().add(circuitExercise);
        trainingService.create(alice, circuitTraining);

        TrainingInput amrapTraining = training("Tag stats amrap");
        var amrapExercise = exercise(TrainingExerciseType.AMRAP, "AMRAP block");
        var amrap = new AmrapConfigInput();
        amrap.setTimecapSeconds(300);
        amrap.setRoundsCompleted(3);
        var amrapStep = new AmrapConfigInput.StepInput();
        amrapStep.setOrderIndex(0);
        amrapStep.setName("Tagged amrap push");
        amrapStep.setReps(4);
        amrapStep.setWeightKg(new BigDecimal("5"));
        amrapStep.getTagIds().add(pushTag.getId());
        amrap.getSteps().add(amrapStep);
        amrapExercise.setAmrap(amrap);
        amrapTraining.getExercises().add(amrapExercise);
        trainingService.create(alice, amrapTraining);

        assertThat(analysisService.listUsedExerciseTags(alice))
                .extracting(TrainingTagEntity::getName)
                .contains(pushTag.getName());

        var stats = analysisService.statsByExerciseTags(alice, List.of(pushTag.getId()), FROM, TO);
        assertThat(stats.totalVolumeKg()).isEqualByComparingTo(new BigDecimal("160"));
        assertThat(stats.totalSets()).isEqualTo(5);
        assertThat(stats.totalReps()).isEqualTo(22);
        assertThat(stats.maxReps()).isEqualTo(5);
        assertThat(stats.maxWeightKg()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(stats.totalMeters()).isZero();
        assertThat(stats.totalSeconds()).isZero();
        assertThat(stats.cardioSeconds()).isZero();
    }

    @Test
    void tagStats_exposeDistanceTimeAndCardioMetrics() {
        TrainingTagEntity cardioTag = systemTag("Kardio", SystemTag.CARDIO);

        TrainingInput in = training("Tag metriky");

        var carryMeters = exercise(TrainingExerciseType.FREEFORM, "Carry meters");
        carryMeters.setSetUnit("METERS");
        carryMeters.getTagIds().add(carryTag.getId());
        carryMeters.getSets().add(set(null, 40));
        in.getExercises().add(carryMeters);

        var carrySeconds = exercise(TrainingExerciseType.FREEFORM, "Carry seconds");
        carrySeconds.setSetUnit("SECONDS");
        carrySeconds.getTagIds().add(carryTag.getId());
        carrySeconds.getSets().add(set(null, 30));
        in.getExercises().add(carrySeconds);

        var plank = exercise(TrainingExerciseType.FREEFORM, "Plank stats");
        plank.setSetUnit("SECONDS");
        plank.getTagIds().add(isometryTag.getId());
        plank.getSets().add(set(null, 45));
        in.getExercises().add(plank);

        var jumpReps = exercise(TrainingExerciseType.FREEFORM, "Jump reps");
        jumpReps.getTagIds().add(jumpsTag.getId());
        jumpReps.getSets().add(set(null, 12));
        in.getExercises().add(jumpReps);

        var jumpMeters = exercise(TrainingExerciseType.FREEFORM, "Jump meters");
        jumpMeters.setSetUnit("METERS");
        jumpMeters.getTagIds().add(jumpsTag.getId());
        jumpMeters.getSets().add(set(null, 8));
        in.getExercises().add(jumpMeters);

        var jumpSeconds = exercise(TrainingExerciseType.FREEFORM, "Jump seconds");
        jumpSeconds.setSetUnit("SECONDS");
        jumpSeconds.getTagIds().add(jumpsTag.getId());
        jumpSeconds.getSets().add(set(null, 20));
        in.getExercises().add(jumpSeconds);

        var cardio = exercise(TrainingExerciseType.CARDIO, "Long slow cardio");
        cardio.getTagIds().add(cardioTag.getId());
        var cardioCfg = new CardioConfigInput();
        cardioCfg.setElapsedMin(30);
        cardioCfg.setActiveMin(25);
        cardioCfg.setDistance(new BigDecimal("5"));
        cardioCfg.setDistanceUnit("KM");
        cardioCfg.setRepetitions(100);
        cardio.setCardio(cardioCfg);
        in.getExercises().add(cardio);

        var taggedAsCardio = exercise(TrainingExerciseType.FREEFORM, "User cardio tag strength");
        taggedAsCardio.getTagIds().add(cardioTag.getId());
        taggedAsCardio.getSets().add(set(new BigDecimal("20"), 10));
        in.getExercises().add(taggedAsCardio);

        trainingService.create(alice, in);

        var carry = analysisService.statsByExerciseTags(alice, List.of(carryTag.getId()), FROM, TO);
        assertThat(carry.totalSets()).isEqualTo(2);
        assertThat(carry.totalReps()).isZero();
        assertThat(carry.totalMeters()).isEqualTo(40);
        assertThat(carry.totalSeconds()).isEqualTo(30);

        var isometry = analysisService.statsByExerciseTags(alice, List.of(isometryTag.getId()), FROM, TO);
        assertThat(isometry.totalSets()).isEqualTo(1);
        assertThat(isometry.totalSeconds()).isEqualTo(45);

        var jumps = analysisService.statsByExerciseTags(alice, List.of(jumpsTag.getId()), FROM, TO);
        assertThat(jumps.totalSets()).isEqualTo(3);
        assertThat(jumps.totalReps()).isEqualTo(12);
        assertThat(jumps.totalMeters()).isEqualTo(8);
        assertThat(jumps.totalSeconds()).isEqualTo(20);

        var cardioStats = analysisService.statsByExerciseTags(alice, List.of(cardioTag.getId()), FROM, TO);
        assertThat(cardioStats.totalSets()).isEqualTo(2);
        assertThat(cardioStats.totalReps()).isEqualTo(110);
        assertThat(cardioStats.totalVolumeKg()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(cardioStats.totalMeters()).isEqualTo(5000);
        assertThat(cardioStats.totalSeconds()).isEqualTo(1500);
        assertThat(cardioStats.cardioSeconds()).isEqualTo(1500);
    }
    @Test
    void topLevelTags_feedAnalysisAcrossOtherExerciseTypesWithTypeSpecificCounts() {
        List<TypeCase> cases = List.of(
                new TypeCase(freeformTaggedExercise(), 2),
                new TypeCase(emomTaggedExercise(), 3),
                new TypeCase(tabataTaggedExercise(), 4),
                new TypeCase(numericSeriesTaggedExercise(TrainingExerciseType.LADDER), 3),
                new TypeCase(numericSeriesTaggedExercise(TrainingExerciseType.STEPLADDER), 3),
                new TypeCase(numericSeriesTaggedExercise(TrainingExerciseType.PYRAMID), 3),
                new TypeCase(straightSetsTaggedExercise(), 3),
                new TypeCase(intervalTaggedExercise(), 5),
                new TypeCase(strongFirstTaggedExercise(), 4),
                new TypeCase(kbSportTaggedExercise(), 1),
                new TypeCase(cardioTaggedExercise(), 1)
        );

        long expectedUnits = 0;
        for (TypeCase typeCase : cases) {
            TrainingExerciseInput ex = typeCase.exercise();
            ex.getTagIds().add(upperBodyTag.getId());
            ex.getTagIds().add(pushTag.getId());
            TrainingInput in = training("Typ " + ex.getType().name());
            in.getExercises().add(ex);
            trainingService.create(alice, in);
            expectedUnits += typeCase.expectedUnits();
        }

        assertThat(valueOf(analysisService.setsPerBodyRegion(alice, FROM, TO), "Horní část těla"))
                .isEqualByComparingTo(BigDecimal.valueOf(expectedUnits));
        assertThat(valueOf(analysisService.setsPerMovementPattern(alice, FROM, TO), "Tlak"))
                .isEqualByComparingTo(BigDecimal.valueOf(expectedUnits));
        assertThat(performance.distributions(alice, FROM, TO).byBodyPart())
                .containsEntry("Horní část těla", (long) cases.size());
    }
    @Test
    void trainingSummaries_filterByTrainingTags() {
        TrainingTagEntity strength = systemTag("Síla", SystemTag.STRENGTH);

        TrainingInput tagged = training("S tagem");
        tagged.getTagIds().add(strength.getId());
        var ex = exercise(TrainingExerciseType.FREEFORM, "Dřep");
        ex.getSets().add(set(new BigDecimal("60"), 5));
        tagged.getExercises().add(ex);
        trainingService.create(alice, tagged);

        trainingService.create(alice, training("Bez tagu"));

        assertThat(performance.trainingSummaries(alice, FROM, TO, null)).hasSize(2);
        var filtered = performance.trainingSummaries(alice, FROM, TO, List.of(strength.getId()));
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).name()).isEqualTo("S tagem");
        assertThat(filtered.get(0).total().reps()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------



    private TrainingExerciseInput freeformTaggedExercise() {
        var ex = exercise(TrainingExerciseType.FREEFORM, "Freeform tag test");
        ex.getSets().add(set(null, 5));
        ex.getSets().add(set(null, 5));
        return ex;
    }

    private TrainingExerciseInput emomTaggedExercise() {
        var ex = exercise(TrainingExerciseType.EMOM, "EMOM tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.EmomConfigInput();
        cfg.setTotalMinutes(3);
        cfg.setDefaultReps(10);
        ex.setEmom(cfg);
        return ex;
    }

    private TrainingExerciseInput tabataTaggedExercise() {
        var ex = exercise(TrainingExerciseType.TABATA, "Tabata tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.TabataConfigInput();
        cfg.setRounds(4);
        cfg.setDefaultReps(8);
        ex.setTabata(cfg);
        return ex;
    }

    private TrainingExerciseInput numericSeriesTaggedExercise(TrainingExerciseType type) {
        var ex = exercise(type, type.name() + " tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.NumericSeriesConfigInput();
        cfg.setStartValue(1);
        cfg.setPeakValue(3);
        cfg.setStepSize(1);
        for (int i = 0; i < 3; i++) {
            var row = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                    .NumericSeriesConfigInput.RowInput();
            row.setRowIndex(i);
            row.setRung(i + 1);
            row.setReps(i + 1);
            cfg.getRows().add(row);
        }
        ex.setNumericSeries(cfg);
        return ex;
    }

    private TrainingExerciseInput straightSetsTaggedExercise() {
        var ex = exercise(TrainingExerciseType.STRAIGHT_SETS, "Straight sets tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.StraightSetsConfigInput();
        cfg.setSetCount(3);
        cfg.setRepsPerSet(5);
        for (int i = 0; i < 3; i++) {
            var row = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                    .StraightSetsConfigInput.RowInput();
            row.setReps(5);
            cfg.getRows().add(row);
        }
        ex.setStraightSets(cfg);
        return ex;
    }

    private TrainingExerciseInput intervalTaggedExercise() {
        var ex = exercise(TrainingExerciseType.INTERVAL, "Interval tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.IntervalConfigInput();
        cfg.setRounds(5);
        cfg.setWorkReps(10);
        cfg.setRestSec(30);
        ex.setInterval(cfg);
        return ex;
    }

    private TrainingExerciseInput strongFirstTaggedExercise() {
        var ex = exercise(TrainingExerciseType.STRONGFIRST_LADDER, "StrongFirst tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                .StrongFirstLadderConfigInput();
        cfg.setLadderHeight(2);
        cfg.setCycles(2);
        for (int i = 0; i < 4; i++) {
            var row = new com.ragnarok.ragnarok_customers_training_diary.training.dto
                    .StrongFirstLadderConfigInput.RowInput();
            row.setLadderIndex(i < 2 ? 1 : 2);
            row.setRung((i % 2) + 1);
            row.setValue((i % 2) + 1);
            cfg.getRows().add(row);
        }
        ex.setStrongFirstLadder(cfg);
        return ex;
    }

    private TrainingExerciseInput kbSportTaggedExercise() {
        var ex = exercise(TrainingExerciseType.KB_SPORT_TIME, "KB sport tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.KbSportConfigInput();
        cfg.setTotalMinutes(1);
        cfg.setTotalReps(20);
        ex.setKbSport(cfg);
        return ex;
    }

    private TrainingExerciseInput cardioTaggedExercise() {
        var ex = exercise(TrainingExerciseType.CARDIO, "Cardio tag test");
        var cfg = new com.ragnarok.ragnarok_customers_training_diary.training.dto.CardioConfigInput();
        cfg.setElapsedMin(30);
        cfg.setDistance(new BigDecimal("5"));
        cfg.setDistanceUnit("KM");
        ex.setCardio(cfg);
        return ex;
    }

    private record TypeCase(TrainingExerciseInput exercise, long expectedUnits) {}
    private BigDecimal valueOf(List<AnalysisService.LabelValuePoint> points, String label) {
        return points.stream()
                .filter(point -> point.label().equals(label))
                .map(AnalysisService.LabelValuePoint::value)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
    private TrainingInput training(String name) {
        TrainingInput in = new TrainingInput();
        in.setTrainingDate(LocalDate.now());
        in.setName(name);
        return in;
    }

    private TrainingExerciseInput exercise(TrainingExerciseType type, String name) {
        var ex = new TrainingExerciseInput();
        ex.setType(type);
        ex.setCustomName(name);
        return ex;
    }

    private SetInput set(BigDecimal weight, Integer reps) {
        SetInput s = new SetInput();
        s.setWeightKg(weight);
        s.setReps(reps);
        return s;
    }
}
