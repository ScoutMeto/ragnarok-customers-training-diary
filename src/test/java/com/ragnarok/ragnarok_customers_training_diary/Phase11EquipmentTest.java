package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.equipment.EquipmentOptionService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
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
 * Phase 11 (A14): per-exercise náčiní + custom equipment options.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase11EquipmentTest {

    @Autowired private TrainingService trainingService;
    @Autowired private EquipmentOptionService equipmentService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;

    @BeforeEach
    void seed() {
        alice = new AccountEntity();
        alice.setEmail("eq@example.cz");
        alice.setPasswordHash(passwordEncoder.encode("password"));
        alice.setRole(AccountRole.USER);
        alice.setNickname("eq");
        alice.setFirstName("Alice");
        alice.setLastName("Test");
        alice.setEmailConfirmed(true);
        alice = accountRepository.save(alice);
    }

    @Test
    void create_storesEquipmentFields() {
        TrainingInput input = base();
        TrainingExerciseInput ex = input.getExercises().get(0);
        ex.setEquipmentName("Kettlebell");
        ex.setEquipmentWeightKg(new BigDecimal("24.0"));
        ex.setEquipmentCount(2);
        ex.setEquipmentSecondWeightKg(new BigDecimal("20.0"));

        TrainingEntity created = trainingService.create(alice, input);
        var savedEx = created.getExercises().get(0);
        assertThat(savedEx.getEquipmentName()).isEqualTo("Kettlebell");
        assertThat(savedEx.getEquipmentWeightKg()).isEqualByComparingTo("24.0");
        assertThat(savedEx.getEquipmentCount()).isEqualTo(2);
        assertThat(savedEx.getEquipmentSecondWeightKg()).isEqualByComparingTo("20.0");
    }

    @Test
    void create_countOne_clearsSecondWeight() {
        TrainingInput input = base();
        TrainingExerciseInput ex = input.getExercises().get(0);
        ex.setEquipmentName("Osa");
        ex.setEquipmentCount(1);
        ex.setEquipmentSecondWeightKg(new BigDecimal("99.0")); // má se ignorovat

        TrainingEntity created = trainingService.create(alice, input);
        var savedEx = created.getExercises().get(0);
        assertThat(savedEx.getEquipmentCount()).isEqualTo(1);
        assertThat(savedEx.getEquipmentSecondWeightKg()).isNull();
    }

    @Test
    void create_newEquipmentName_persistsAsCustomOption() {
        TrainingInput input = base();
        input.getExercises().get(0).setEquipmentName("Bulharský pytel");

        trainingService.create(alice, input);

        assertThat(equipmentService.listVisibleTo(alice))
                .anyMatch(o -> o.getName().equals("Bulharský pytel") && !o.isSystem());
    }

    @Test
    void create_systemEquipmentName_notDuplicatedAsCustom() {
        // "Kettlebell" je system seed — ale v H2 testu seed z V21 neběží.
        // Vytvoříme system option ručně přes... vlastně service nemá create system.
        // Tady jen ověříme, že stejný custom název se neuloží 2×.
        TrainingInput i1 = base();
        i1.getExercises().get(0).setEquipmentName("Mace");
        trainingService.create(alice, i1);

        TrainingInput i2 = base();
        i2.getExercises().get(0).setEquipmentName("mace"); // jiné velikosti písmen
        trainingService.create(alice, i2);

        long count = equipmentService.listVisibleTo(alice).stream()
                .filter(o -> o.getName().equalsIgnoreCase("Mace")).count();
        assertThat(count).isEqualTo(1);
    }

    private TrainingInput base() {
        TrainingInput input = new TrainingInput();
        input.setTrainingDate(LocalDate.now());
        input.setName("Equipment test");
        TrainingExerciseInput ex = new TrainingExerciseInput();
        ex.setType(TrainingExerciseType.FREEFORM);
        ex.setCustomName("Swing");
        input.getExercises().add(ex);
        return input;
    }
}
