package com.ragnarok.ragnarok_customers_training_diary.controller;

import com.ragnarok.ragnarok_customers_training_diary.dto.CircuitDTO;
import com.ragnarok.ragnarok_customers_training_diary.service.CircuitService;
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
public class CircuitController {

    private final CircuitService circuitService;

    public CircuitController(CircuitService circuitService) {
        this.circuitService = circuitService;
    }

    @PostMapping({"/circuit", "/circuit/"})
    public ResponseEntity<CircuitDTO> createCircuit(@RequestBody CircuitDTO circuitDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(circuitService.createCircuit(circuitDTO));
    }

    @GetMapping({"/circuit/{circuitId}", "/circuit/{circuitId}/"})
    public CircuitDTO getCircuitById(@PathVariable Long circuitId) {
        return circuitService.getCircuitById(circuitId);
    }

    @GetMapping({"/trainings/{trainingId}/circuits", "/trainings/{trainingId}/circuits/"})
    public List<CircuitDTO> getCircuitsByTrainingId(@PathVariable Long trainingId) {
        return circuitService.getCircuitsByTrainingId(trainingId);
    }

    @PutMapping({"/circuit/{circuitId}", "/circuit/{circuitId}/"})
    public CircuitDTO updateCircuit(@PathVariable Long circuitId, @RequestBody CircuitDTO circuitDTO) {
        return circuitService.updateCircuit(circuitId, circuitDTO);
    }

    @DeleteMapping({"/circuit/{circuitId}", "/circuit/{circuitId}/"})
    public ResponseEntity<Void> deleteCircuit(@PathVariable Long circuitId) {
        circuitService.deleteCircuit(circuitId);
        return ResponseEntity.noContent().build();
    }
}