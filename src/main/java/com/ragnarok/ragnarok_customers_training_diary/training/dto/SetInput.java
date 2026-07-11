package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form-backing pro jeden set cviku. Všechna pole nullable — klient si píše, co chce.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetInput {

    private Long id;

    private BigDecimal weightKg;

    @Min(0)
    private Integer reps;

    /** ScoutMeto kolo 8: odpočinek po sérii (s). */
    @Min(0)
    private Integer restSeconds;

    @Min(1)
    @Max(10)
    private Short rpe;

    @Size(max = 255)
    private String note;
}
