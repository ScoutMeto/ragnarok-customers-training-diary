package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogAttributeOptionEntity.Kind;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 20a/b: správa rozšiřitelného číselníku atributů katalogu (pohybový vzorec, náčiní).
 * Systémové hodnoty jsou neměnné; vlastní lze přidat a smazat.
 */
@Service
@Transactional(readOnly = true)
public class CatalogAttributeOptionService {

    private final CatalogAttributeOptionRepository repository;

    public CatalogAttributeOptionService(CatalogAttributeOptionRepository repository) {
        this.repository = repository;
    }

    public List<CatalogAttributeOptionEntity> list(Kind kind) {
        return repository.findByKindOrderByIsSystemDescNameAsc(kind);
    }

    /** Přidá vlastní hodnotu (idempotentně — pokud už existuje, vrátí ji). */
    @Transactional
    public CatalogAttributeOptionEntity addCustom(Kind kind, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Název nesmí být prázdný.");
        }
        String trimmed = name.trim();
        return repository.findByKindAndNameIgnoreCase(kind, trimmed)
                .orElseGet(() -> repository.save(
                        new CatalogAttributeOptionEntity(kind, trimmed, false)));
    }

    /** Smaže vlastní hodnotu. Systémovou smazat nelze. */
    @Transactional
    public void delete(Long id) {
        CatalogAttributeOptionEntity opt = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hodnota číselníku (id=" + id + ") nenalezena."));
        if (opt.isSystem()) {
            throw new ForbiddenException("Systémovou hodnotu nelze smazat.");
        }
        repository.delete(opt);
    }
}
