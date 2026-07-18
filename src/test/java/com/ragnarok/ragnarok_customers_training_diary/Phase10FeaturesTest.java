package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smoke testy pro Phase 10 (E1) — deaktivace účtu (read-only mód).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase10FeaturesTest {

    @Autowired private AccountService accountService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AccountEntity client;

    @BeforeEach
    void seed() {
        client = new AccountEntity();
        client.setEmail("inactive@example.cz");
        client.setPasswordHash(passwordEncoder.encode("password"));
        client.setRole(AccountRole.USER);
        client.setNickname("inactive");
        client.setFirstName("In");
        client.setLastName("Active");
        client.setEmailConfirmed(true);
        client.setApprovedAt(java.time.LocalDateTime.now()); // kolo 9: zrozený viking
        client = accountRepository.save(client);
    }

    @Test
    void newAccount_isActiveByDefault() {
        assertThat(client.isDeactivated()).isFalse();
        assertThat(accountService.isDeactivated(client.getId())).isFalse();
    }

    @Test
    void deactivate_setsReadOnly() {
        accountService.deactivate(client.getId());
        assertThat(accountService.isDeactivated(client.getId())).isTrue();

        AccountEntity reloaded = accountRepository.findById(client.getId()).orElseThrow();
        assertThat(reloaded.isDeactivated()).isTrue();
        assertThat(reloaded.getDeactivatedAt()).isNotNull();
    }

    @Test
    void reactivate_restoresAccess() {
        accountService.deactivate(client.getId());
        accountService.reactivate(client.getId());

        assertThat(accountService.isDeactivated(client.getId())).isFalse();
        AccountEntity reloaded = accountRepository.findById(client.getId()).orElseThrow();
        assertThat(reloaded.getDeactivatedAt()).isNull();
    }

    @Test
    void deactivate_isIdempotent() {
        accountService.deactivate(client.getId());
        var firstTimestamp = accountRepository.findById(client.getId()).orElseThrow().getDeactivatedAt();
        accountService.deactivate(client.getId());
        var secondTimestamp = accountRepository.findById(client.getId()).orElseThrow().getDeactivatedAt();
        // Druhé volání nepřepíše timestamp
        assertThat(secondTimestamp).isEqualTo(firstTimestamp);
    }

    @Test
    void deactivatedAccount_canStillLogin() {
        accountService.deactivate(client.getId());
        AccountEntity reloaded = accountRepository.findById(client.getId()).orElseThrow();
        // Deaktivace neblokuje login (jen read-only) — isEnabled závisí na deletedAt + emailConfirmed
        assertThat(reloaded.isEnabled()).isTrue();
    }
}
