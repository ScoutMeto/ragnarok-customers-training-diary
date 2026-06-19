package com.ragnarok.ragnarok_customers_training_diary.account;

import com.ragnarok.ragnarok_customers_training_diary.mail.EmailService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reset zapomenutého hesla přes emailový kód (ScoutMeto kolo 6).
 *  <ul>
 *    <li>{@link #startReset(String)} — pro existující účet vyrobí 6místný kód, uloží ho
 *        s expirací a pošle mail. Pro neexistující email mlčí (neúnikáme existenci účtů).</li>
 *    <li>{@link #resetPassword(String, String, String)} — ověří kód + expiraci a nastaví
 *        nové heslo (BCrypt), kód zruší.</li>
 *  </ul>
 *
 * Kódy mají TTL (sdílí konfiguraci s email confirmation). Single-tenant gym → bez rate-limitu.
 */
@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${ragnarok.email-confirmation.code-ttl-minutes:60}")
    private long codeTtlMinutes;

    public PasswordResetService(AccountRepository accountRepository, EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spustí reset pro daný email. Pokud aktivní účet existuje, pošle kód; jinak nic
     * (caller vždy zobrazí stejnou neutrální hlášku, aby neúnikal existenci účtů).
     */
    public void startReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        accountRepository.findByEmailAndDeletedAtIsNull(email.trim()).ifPresentOrElse(account -> {
            String code = generate6DigitCode();
            LocalDateTime now = LocalDateTime.now();
            account.setPasswordResetCode(code);
            account.setPasswordResetExpiresAt(now.plus(Duration.ofMinutes(codeTtlMinutes)));
            accountRepository.save(account);
            emailService.sendPasswordResetCode(account, code);
            log.info("Password reset kod vyrobeny pro account={}", account.getEmail());
        }, () -> log.info("Password reset pozadovan pro neexistujici email={} - ignoruji", email));
    }

    /**
     * Ověří kód a nastaví nové heslo. Po úspěchu kód zruší.
     *
     * @throws EmailConfirmationException pokud účet/kód neexistuje, vypršel nebo nesedí
     */
    public void resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new EmailConfirmationException("Heslo musí mít aspoň 8 znaků.");
        }
        AccountEntity account = accountRepository.findByEmailAndDeletedAtIsNull(email == null ? "" : email.trim())
                .orElseThrow(() -> new EmailConfirmationException("Neplatný email nebo kód."));

        if (account.getPasswordResetCode() == null) {
            throw new EmailConfirmationException("Pro tento email není aktivní žádný kód. Nech si poslat nový.");
        }
        if (account.getPasswordResetExpiresAt() != null
                && LocalDateTime.now().isAfter(account.getPasswordResetExpiresAt())) {
            throw new EmailConfirmationException("Kód vypršel. Nech si poslat nový.");
        }
        if (!account.getPasswordResetCode().equals(normalizeCode(code))) {
            throw new EmailConfirmationException("Neplatný kód. Zkontroluj 6 číslic z mailu.");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setPasswordResetCode(null);
        account.setPasswordResetExpiresAt(null);
        accountRepository.save(account);
        log.info("Password reset proveden pro account={}", account.getEmail());
    }

    private static String generate6DigitCode() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().replaceAll("\\s+", "");
    }
}
