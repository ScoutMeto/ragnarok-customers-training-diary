package com.ragnarok.ragnarok_customers_training_diary.equipment;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 11 (A14): správa pomůcek. Klient vidí systémové + svoje custom.
 * Když ve formuláři zadá nový název, automaticky se uloží jako jeho custom
 * pomůcka (pro příště). Custom může smazat.
 */
@Service
@Transactional(readOnly = true)
public class EquipmentOptionService {

    private final EquipmentOptionRepository repository;

    public EquipmentOptionService(EquipmentOptionRepository repository) {
        this.repository = repository;
    }

    public List<EquipmentOptionEntity> listVisibleTo(AccountEntity user) {
        return repository.findVisibleTo(user.getId());
    }

    /**
     * Zajistí, že pomůcka daného názvu existuje pro uživatele. Pokud je to nový
     * název (není system ani jeho custom), uloží ji jako jeho custom. No-op pro
     * prázdný název nebo už existující.
     */
    @Transactional
    public void ensureExistsForUser(AccountEntity user, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        if (repository.findSystemByName(trimmed).isPresent()) {
            return; // systémová — netřeba ukládat
        }
        if (repository.findOwnByName(user.getId(), trimmed).isPresent()) {
            return; // už ji má
        }
        EquipmentOptionEntity opt = new EquipmentOptionEntity();
        opt.setName(trimmed);
        opt.setSystem(false);
        opt.setOwner(user);
        repository.save(opt);
    }

    /** Smaže uživatelovu custom pomůcku. Systémové smazat nelze. */
    @Transactional
    public void deleteOwn(AccountEntity user, Long id) {
        EquipmentOptionEntity opt = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pomůcka (id=" + id + ") nenalezena."));
        if (opt.isSystem() || opt.getOwner() == null || !opt.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Tuto pomůcku nemůžeš smazat.");
        }
        repository.delete(opt);
    }
}
