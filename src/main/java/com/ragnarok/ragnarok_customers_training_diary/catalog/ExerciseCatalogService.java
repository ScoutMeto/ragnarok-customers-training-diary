package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Čte katalog cviků. Není tu CRUD pro admin/uživatele — to přijde později
 * (Fáze 2 admin sekce). Pro Fázi 1 stačí read.
 */
@Service
@Transactional(readOnly = true)
public class ExerciseCatalogService {

    private static final int SEARCH_LIMIT = 50;

    private final ExerciseCatalogItemRepository repository;

    public ExerciseCatalogService(ExerciseCatalogItemRepository repository) {
        this.repository = repository;
    }

    public List<ExerciseCatalogItemEntity> listAll() {
        return repository.findByActiveTrueOrderByNameAsc();
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
}
