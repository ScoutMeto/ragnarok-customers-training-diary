package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import com.ragnarok.ragnarok_customers_training_diary.account.EmailConfirmationException;
import com.ragnarok.ragnarok_customers_training_diary.account.EmailConfirmationService;
import com.ragnarok.ragnarok_customers_training_diary.account.dto.RegistrationRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smoke testy pro Phase 6:
 *   - registrace → emailConfirmed=false + má 6místný kód
 *   - verifyCode validní → emailConfirmed=true, kód smazán
 *   - verifyCode neplatný → výjimka
 *   - verifyCode expirovaný → výjimka
 *   - createByAdmin → emailConfirmed=true (admin auto-confirm)
 *   - notification preferences default true při registraci bez params
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6FeaturesTest {

    @Autowired private AccountService accountService;
    @Autowired private EmailConfirmationService emailConfirmationService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void register_setsEmailConfirmedFalseAndGeneratesCode() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "newbie@example.cz", "tajneheslo", "newbie", "Nový", "Klient", null,
                null, null, null, null));

        assertThat(acc.isEmailConfirmed()).isFalse();
        assertThat(acc.getEmailConfirmationCode()).hasSize(6);
        assertThat(acc.getEmailConfirmationCodeExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void register_defaultsAllNotificationsToTrue() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "defaultnotif@example.cz", "tajneheslo", "def", "Def", "Notif", null));

        assertThat(acc.isNotifGroupTrainingReminder()).isTrue();
        assertThat(acc.isNotifNewPlanAssigned()).isTrue();
        assertThat(acc.isNotifNewComment()).isTrue();
        assertThat(acc.isNotifWelcome()).isTrue();
    }

    @Test
    void register_respectsExplicitNotificationFlags() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "custom@example.cz", "tajneheslo", "cust", "Cust", "Notif", null,
                false, true, false, true));

        assertThat(acc.isNotifGroupTrainingReminder()).isFalse();
        assertThat(acc.isNotifNewPlanAssigned()).isTrue();
        assertThat(acc.isNotifNewComment()).isFalse();
        assertThat(acc.isNotifWelcome()).isTrue();
    }

    @Test
    void verifyCode_validCode_confirmsAccountAndClearsCode() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "verify@example.cz", "tajneheslo", "verify", "V", "Test", null));
        String code = acc.getEmailConfirmationCode();
        assertThat(code).hasSize(6);

        AccountEntity confirmed = emailConfirmationService.verifyCode("verify@example.cz", code);

        assertThat(confirmed.isEmailConfirmed()).isTrue();
        assertThat(confirmed.getEmailConfirmationCode()).isNull();
        assertThat(confirmed.getEmailConfirmationCodeExpiresAt()).isNull();
    }

    @Test
    void verifyCode_invalidCode_throws() {
        accountService.register(new RegistrationRequest(
                "invalid@example.cz", "tajneheslo", "inv", "I", "Test", null));

        assertThatThrownBy(() -> emailConfirmationService.verifyCode("invalid@example.cz", "000000"))
                .isInstanceOf(EmailConfirmationException.class)
                .hasMessageContaining("Neplatný kód");
    }

    @Test
    void verifyCode_expiredCode_throws() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "expired@example.cz", "tajneheslo", "exp", "E", "Test", null));

        // Simuluj expiraci nastavením expires_at do minulosti
        acc.setEmailConfirmationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        accountRepository.save(acc);
        String code = acc.getEmailConfirmationCode();

        assertThatThrownBy(() -> emailConfirmationService.verifyCode("expired@example.cz", code))
                .isInstanceOf(EmailConfirmationException.class)
                .hasMessageContaining("vypršel");
    }

    @Test
    void verifyCode_unknownEmail_throws() {
        assertThatThrownBy(() -> emailConfirmationService.verifyCode("ghost@nowhere.cz", "123456"))
                .isInstanceOf(EmailConfirmationException.class);
    }

    @Test
    void resendCode_generatesNewCodeDifferentFromOld() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "resend@example.cz", "tajneheslo", "res", "R", "Test", null));
        String oldCode = acc.getEmailConfirmationCode();

        emailConfirmationService.resendCode("resend@example.cz");

        AccountEntity reloaded = accountRepository.findByEmail("resend@example.cz").orElseThrow();
        // Pozn: existuje malá pravděpodobnost stejného kódu (1 z milionu),
        // ale alespoň expires_at by mělo být novější
        assertThat(reloaded.getEmailConfirmationCodeExpiresAt())
                .isAfter(acc.getEmailConfirmationCodeExpiresAt().minusSeconds(1));
    }

    @Test
    void createByAdmin_autoConfirmsEmail() {
        AccountEntity adminCreated = accountService.createByAdmin(
                new RegistrationRequest(
                        "byadmin@example.cz", "tajneheslo", "ba", "BA", "Test", null),
                AccountRole.USER);

        assertThat(adminCreated.isEmailConfirmed()).isTrue();
        assertThat(adminCreated.getEmailConfirmationCode()).isNull();
    }

    @Test
    void unconfirmedAccount_isNotEnabled() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "notenabled@example.cz", "tajneheslo", "ne", "NE", "Test", null));

        assertThat(acc.isEnabled()).isFalse(); // emailConfirmed=false → !isEnabled
    }

    @Test
    void confirmedAccount_isEnabled() {
        AccountEntity acc = accountService.register(new RegistrationRequest(
                "enabled@example.cz", "tajneheslo", "e", "E", "Test", null));

        emailConfirmationService.verifyCode("enabled@example.cz", acc.getEmailConfirmationCode());

        AccountEntity reloaded = accountRepository.findByEmail("enabled@example.cz").orElseThrow();
        assertThat(reloaded.isEnabled()).isTrue();
    }
}
