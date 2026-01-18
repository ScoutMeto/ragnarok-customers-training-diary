package com.ragnarok.ragnarok_customers_training_diary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AimedTrainingsRequestDTO {

    private List<Long> trainingIds;

}
