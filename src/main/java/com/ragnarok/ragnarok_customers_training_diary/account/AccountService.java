package com.ragnarok.ragnarok_customers_training_diary.account;

import com.ragnarok.ragnarok_customers_training_diary.account.dto.RegistrationRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logika nad {@link AccountEntity}.
 *
 * <p>Aktuálně podporuje registraci klientů (role USER) — adminy zakládá pouze admin-initializer
 * při startu, později pak admin přes admin sekci.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Vytvoří nový klientský účet (role USER).
     *
     * @throws EmailAlreadyTakenException pokud email už v DB existuje
     */
    @Transactional
    public AccountEntity register(RegistrationRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyTakenException(request.email());
        }

        AccountEntity account = new AccountEntity();
        account.setEmail(request.email());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setNickname(request.nickname());
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setPhone(request.phone());
        account.setRole(AccountRole.USER);

        return accountRepository.save(account);
    }
}
