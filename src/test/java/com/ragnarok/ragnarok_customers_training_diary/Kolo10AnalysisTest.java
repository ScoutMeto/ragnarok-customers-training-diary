package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.analysis.PerformanceAnalysisService;
import com.ragnarok.ragnarok_customers_training_diary.tag.SystemTag;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
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
 * ScoutMeto kolo 10: výkonnostní analytika — charakter provedení rozhoduje o tom,
 * které metriky se počítají, a pět oblastí musí sedět na správné cviky.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Kolo10AnalysisTest {

    @Autowired private TrainingService trainingService;
    @Autowired private PerformanceAnalysisService performance;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private TrainingTagEntity carryTag;
    private TrainingTagEntity isometryTag;
    private TrainingTagEntity bodyweightTag;

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
