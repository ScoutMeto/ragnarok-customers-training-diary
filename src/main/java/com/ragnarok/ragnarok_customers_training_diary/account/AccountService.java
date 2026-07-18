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
    private final EmailConfirmationService emailConfirmationService;

    public AccountService(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          EmailConfirmationService emailConfirmationService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailConfirmationService = emailConfirmationService;
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
        // Phase 6: nově registrovaný účet je nepotvrzený — login musí čekat na kód z mailu
        account.setEmailConfirmed(false);
        // Default notification preferences (může overridem nastavit z formu)
        if (request.notifGroupTrainingReminder() != null) {
            account.setNotifGroupTrainingReminder(request.notifGroupTrainingReminder());
        }
        if (request.notifNewPlanAssigned() != null) {
            account.setNotifNewPlanAssigned(request.notifNewPlanAssigned());
        }
        if (request.notifNewComment() != null) {
            account.setNotifNewComment(request.notifNewComment());
        }
        if (request.notifWelcome() != null) {
            account.setNotifWelcome(request.notifWelcome());
        }

        AccountEntity saved = accountRepository.save(account);
        // Spustí confirmation flow — pošle 6místný kód
        emailConfirmationService.startConfirmation(saved);
        return saved;
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

    /** Uloží změny účtu (ScoutMeto kolo 7: členství apod.). */
    public AccountEntity save(AccountEntity account) {
        return accountRepository.save(account);
    }

    /** Admin vytvoří nový účet (klient nebo další admin). Automaticky potvrzený. */
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
        // Admin-vytvořené účty obcházejí email confirmation flow i „Zrodit vikinga"
        account.setEmailConfirmed(true);
        account.setApprovedAt(LocalDateTime.now());

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
     * Phase 10: deaktivace účtu (read-only mód). Klient se může přihlásit a prohlížet,
     * ale nesmí přidávat/upravovat tréninky. Idempotentní.
     */
    @Transactional
    public void deactivate(Long id) {
        AccountEntity account = getById(id);
        if (account.getDeactivatedAt() == null) {
            account.setDeactivatedAt(LocalDateTime.now());
        }
    }

    /** Phase 10: reaktivace účtu — zpřístupní opět všechny funkce. Idempotentní. */
    @Transactional
    public void reactivate(Long id) {
        AccountEntity account = getById(id);
        account.setDeactivatedAt(null);
    }

    /** {@code true} pokud je účet (dle čerstvého stavu v DB) deaktivovaný. */
    @Transactional(readOnly = true)
    public boolean isDeactivated(Long id) {
        return getById(id).isDeactivated();
    }

    /**
     * ScoutMeto kolo 9: „Poslat do záhrobí" (dřív soft delete s anonymizací).
     * Nastaví jen {@code deleted_at} — data zůstávají nedotčená, takže účet lze
     * kdykoli plně obnovit přes {@link #restore}. Uživatel neprojde loginem.
     */
    @Transactional
    public void softDelete(Long id) {
        AccountEntity account = getById(id);
        if (account.getDeletedAt() != null) {
            return; // už v záhrobí
        }
        account.setDeletedAt(LocalDateTime.now());
    }

    /** ScoutMeto kolo 9: „Probrat ze záhrobí" — plná obnova účtu beze změn. Idempotentní. */
    @Transactional
    public void restore(Long id) {
        AccountEntity account = getById(id);
        account.setDeletedAt(null);
    }

    /**
     * ScoutMeto kolo 9: „Zrodit vikinga" — první aktivace nového účtu adminem.
     * Do té doby se uživatel nepřihlásí. Idempotentní.
     */
    @Transactional
    public void approve(Long id) {
        AccountEntity account = getById(id);
        if (account.getApprovedAt() == null) {
            account.setApprovedAt(LocalDateTime.now());
        }
    }

    /** Všechny účty včetně záhrobí (admin přehled — záhrobní svítí červeně). */
    @Transactional(readOnly = true)
    public List<AccountEntity> listAllAccounts() {
        return accountRepository.findAllByOrderByLastNameAscFirstNameAsc();
    }
}
