package com.ragnarok.ragnarok_customers_training_diary.account;

import com.ragnarok.ragnarok_customers_training_diary.account.dto.AdminAccountUpdateRequest;
import com.ragnarok.ragnarok_customers_training_diary.account.dto.RegistrationRequest;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logika nad {@link AccountEntity}.
 *
 * <p>Registrace klientů (role USER), admin operace nad účty (CRUD + soft delete).
 * Bootstrap admin se zakládá v {@link
 * com.ragnarok.ragnarok_customers_training_diary.configuration.AdminInitializer}.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -----------------------------------------------------------------------------
    // Self-registrace (anonymous → USER)
    // -----------------------------------------------------------------------------

    /**
     * Vytvoří nový klientský účet (role USER).
     *
     * @throws EmailAlreadyTakenException pokud email už v DB existuje (i pokud je soft-deleted)
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

    // -----------------------------------------------------------------------------
    // Admin operace
    // -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AccountEntity> listActiveAccounts() {
        return accountRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public AccountEntity getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Účet (id=" + id + ") nenalezen."));
    }

    /** Admin vytvoří nový účet (klient nebo další admin). */
    @Transactional
    public AccountEntity createByAdmin(RegistrationRequest request, AccountRole role) {
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
        account.setRole(role);

        return accountRepository.save(account);
    }

    @Transactional
    public AccountEntity updateByAdmin(Long id, AdminAccountUpdateRequest request) {
        AccountEntity account = getById(id);

        account.setNickname(request.nickname());
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setPhone(request.phone());
        if (request.role() != null) {
            account.setRole(request.role());
        }
        // Heslo měníme jen pokud admin vyplnil nové
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        return account; // dirty checking, autoflush
    }

    /**
     * Soft delete — anonymizuje účet a nastaví {@code deleted_at}. Účet se nesmaže
     * fyzicky, aby zůstaly historické tréninky a komentáře platné. Po soft delete
     * se nelze přihlásit (viz {@link AccountEntity#isEnabled()}).
     */
    @Transactional
    public void softDelete(Long id) {
        AccountEntity account = getById(id);
        if (account.getDeletedAt() != null) {
            return; // už smazaný
        }

        // Anonymizace — uvolní email pro pozdější re-registraci
        String anonymizedEmail = "deleted-" + account.getId() + "-" + UUID.randomUUID() + "@deleted.local";
        account.setEmail(anonymizedEmail);
        account.setNickname("Smazaný účet");
        account.setFirstName("Smazaný");
        account.setLastName("Účet");
        account.setPhone(null);
        account.setDeletedAt(LocalDateTime.now());
    }
}
