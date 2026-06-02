package com.ragnarok.ragnarok_customers_training_diary.equipment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentOptionRepository extends JpaRepository<EquipmentOptionEntity, Long> {

    /** Pomůcky viditelné uživateli: systémové + jeho vlastní. */
    @Query("""
            SELECT e FROM EquipmentOptionEntity e
            WHERE e.isSystem = true OR e.owner.id = :userId
            ORDER BY e.isSystem DESC, e.name ASC
            """)
    List<EquipmentOptionEntity> findVisibleTo(@Param("userId") Long userId);

    /** Hledá uživatelovu custom pomůcku podle názvu (case-insensitive). */
    @Query("""
            SELECT e FROM EquipmentOptionEntity e
            WHERE e.owner.id = :userId AND LOWER(e.name) = LOWER(:name)
            """)
    Optional<EquipmentOptionEntity> findOwnByName(@Param("userId") Long userId, @Param("name") String name);

    /** Hledá systémovou pomůcku podle názvu (case-insensitive). */
    @Query("""
            SELECT e FROM EquipmentOptionEntity e
            WHERE e.isSystem = true AND LOWER(e.name) = LOWER(:name)
            """)
    Optional<EquipmentOptionEntity> findSystemByName(@Param("name") String name);
}
