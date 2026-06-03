package com.ragnarok.ragnarok_customers_training_diary.training.types.kbsport;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phase 13 (A12): KB sport time — počet opakování za určený čas. Souhrn (celkový
 * čas + celkový počet opakování) + volitelný podrobný záznam rozdělený na intervaly
 * ({@code splitIntervalSeconds}). U unilaterálních cviků lze každý interval označit
 * L/P (multiswitch — libovolná délka intervalu).
 */
@Entity
@Table(name = "kb_sport_config")
@Getter
@Setter
@NoArgsConstructor
public class KbSportConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    /** Celkový čas setu v sekundách (např. 600 = 10 min). */
    @Column(name = "total_seconds", nullable = false)
    private Integer totalSeconds;

    /** Celkový počet opakování (souhrnný zápis). */
    @Column(name = "total_reps")
    private Integer totalReps;

    /** Délka intervalu pro podrobný záznam (s). null = jen souhrn. */
    @Column(name = "split_interval_seconds")
    private Integer splitIntervalSeconds;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    /** Unilaterální cvik — intervaly lze označit L/P. */
    @Column(nullable = false)
    private boolean unilateral = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "kbSportConfig", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("intervalIndex ASC")
    private List<KbSportIntervalEntity> intervals = new ArrayList<>();
}
