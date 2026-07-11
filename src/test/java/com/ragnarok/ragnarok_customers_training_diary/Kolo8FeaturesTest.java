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
import com.ragnarok.ragnarok_customers_training_diary.training.dto.CircuitConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.KbSportConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.NumericSeriesConfigInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScoutMeto kolo 8: přepracované formuláře typů cviků — perzistence nových polí.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Kolo8FeaturesTest {

    @Autowired private TrainingService trainingService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TrainingTagRepository tagRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("kolo8@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("k8");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void freeformSet_persistsRestSecondsAndUnit() {
        TrainingInput input = base(TrainingExerciseType.FREEFORM);
        var ex = input.getExercises().get(0);
        ex.setSetUnit("METERS"); // Carry: metry místo opakování
        SetInput s = new SetInput();
        s.setWeightKg(new BigDecimal("24"));
        s.setReps(40);
        s.setRestSeconds(90);
        ex.getSets().add(s);

        TrainingEntity created = trainingService.create(alice, input);
        var savedEx = created.getExercises().get(0);
        assertThat(savedEx.getSetUnit()).isEqualTo("METERS");
        assertThat(savedEx.getSets().get(0).getRestSeconds()).isEqualTo(90);
    }

    @Test
    void setUnitReps_normalizedToNull() {
        TrainingInput input = base(TrainingExerciseType.FREEFORM);
        input.getExercises().get(0).setSetUnit("REPS");
        SetInput s = new SetInput();
        s.setReps(10);
        input.getExercises().get(0).getSets().add(s);

        TrainingEntity created = trainingService.create(alice, input);
        assertThat(created.getExercises().get(0).getSetUnit()).isNull();
    }

    @Test
    void kbSport_partsWithDurationAndBilateral() {
        TrainingInput input = base(TrainingExerciseType.KB_SPORT_TIME);
        var kb = new KbSportConfigInput();
        kb.setTotalMinutes(10);
        kb.setTotalReps(80);
        kb.setBilateralDetail(true); // bez L/P
        var p1 = new KbSportConfigInput.IntervalInput();
        p1.setIntervalIndex(0); p1.setReps(40); p1.setDurationMinutes(5); p1.setDurationSeconds(0);
        var p2 = new KbSportConfigInput.IntervalInput();
        p2.setIntervalIndex(1); p2.setReps(40); p2.setDurationMinutes(4); p2.setDurationSeconds(30);
        kb.getIntervals().add(p1);
        kb.getIntervals().add(p2);
        input.getExercises().get(0).setKbSport(kb);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getKbSportConfig();
        assertThat(cfg.isUnilateral()).isFalse();
        assertThat(cfg.getIntervals()).hasSize(2);
        assertThat(cfg.getIntervals().get(0).getDurationSeconds()).isEqualTo(300);
        assertThat(cfg.getIntervals().get(1).getDurationSeconds()).isEqualTo(270);
        assertThat(cfg.getIntervals().get(1).getSide()).isNull();
    }

    @Test
    void circuitStep_persistsEquipmentAndTags() {
        TrainingTagEntity tag = new TrainingTagEntity();
        tag.setName("Kolo8 Fokus");
        tag.setSystem(true);
        tag = tagRepository.save(tag);

        TrainingInput input = base(TrainingExerciseType.CIRCUIT);
        var circuit = new CircuitConfigInput();
        circuit.setRounds(3);
        var step = new CircuitConfigInput.StepInput();
        step.setName("KB Swing");
        step.setReps(15);
        step.setRestSeconds(30);
        step.setEquipmentName("kettlebell");
        step.setEquipmentWeightKg(new BigDecimal("24"));
        step.setEquipmentCount(2);
        step.setEquipmentSecondWeightKg(new BigDecimal("16"));
        step.getTagIds().add(tag.getId());
        circuit.getSteps().add(step);
        input.getExercises().get(0).setCircuit(circuit);

        TrainingEntity created = trainingService.create(alice, input);
        var savedStep = created.getExercises().get(0).getCircuitConfig().getSteps().get(0);
        assertThat(savedStep.getEquipmentName()).isEqualTo("kettlebell");
        assertThat(savedStep.getEquipmentCount()).isEqualTo(2);
        assertThat(savedStep.getEquipmentSecondWeightKg()).isEqualByComparingTo("16");
        assertThat(savedStep.getTags()).extracting(TrainingTagEntity::getName).containsExactly("Kolo8 Fokus");
    }

    @Test
    void numericSeries_persistsGeneratedRows_andEditRoundtrip() {
        TrainingInput input = base(TrainingExerciseType.LADDER);
        var series = new NumericSeriesConfigInput();
        series.setStartValue(1);
        series.setPeakValue(3);
        series.setStepSize(1);
        // ladder 1-3: 1. série 1 řádek, 2. série 2 řádky, 3. série 3 řádky
        int idx = 0;
        for (int rung = 1; rung <= 3; rung++) {
            for (int r = 0; r < rung; r++) {
                var row = new NumericSeriesConfigInput.RowInput();
                row.setRowIndex(idx++);
                row.setRung(rung);
                row.setReps(rung);
                row.setWeightKg(new BigDecimal("12"));
                series.getRows().add(row);
            }
        }
        input.getExercises().get(0).setNumericSeries(series);

        TrainingEntity created = trainingService.create(alice, input);
        var cfg = created.getExercises().get(0).getNumericSeriesConfig();
        assertThat(cfg.getRows()).hasSize(6);
        assertThat(cfg.getRows().get(5).getRung()).isEqualTo(3);
        assertThat(cfg.getRows().get(5).getReps()).isEqualTo(3);
        assertThat(cfg.getRows().get(0).getWeightKg()).isEqualByComparingTo("12");

        // edit roundtrip: uživatel upraví jeden řádek
        TrainingInput edit = base(TrainingExerciseType.LADDER);
        var series2 = new NumericSeriesConfigInput();
        series2.setStartValue(1);
        series2.setPeakValue(3);
        series2.setStepSize(1);
        var row = new NumericSeriesConfigInput.RowInput();
        row.setRowIndex(0); row.setRung(1); row.setReps(2); // edit: 1 → 2
        series2.getRows().add(row);
        edit.getExercises().get(0).setNumericSeries(series2);
        TrainingEntity updated = trainingService.update(alice, created.getId(), edit);
        assertThat(updated.getExercises().get(0).getNumericSeriesConfig().getRows()).hasSize(1);
        assertThat(updated.getExercises().get(0).getNumericSeriesConfig().getRows().get(0).getReps()).isEqualTo(2);
    }

    @Test
    void coreType_noLongerExists() {
        assertThat(TrainingExerciseType.values())
                .extracting(Enum::name)
                .doesNotContain("CORE");
    }

    private TrainingInput base(TrainingExerciseType type) {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Kolo 8 test");
        var ex = new TrainingExerciseInput();
        ex.setType(type);
        ex.setCustomName("Testovací cvik");
        input.getExercises().add(ex);
        return input;
    }
}
