package com.ragnarok.ragnarok_customers_training_diary.account;

import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.mail.EmailService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Správa registrace přes email confirmation:
 *  <ul>
 *    <li>{@link #startConfirmation(AccountEntity)} — vygeneruje 6místný kód, uloží ho,
 *        nastaví expirační čas a pošle mail.</li>
 *    <li>{@link #verifyCode(String, String)} — validuje kód, při úspěchu nastaví
 *        {@code email_confirmed=true} a pošle welcome mail.</li>
 *    <li>{@link #resendCode(String)} — vygeneruje nový kód (zruší starý) a pošle.</li>
 *  </ul>
 *
 * Anti-bot: kódy mají TTL (default 60 minut), brute-force ochrana není potřeba na
 * této úrovni (single-tenant, ~N klientů), v případě potřeby se přidá rate-limit.
 */
@Service
@Transactional
public class EmailConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(EmailConfirmationService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final AccountRepository accountRepository;
    private final EmailService emailService;

    @Value("${ragnarok.email-confirmation.code-ttl-minutes:60}")
    private long codeTtlMinutes;

    public EmailConfirmationService(AccountRepository accountRepository, EmailService emailService) {
        this.accountRepository = accountRepository;
        this.emailService = emailService;
    }

    /** Vyrobí kód pro {@code account}, uloží ho a odešle email. */
    public void startConfirmation(AccountEntity account) {
        String code = generate6DigitCode();
        LocalDateTime now = LocalDateTime.now();
        account.setEmailConfirmationCode(code);
        account.setEmailConfirmationCodeSentAt(now);
        account.setEmailConfirmationCodeExpiresAt(now.plus(Duration.ofMinutes(codeTtlMinutes)));
        account.setEmailConfirmed(false);
        accountRepository.save(account);
        emailService.sendConfirmationCode(account, code);
        log.info("Email confirmation kod vyrobeny pro account={}", account.getEmail());
    }

    /**
     * Validuje kód pro daný email. Při úspěchu nastaví {@code emailConfirmed=true},
     * smaže kód a pošle welcome email.
     *
     * @return potvrzený {@link AccountEntity}
     * @throws EmailConfirmationException pokud kód neexistuje, vypršel nebo nesedí
     */
    public AccountEntity verifyCode(String email, String code) {
        AccountEntity account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new EmailConfirmationException("Účet s tímto emailem neexistuje."));

        if (account.isEmailConfirmed()) {
            // Already confirmed - idempotent OK
            return account;
        }

        if (account.getEmailConfirmationCode() == null) {
            throw new EmailConfirmationException("Pro tento email není aktivní žádný potvrzovací kód. Klikni na 'Poslat nový kód'.");
        }
        if (account.getEmailConfirmationCodeExpiresAt() != null
                && LocalDateTime.now().isAfter(account.getEmailConfirmationCodeExpiresAt())) {
            throw new EmailConfirmationException("Kód vypršel. Klikni na 'Poslat nový kód'.");
        }
        if (!account.getEmailConfirmationCode().equals(normalizeCode(code))) {
            throw new EmailConfirmationException("Neplatný kód. Zkontroluj 6 číslic z mailu.");
        }

        account.setEmailConfirmed(true);
        account.setEmailConfirmationCode(null);
        account.setEmailConfirmationCodeSentAt(null);
        account.setEmailConfirmationCodeExpiresAt(null);
        accountRepository.save(account);

        emailService.sendWelcomeEmail(account);
        log.info("Email confirmed for account={}", email);
        return account;
    }

    /**
     * Pošle nový kód na daný email (předpoklad: účet existuje a není potvrzený).
     */
    public void resendCode(String email) {
        AccountEntity account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Účet s emailem " + email + " neexistuje."));
        if (account.isEmailConfirmed()) {
            log.info("Resend code volano na uz potvrzeny ucet={} - skip", email);
            return;
        }
        startConfirmation(account);
    }

    // ---------------------------------------------------------------------------------------------

    private static String generate6DigitCode() {
        // 6 number digits, possibly with leading zeros
        int n = RNG.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private static String normalizeCode(String code) {
        if (code == null) return null;
        return code.trim().replaceAll("\\s+", "");
    }
}
