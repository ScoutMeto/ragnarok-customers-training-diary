package com.ragnarok.ragnarok_customers_training_diary.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircuitDTO {

    private Long circuitId;
    private Long trainingId;
    private String name;
    private String notes;
    private int rounds;
    private List<CircuitStepDTO> steps = new ArrayList<>();
    private List<CircuitRoundRestDTO> roundRests = new ArrayList<>();
}