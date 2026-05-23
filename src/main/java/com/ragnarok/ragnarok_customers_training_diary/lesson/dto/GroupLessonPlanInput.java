package com.ragnarok.ragnarok_customers_training_diary.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Form-backing pro tvorbu / editaci skupinové lekce. */
@Getter
@Setter
@NoArgsConstructor
public class GroupLessonPlanInput {

    private Long id;

    @NotNull(message = "Datum lekce je povinné")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lessonDate = LocalDate.now();

    @NotNull(message = "Začátek je povinný")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotBlank(message = "Název lekce je povinný")
    @Size(max = 128)
    private String lessonName;

    /** ID trenéra (admin). Default = aktuálně přihlášený admin. */
    @NotNull(message = "Trenér je povinný")
    private Long coachId;

    private String description;
}
