package com.ragnarok.ragnarok_customers_training_diary.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircuitStepDTO {

    private Long circuitStepId;
    private int orderIndex;
    private String name;
    private Integer reps;
    private Integer timeSeconds;
    private Double weight;
    private Integer restSeconds;
    private String notes;
}