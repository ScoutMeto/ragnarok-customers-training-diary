package com.ragnarok.ragnarok_customers_training_diary.account;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security adaptér nad {@link AccountRepository}. Načítá {@link AccountEntity}
 * podle emailu (=username). Role je odvozená z {@code AccountEntity.role}.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Pouze aktivní (deleted_at IS NULL) účty se mohou přihlásit.
        return accountRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + email));
    }
}
