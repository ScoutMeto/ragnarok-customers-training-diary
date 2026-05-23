package com.ragnarok.ragnarok_customers_training_diary.configuration;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Při startu zajistí, že existuje aspoň jeden admin účet (trenér). Pokud admin
 * s nakonfigurovaným emailem v DB chybí, vytvoří ho. Pokud existuje, nedělá nic.
 *
 * <p>Konfigurace přes env proměnné — viz {@code application.properties}:
 * {@code app.bootstrap.admin.email}, {@code .password}, {@code .first-name}, {@code .last-name}.
 *
 * <p><b>Bezpečnostní upozornění:</b> defaultní heslo {@code heslo123} je jen pro lokální dev.
 * Na Railway nastav silnější heslo přes env var {@code BOOTSTRAP_ADMIN_PASSWORD}.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:admin@admin.cz}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:heslo123}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.first-name:Admin}")
    private String firstName;

    @Value("${app.bootstrap.admin.last-name:Admin}")
    private String lastName;

    @Value("${app.bootstrap.admin.nickname:admin}")
    private String nickname;

    public AdminInitializer(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.findByEmail(adminEmail).isPresent()) {
            log.debug("Bootstrap admin {} už existuje, přeskakuji.", adminEmail);
            return;
        }

        AccountEntity admin = new AccountEntity();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(AccountRole.ADMIN);
        admin.setNickname(nickname);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        accountRepository.save(admin);

        log.info("Bootstrap admin {} vytvořen.", adminEmail);
    }
}
