package com.ragnarok.ragnarok_customers_training_diary.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogAttributeOptionRepository extends JpaRepository<CatalogAttributeOptionEntity, Long> {

    /** Hodnoty daného druhu — systémové první, pak abecedně. */
    List<CatalogAttributeOptionEntity> findByKindOrderByIsSystemDescNameAsc(CatalogAttributeOptionEntity.Kind kind);

    Optional<CatalogAttributeOptionEntity> findByKindAndNameIgnoreCase(
            CatalogAttributeOptionEntity.Kind kind, String name);
}
