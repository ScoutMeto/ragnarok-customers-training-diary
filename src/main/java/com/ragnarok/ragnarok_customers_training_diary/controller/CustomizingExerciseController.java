package com.ragnarok.ragnarok_customers_training_diary.controller;

import com.ragnarok.ragnarok_customers_training_diary.dto.CustomizingExerciseDTO;
import com.ragnarok.ragnarok_customers_training_diary.service.CustomizingExerciseService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CustomizingExerciseController {

    private final CustomizingExerciseService customizingExerciseService;

    public CustomizingExerciseController(CustomizingExerciseService customizingExerciseService) {
        this.customizingExerciseService = customizingExerciseService;
    }

    @PostMapping({"/customizing-exercise", "/customizing-exercise/"})
    public ResponseEntity<CustomizingExerciseDTO> createCustomizingExercise(@RequestBody CustomizingExerciseDTO customizingExerciseDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customizingExerciseService.createCustomizingExercise(customizingExerciseDTO));
    }

    @GetMapping({"/customizing-exercise/{customizingExerciseId}", "/customizing-exercise/{customizingExerciseId}/"})
    public CustomizingExerciseDTO getCustomizingExerciseById(@PathVariable Long customizingExerciseId) {
        return customizingExerciseService.getCustomizingExerciseById(customizingExerciseId);
    }

    @GetMapping({"/trainings/{trainingId}/customizing-exercises", "/trainings/{trainingId}/customizing-exercises/"})
    public List<CustomizingExerciseDTO> getCustomizingExercisesByTrainingId(@PathVariable Long trainingId) {
        return customizingExerciseService.getCustomizingExercisesByTrainingId(trainingId);
    }

    @PutMapping({"/customizing-exercise/{customizingExerciseId}", "/customizing-exercise/{customizingExerciseId}/"})
    public CustomizingExerciseDTO updateCustomizingExercise(@PathVariable Long customizingExerciseId,
                                                            @RequestBody CustomizingExerciseDTO customizingExerciseDTO) {
        return customizingExerciseService.updateCustomizingExercise(customizingExerciseId, customizingExerciseDTO);
    }

    @DeleteMapping({"/customizing-exercise/{customizingExerciseId}", "/customizing-exercise/{customizingExerciseId}/"})
    public ResponseEntity<Void> deleteCustomizingExercise(@PathVariable Long customizingExerciseId) {
        customizingExerciseService.deleteCustomizingExercise(customizingExerciseId);
        return ResponseEntity.noContent().build();
    }
}