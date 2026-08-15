package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rozšiřitelný číselník atributů katalogu. Systémové hodnoty jsou globální,
 * vlastní hodnoty mohou patřit konkrétnímu uživateli.
 */
@Entity
@Table(name = "catalog_attribute_option")
@Getter
@Setter
@NoArgsConstructor
public class CatalogAttributeOptionEntity {

    public enum Kind { BODY_REGION, MOVEMENT_PATTERN, EQUIPMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AccountEntity owner;

    public CatalogAttributeOptionEntity(Kind kind, String name, boolean isSystem) {
        this.kind = kind;
        this.name = name;
        this.isSystem = isSystem;
    }

    public String getLabel() {
        if (kind == Kind.BODY_REGION) {
            return CatalogLabels.bodyRegion(name);
        }
        if (kind == Kind.MOVEMENT_PATTERN) {
            return CatalogLabels.movementPattern(name);
        }
        return name;
    }
}