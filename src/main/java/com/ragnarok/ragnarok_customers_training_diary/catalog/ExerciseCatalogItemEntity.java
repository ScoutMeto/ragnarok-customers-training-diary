package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Položka v katalogu cviků. Systémové položky jsou sdílené, uživatelská úprava
 * systémové položky se ukládá jako vlastní kopie.
 */
@Entity
@Table(name = "exercise_catalog_item")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseCatalogItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    /**
     * Rozšiřitelný číselník oblastí těla. Systémové hodnoty zůstávají uložené
     * jako původní stabilní klíče: FULL_BODY, UPPER_BODY, LOWER_BODY, CORE.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exercise_catalog_body_region",
            joinColumns = @JoinColumn(name = "catalog_item_id"))
    @Column(name = "body_region", length = 64)
    private Set<String> bodyRegions = new LinkedHashSet<>();

    /** Rozšiřitelný číselník pohybových vzorců uložený jako stabilní textový klíč. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exercise_catalog_movement_pattern",
            joinColumns = @JoinColumn(name = "catalog_item_id"))
    @Column(name = "movement_pattern", length = 64)
    private Set<String> movementPatterns = new LinkedHashSet<>();

    @Column(name = "primary_muscle", length = 64)
    private String primaryMuscle;

    @Column(name = "secondary_muscles", length = 255)
    private String secondaryMuscles;

    @Column(length = 64)
    private String equipment;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AccountEntity createdBy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}