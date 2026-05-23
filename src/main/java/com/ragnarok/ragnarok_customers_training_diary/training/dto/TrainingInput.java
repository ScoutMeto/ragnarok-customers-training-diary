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

    private Set<Long> tagIds = new HashSet<>();

    @Valid
    private List<TrainingExerciseInput> exercises = new ArrayList<>();
}
