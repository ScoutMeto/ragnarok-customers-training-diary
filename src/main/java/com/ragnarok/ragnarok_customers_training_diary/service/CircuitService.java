package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.CircuitDTO;
import java.util.List;

public interface CircuitService {

    CircuitDTO createCircuit(CircuitDTO circuitDTO);

    CircuitDTO getCircuitById(Long circuitId);

    List<CircuitDTO> getCircuitsByTrainingId(Long trainingId);

    CircuitDTO updateCircuit(Long circuitId, CircuitDTO circuitDTO);

    void deleteCircuit(Long circuitId);
}