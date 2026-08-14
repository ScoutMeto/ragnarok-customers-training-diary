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
