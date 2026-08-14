package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Položka v globálním katalogu cviků. Klient si volí z katalogu (drop-down) anebo
 * napíše vlastní {@code custom_name} přímo v cviku tréninku.
 *
 * <p>Systémové položky ({@code isSystem = true}) jsou součástí seed migrace a
 * nelze je smazat (jen deaktivovat). Custom položky vytváří admin.
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
     * kolo 10: cvik může spadat do víc oblastí těla najednou (dřep s výskokem =
     * dolní část těla + střed těla). Do V42 to byla jedna hodnota.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exercise_catalog_body_region",
            joinColumns = @JoinColumn(name = "catalog_item_id"))
    @Column(name = "body_region", length = 32)
    @Enumerated(EnumType.STRING)
    private Set<BodyRegion> bodyRegions = new LinkedHashSet<>();

    /**
     * Phase 20a: rozšiřitelný číselník (system hodnoty + admin custom) → uložen jako text.
     * kolo 10: složitější cvik obsahuje víc vzorců, ne jen jeden primární.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exercise_catalog_movement_pattern",
            joinColumns = @JoinColumn(name = "catalog_item_id"))
    @Column(name = "movement_pattern", length = 64)
    private Set<String> movementPatterns = new LinkedHashSet<>();

    @Column(name = "primary_muscle", length = 64)
    private String primaryMuscle;

    /** Phase 17: zapojené/vedlejší svalové skupiny (volný text). */
    @Column(name = "secondary_muscles", length = 255)
    private String secondaryMuscles;

    /** Phase 20b: rozšiřitelný číselník (system enum hodnoty + admin custom) → uložen jako text. */
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
