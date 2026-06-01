package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katalog cviků. Phase 17: per-user model — klient vidí systémové cviky (seed +
 * admin) plus svoje vlastní. CRUD:
 *  <ul>
 *    <li>Admin vytvoří/upraví → systémový cvik (vidí všichni).</li>
 *    <li>Klient vytvoří → custom cvik (vidí jen on).</li>
 *    <li>Klient může upravit/smazat jen svoje custom cviky (ne systémové).</li>
 *  </ul>
 */
@Service
@Transactional(readOnly = true)
public class ExerciseCatalogService {

    private static final int SEARCH_LIMIT = 50;

    private final ExerciseCatalogItemRepository repository;

    public ExerciseCatalogService(ExerciseCatalogItemRepository repository) {
        this.repository = repository;
    }

    /** Všechny aktivní cviky (system + všech klientů). Pozor: pro formuláře použij {@link #listVisibleTo}. */
    public List<ExerciseCatalogItemEntity> listAll() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    /** Phase 17: cviky viditelné pro uživatele = systémové + jeho vlastní custom. */
    public List<ExerciseCatalogItemEntity> listVisibleTo(AccountEntity user) {
        return repository.findVisibleTo(user.getId());
    }

    public List<ExerciseCatalogItemEntity> search(String query) {
        if (query == null || query.isBlank()) {
            return listAll();
        }
        Pageable limit = PageRequest.of(0, SEARCH_LIMIT);
        return repository.searchByName(query.trim(), limit);
    }

    public ExerciseCatalogItemEntity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cvik z katalogu (id=" + id + ") nenalezen."));
    }

    /**
     * Cvik dostupný uživateli (pro detail/edit). Klient smí vidět systémové +
     * svoje; admin vidí vše.
     */
    public ExerciseCatalogItemEntity getVisible(AccountEntity user, Long id) {
        ExerciseCatalogItemEntity item = getById(id);
        boolean admin = user.getRole() == AccountRole.ADMIN;
        boolean ownCustom = item.getCreatedBy() != null && item.getCreatedBy().getId().equals(user.getId());
        if (!admin && !item.isSystem() && !ownCustom) {
            throw new NotFoundException("Cvik z katalogu (id=" + id + ") nenalezen.");
        }
        return item;
    }

    // -----------------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------------

    /**
     * Vytvoří nový cvik. Admin → systémový (vidí všichni), klient → custom (jen on).
     */
    @Transactional
    public ExerciseCatalogItemEntity create(AccountEntity creator, ExerciseCatalogItemEntity data) {
        boolean admin = creator.getRole() == AccountRole.ADMIN;
        ExerciseCatalogItemEntity item = new ExerciseCatalogItemEntity();
        copyEditableFields(data, item);
        item.setSystem(admin);             // admin → system, klient → custom
        item.setCreatedBy(creator);
        item.setActive(true);
        return repository.save(item);
    }

    /**
     * Upraví cvik. Klient smí jen svoje custom; admin smí systémové (i cizí).
     */
    @Transactional
    public ExerciseCatalogItemEntity update(AccountEntity editor, Long id, ExerciseCatalogItemEntity data) {
        ExerciseCatalogItemEntity item = getById(id);
        requireEditable(editor, item);
        copyEditableFields(data, item);
        return item; // dirty checking
    }

    /**
     * Smaže (deaktivuje) cvik. Klient jen svoje custom; admin systémové.
     * Systémové cviky se jen deaktivují (active=false), aby nezůstaly visící FK
     * z historických tréninků; custom klienta lze deaktivovat stejně.
     */
    @Transactional
    public void delete(AccountEntity editor, Long id) {
        ExerciseCatalogItemEntity item = getById(id);
        requireEditable(editor, item);
        item.setActive(false);
    }

    // -----------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------

    private void requireEditable(AccountEntity editor, ExerciseCatalogItemEntity item) {
        boolean admin = editor.getRole() == AccountRole.ADMIN;
        if (admin) {
            return; // admin smí vše
        }
        // klient smí jen svoje custom (ne systémové)
        boolean ownCustom = !item.isSystem()
                && item.getCreatedBy() != null
                && item.getCreatedBy().getId().equals(editor.getId());
        if (!ownCustom) {
            throw new ForbiddenException("Tento cvik nemůžeš upravovat — patří systému nebo jinému uživateli.");
        }
    }

    private void copyEditableFields(ExerciseCatalogItemEntity from, ExerciseCatalogItemEntity to) {
        to.setName(from.getName());
        to.setBodyRegion(from.getBodyRegion());
        to.setMovementPattern(from.getMovementPattern());
        to.setPrimaryMuscle(from.getPrimaryMuscle());
        to.setSecondaryMuscles(from.getSecondaryMuscles());
        to.setEquipment(from.getEquipment());
        to.setDescription(from.getDescription());
    }
}
