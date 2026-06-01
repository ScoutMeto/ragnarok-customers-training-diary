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
import java.time.LocalDateTime;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "body_region", length = 32)
    private BodyRegion bodyRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_pattern", length = 32)
    private MovementPattern movementPattern;

    @Column(name = "primary_muscle", length = 64)
    private String primaryMuscle;

    /** Phase 17: zapojené/vedlejší svalové skupiny (volný text). */
    @Column(name = "secondary_muscles", length = 255)
    private String secondaryMuscles;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private Equipment equipment;

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
