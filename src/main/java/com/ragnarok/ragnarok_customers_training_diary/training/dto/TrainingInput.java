package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form-backing pro tvorbu / editaci tréninku.
 */
@Getter
@Setter
@NoArgsConstructor
public class TrainingInput {

    private Long id;

    @NotNull(message = "Datum tréninku je povinné")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate trainingDate = LocalDate.now();

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 128)
    private String name;

    private TrainingDifficulty difficulty;

    @Min(1)
    @Max(10)
    private Short rpe;

    private String notes;

    // -------- ScoutMeto kolo 5: kondiční metriky (nepovinné) --------
    @jakarta.validation.constraints.DecimalMin("0.0")
    @jakarta.validation.constraints.DecimalMax("500.0")
    private java.math.BigDecimal bodyweightKg;

    @Min(20) @Max(250)
    private Short restingHrBpm;

    @Min(0) @Max(100)
    private Short sleepQuality;

    @Min(1) @Max(10)
    private Short sleepQualityRpe;

    @Min(20) @Max(250)
    private Short avgHrBpm;

    @Min(20) @Max(250)
    private Short maxHrBpm;

    // -------- ScoutMeto kolo 5: ženský cyklus --------
    @Min(1) @Max(40)
    private Short cycleDay;

    private com.ragnarok.ragnarok_customers_training_diary.training.CyclePhase cyclePhase;

    private Set<Long> tagIds = new HashSet<>();

    @Valid
    private List<TrainingExerciseInput> exercises = new ArrayList<>();
}
