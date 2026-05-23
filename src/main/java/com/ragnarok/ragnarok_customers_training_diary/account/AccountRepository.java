package com.ragnarok.ragnarok_customers_training_diary.account;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    /**
     * Login lookup — vrátí jen aktivní účty (soft-deleted se nesmí přihlásit).
     */
    Optional<AccountEntity> findByEmailAndDeletedAtIsNull(String email);

    /**
     * Bootstrap admin check + obecný lookup — vrací i soft-deleted účty.
     * Pro login použij {@link #findByEmailAndDeletedAtIsNull(String)}.
     */
    Optional<AccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Aktivní účty seřazené podle příjmení (admin sekce list). */
    List<AccountEntity> findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();

    /** Aktivní účty filtrované rolí. */
    List<AccountEntity> findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(AccountRole role);
}
