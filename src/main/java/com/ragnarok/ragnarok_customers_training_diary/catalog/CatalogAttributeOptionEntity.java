package com.ragnarok.ragnarok_customers_training_diary.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phase 20a/b: rozšiřitelný číselník atributů katalogu (pohybový vzorec, náčiní).
 * Systémové hodnoty (seed z původních enumů) nelze smazat; vlastní (admin přidá) ano.
 */
@Entity
@Table(name = "catalog_attribute_option")
@Getter
@Setter
@NoArgsConstructor
public class CatalogAttributeOptionEntity {

    /** Druh atributu, který číselník reprezentuje. */
    public enum Kind { MOVEMENT_PATTERN, EQUIPMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    public CatalogAttributeOptionEntity(Kind kind, String name, boolean isSystem) {
        this.kind = kind;
        this.name = name;
        this.isSystem = isSystem;
    }
}
