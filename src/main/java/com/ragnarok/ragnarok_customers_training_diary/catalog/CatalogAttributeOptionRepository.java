package com.ragnarok.ragnarok_customers_training_diary.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogAttributeOptionRepository extends JpaRepository<CatalogAttributeOptionEntity, Long> {

    List<CatalogAttributeOptionEntity> findByKindOrderByIsSystemDescNameAsc(CatalogAttributeOptionEntity.Kind kind);

    @Query("""
            SELECT o FROM CatalogAttributeOptionEntity o
            WHERE o.kind = :kind
              AND (o.isSystem = true OR o.owner.id = :ownerId OR o.owner IS NULL)
            ORDER BY o.isSystem DESC, o.name ASC
            """)
    List<CatalogAttributeOptionEntity> findVisibleTo(
            @Param("kind") CatalogAttributeOptionEntity.Kind kind,
            @Param("ownerId") Long ownerId);

    Optional<CatalogAttributeOptionEntity> findByKindAndNameIgnoreCase(
            CatalogAttributeOptionEntity.Kind kind, String name);
}