package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogService;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smoke testy pro Phase 17: per-user katalog cviků (copy-on-write pattern).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase17FeaturesTest {

    @Autowired private ExerciseCatalogService catalogService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity alice;
    private AccountEntity bob;
    private AccountEntity admin;

    @BeforeEach
    void seed() {
        alice = createUser("alice17@example.cz", AccountRole.USER);
        bob = createUser("bob17@example.cz", AccountRole.USER);
        admin = createUser("admin17@example.cz", AccountRole.ADMIN);
    }

    @Test
    void clientCreate_makesCustomVisibleOnlyToOwner() {
        ExerciseCatalogItemEntity data = newItem("Aliciny KB swing");
        catalogService.create(alice, data);

        // Alice vidí svůj custom
        assertThat(catalogService.listVisibleTo(alice))
                .anyMatch(i -> i.getName().equals("Aliciny KB swing") && !i.isSystem());
        // Bob ho nevidí
        assertThat(catalogService.listVisibleTo(bob))
                .noneMatch(i -> i.getName().equals("Aliciny KB swing"));
    }

    @Test
    void adminCreate_makesSystemVisibleToAll() {
        ExerciseCatalogItemEntity data = newItem("Oficiální cvik");
        catalogService.create(admin, data);

        assertThat(catalogService.listVisibleTo(alice))
                .anyMatch(i -> i.getName().equals("Oficiální cvik") && i.isSystem());
        assertThat(catalogService.listVisibleTo(bob))
                .anyMatch(i -> i.getName().equals("Oficiální cvik") && i.isSystem());
    }

    @Test
    void clientCannotEditOthersCustom() {
        ExerciseCatalogItemEntity created = catalogService.create(alice, newItem("Aliciny cvik"));

        assertThatThrownBy(() -> catalogService.update(bob, created.getId(), newItem("Hacknutý")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void clientEditSystemExerciseCreatesOwnCustomCopy() {
        ExerciseCatalogItemEntity systemItem = catalogService.create(admin, newItem("Systémový"));
        ExerciseCatalogItemEntity data = newItem("Systémový upravený");
        data.getMovementPatterns().add("PUSH");
        data.getMovementPatterns().add("PLYO");

        ExerciseCatalogItemEntity copy = catalogService.update(alice, systemItem.getId(), data);

        assertThat(copy.getId()).isNotEqualTo(systemItem.getId());
        assertThat(copy.isSystem()).isFalse();
        assertThat(copy.getCreatedBy().getId()).isEqualTo(alice.getId());
        assertThat(copy.getName()).isEqualTo("Systémový upravený");
        assertThat(copy.getMovementPatterns()).containsExactly("PUSH", "PLYO");

        ExerciseCatalogItemEntity original = catalogService.getById(systemItem.getId());
        assertThat(original.isSystem()).isTrue();
        assertThat(original.getName()).isEqualTo("Systémový");

        assertThat(catalogService.listVisibleTo(alice))
                .anyMatch(i -> i.getId().equals(copy.getId()) && !i.isSystem());
        assertThat(catalogService.listVisibleTo(bob))
                .noneMatch(i -> i.getId().equals(copy.getId()));
    }

    @Test
    void clientCanEditOwnCustom() {
        ExerciseCatalogItemEntity created = catalogService.create(alice, newItem("Původní"));
        ExerciseCatalogItemEntity data = newItem("Upravený");
        data.setPrimaryMuscle("hamstringy");

        ExerciseCatalogItemEntity updated = catalogService.update(alice, created.getId(), data);
        assertThat(updated.getName()).isEqualTo("Upravený");
        assertThat(updated.getPrimaryMuscle()).isEqualTo("hamstringy");
    }

    @Test
    void delete_deactivatesAndHidesFromList() {
        ExerciseCatalogItemEntity created = catalogService.create(alice, newItem("Smazat mě"));
        catalogService.delete(alice, created.getId());

        assertThat(catalogService.listVisibleTo(alice))
                .noneMatch(i -> i.getName().equals("Smazat mě"));
    }

    @Test
    void adminCanEditSystemExercise() {
        ExerciseCatalogItemEntity systemItem = catalogService.create(admin, newItem("Sys"));
        ExerciseCatalogItemEntity data = newItem("Sys upraveno");

        ExerciseCatalogItemEntity updated = catalogService.update(admin, systemItem.getId(), data);
        assertThat(updated.getName()).isEqualTo("Sys upraveno");
    }

    // helpers
    private AccountEntity createUser(String email, AccountRole role) {
        AccountEntity a = new AccountEntity();
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode("password"));
        a.setRole(role);
        a.setNickname(email.substring(0, 4));
        a.setFirstName("Test");
        a.setLastName("User");
        a.setEmailConfirmed(true);
        return accountRepository.save(a);
    }

    private ExerciseCatalogItemEntity newItem(String name) {
        ExerciseCatalogItemEntity i = new ExerciseCatalogItemEntity();
        i.setName(name);
        return i;
    }
}
