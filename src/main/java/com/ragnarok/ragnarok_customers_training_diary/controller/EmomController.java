package com.ragnarok.ragnarok_customers_training_diary.controller;

import com.ragnarok.ragnarok_customers_training_diary.dto.EmomDTO;
import com.ragnarok.ragnarok_customers_training_diary.service.EmomService;
import jakarta.validation.Valid;
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
public class EmomController {

    private final EmomService emomService;

    public EmomController(EmomService emomService) {
        this.emomService = emomService;
    }

    @PostMapping({"/emom", "/emom/"})
    public ResponseEntity<EmomDTO> createEmom(@RequestBody @Valid EmomDTO emomDTO) {
        EmomDTO created = emomService.createEmom(emomDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping({"/emom/{emomId}", "/emom/{emomId}/"})
    public EmomDTO getEmomById(@PathVariable Long emomId) {
        return emomService.getEmomById(emomId);
    }

    @GetMapping({"/trainings/{trainingId}/emom", "/trainings/{trainingId}/emom/"})
    public List<EmomDTO> getEmomsByTrainingId(@PathVariable Long trainingId) {
        return emomService.getEmomsByTrainingId(trainingId);
    }

    @PutMapping({"/emom/{emomId}", "/emom/{emomId}/"})
    public EmomDTO updateEmom(@PathVariable Long emomId, @RequestBody @Valid EmomDTO emomDTO) {
        return emomService.updateEmom(emomId, emomDTO);
    }

    @DeleteMapping({"/emom/{emomId}", "/emom/{emomId}/"})
    public ResponseEntity<Void> deleteEmom(@PathVariable Long emomId) {
        emomService.deleteEmom(emomId);
        return ResponseEntity.noContent().build();
    }
}