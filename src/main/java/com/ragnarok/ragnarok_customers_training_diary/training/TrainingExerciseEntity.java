package com.ragnarok.ragnarok_customers_training_diary.training;

import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.amrap.AmrapConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.circuit.CircuitConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.composite.CompositeSetConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.emom.EmomConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.series.NumericSeriesConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.straight.StraightSetsConfigEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.types.tabata.TabataConfigEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cvik v rámci tréninku — drží odkaz na katalog ({@link #catalogItem}) nebo vlastní
 * název ({@link #customName}); právě jeden z těchto dvou musí být vyplněn (XOR).
 *
 * <p>Typ ({@link TrainingExerciseType}) určuje formulář a logiku — Fáze 1 implementuje
 * jen {@link TrainingExerciseType#FREEFORM}, ostatní typy přijdou ve Fázi 3.
 */
@Entity
@Table(name = "training_exercise")
@Getter
@Setter
@NoArgsConstructor
public class TrainingExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_id", nullable = false)
    private TrainingEntity training;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TrainingExerciseType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private ExerciseCatalogItemEntity catalogItem;

    @Column(name = "custom_name", length = 128)
    private String customName;

    private Short rpe;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Phase 14 (A16): „korunka" — per-instance oblíbený cvik (důležitý pro uživatele). */
    @Column(name = "starred", nullable = false)
    private boolean starred = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("setIndex ASC")
    private List<ExerciseSetEntity> sets = new ArrayList<>();

    // --- Phase 11 (A14): použité náčiní/nářadí ---
    @Column(name = "equipment_name", length = 64)
    private String equipmentName;

    @Column(name = "equipment_weight_kg", precision = 7, scale = 2)
    private java.math.BigDecimal equipmentWeightKg;

    /** 1 = jedna zátěž, 2 = dvě zátěže současně (např. 2 kettlebelly). */
    @Column(name = "equipment_count", nullable = false)
    private int equipmentCount = 1;

    /** Váha druhé zátěže (když count=2 a liší se od první). */
    @Column(name = "equipment_second_weight_kg", precision = 7, scale = 2)
    private java.math.BigDecimal equipmentSecondWeightKg;

    /** Phase 11 (A2): per-exercise tagy zaměření (sdílený pool s tréninkovými tagy). */
    @jakarta.persistence.ManyToMany(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinTable(
            name = "training_exercise_tag_link",
            joinColumns = @jakarta.persistence.JoinColumn(name = "training_exercise_id"),
            inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "tag_id")
    )
    private java.util.Set<com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity> tags = new java.util.HashSet<>();

    // --- Per-type konfigurace (OneToOne přes shared PK).
    // V daný okamžik je pro cvik vyplněna jen jedna config (podle type).
    // Cascade ALL + orphanRemoval — když se vymění typ, stará config se smaže.

    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EmomConfigEntity emomConfig;

    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TabataConfigEntity tabataConfig;

    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AmrapConfigEntity amrapConfig;

    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CircuitConfigEntity circuitConfig;

    /** Sdílená config pro Ladder, Stepladder, Pyramid (typ rozliší {@link #type}). */
    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private NumericSeriesConfigEntity numericSeriesConfig;

    /** Sdílená config pro Superset a Complex. */
    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CompositeSetConfigEntity compositeSetConfig;

    /** Straight sets (3×8, 5×5, ...). */
    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private StraightSetsConfigEntity straightSetsConfig;

    /** Phase 13 (A11): Interval — N kol práce (reps/čas) + pauza. */
    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private com.ragnarok.ragnarok_customers_training_diary.training.types.interval.IntervalConfigEntity intervalConfig;

    /** Phase 13 (A10): StrongFirst žebřík. */
    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private com.ragnarok.ragnarok_customers_training_diary.training.types.strongfirst.StrongFirstLadderConfigEntity strongFirstLadderConfig;

    /** Phase 13 (A12): KB sport time. */
    @OneToOne(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private com.ragnarok.ragnarok_customers_training_diary.training.types.kbsport.KbSportConfigEntity kbSportConfig;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Zobrazitelné jméno cviku — buď z katalogu, nebo custom.
     */
    public String getDisplayName() {
        if (catalogItem != null) {
            return catalogItem.getName();
        }
        return customName;
    }

    public void addSet(ExerciseSetEntity set) {
        sets.add(set);
        set.setTrainingExercise(this);
    }

    public void removeSet(ExerciseSetEntity set) {
        sets.remove(set);
        set.setTrainingExercise(null);
    }
}
