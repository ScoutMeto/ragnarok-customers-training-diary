package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form-backing pro cvik v rámci tréninku. Buď {@code catalogItemId} (vybráno z katalogu)
 * nebo {@code customName} (vlastní text) — XOR, jeden z nich musí být vyplněn.
 */
@Getter
@Setter
@NoArgsConstructor
public class TrainingExerciseInput {

    private Long id;

    @NotNull
    private TrainingExerciseType type = TrainingExerciseType.FREEFORM;

    private Long catalogItemId;

    @Size(max = 128)
    private String customName;

    @Min(1)
    @Max(10)
    private Short rpe;

    private String notes;

    /** Phase 11 (A2): ID tagů zaměření přiřazených cviku. */
    private java.util.Set<Long> tagIds = new java.util.HashSet<>();

    // --- Phase 11 (A14): náčiní/nářadí ---
    @Size(max = 64)
    private String equipmentName;
    private java.math.BigDecimal equipmentWeightKg;
    private Integer equipmentCount;
    private java.math.BigDecimal equipmentSecondWeightKg;

    @Valid
    private List<SetInput> sets = new ArrayList<>();

    // --- Per-type config (jen ten se vyplní co odpovídá `type`) ---

    @Valid
    private EmomConfigInput emom;

    @Valid
    private TabataConfigInput tabata;

    @Valid
    private AmrapConfigInput amrap;

    @Valid
    private CircuitConfigInput circuit;

    /** Sdílený pro Ladder, Stepladder, Pyramid. */
    @Valid
    private NumericSeriesConfigInput numericSeries;

    /** Sdílený pro Superset a Complex. */
    @Valid
    private CompositeSetConfigInput composite;

    @Valid
    private StraightSetsConfigInput straightSets;

    /** Phase 13 (A11): Interval. */
    @Valid
    private IntervalConfigInput interval;

    /**
     * Validuje XOR mezi {@code catalogItemId} a {@code customName} — exactly one set.
     */
    public boolean isNamingValid() {
        boolean hasCatalog = catalogItemId != null;
        boolean hasCustom  = customName != null && !customName.isBlank();
        return hasCatalog ^ hasCustom;
    }
}
