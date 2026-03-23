package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.CustomizingExerciseDTO;
import java.util.List;

public interface CustomizingExerciseService {

    CustomizingExerciseDTO createCustomizingExercise(CustomizingExerciseDTO customizingExerciseDTO);

    CustomizingExerciseDTO getCustomizingExerciseById(Long customizingExerciseId);

    List<CustomizingExerciseDTO> getCustomizingExercisesByTrainingId(Long trainingId);

    CustomizingExerciseDTO updateCustomizingExercise(Long customizingExerciseId, CustomizingExerciseDTO customizingExerciseDTO);

    void deleteCustomizingExercise(Long customizingExerciseId);
}