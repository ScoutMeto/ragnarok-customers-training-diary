package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.CircuitDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.CircuitRoundRestDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.CircuitStepDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.CircuitEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.CircuitRoundRestEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.CircuitStepEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.CircuitRepository;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.ExerciseRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CircuitServiceImpl implements CircuitService {

    private final CircuitRepository circuitRepository;
    private final ExerciseRepository exerciseRepository;

    public CircuitServiceImpl(CircuitRepository circuitRepository, ExerciseRepository exerciseRepository) {
        this.circuitRepository = circuitRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    @Transactional
    public CircuitDTO createCircuit(CircuitDTO circuitDTO) {
        ExerciseEntity exercise = getAndValidateCircuitExercise(circuitDTO.getExerciseId());
        CircuitEntity entity = toEntity(circuitDTO, exercise);
        return toDTO(circuitRepository.save(entity));
    }

    @Override
    public CircuitDTO getCircuitById(Long circuitId) {
        CircuitEntity entity = circuitRepository.findById(circuitId)
                .orElseThrow(() -> new EntityNotFoundException("Circuit se zadaným ID nebyl nalezen."));
        return toDTO(entity);
    }

    @Override
    public List<CircuitDTO> getCircuitsByTrainingId(Long trainingId) {
        return circuitRepository.findByExerciseTrainingTrainingId(trainingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public CircuitDTO updateCircuit(Long circuitId, CircuitDTO circuitDTO) {
        CircuitEntity existing = circuitRepository.findById(circuitId)
                .orElseThrow(() -> new EntityNotFoundException("Circuit se zadaným ID nebyl nalezen."));

        ExerciseEntity exercise = getAndValidateCircuitExercise(circuitDTO.getExerciseId());
        existing.setExercise(exercise);
        existing.setNotes(circuitDTO.getNotes());
        existing.setRounds(circuitDTO.getRounds());

        existing.getSteps().clear();
        for (CircuitStepDTO stepDTO : circuitDTO.getSteps()) {
            CircuitStepEntity stepEntity = new CircuitStepEntity();
            stepEntity.setCircuit(existing);
            stepEntity.setOrderIndex(stepDTO.getOrderIndex());
            stepEntity.setName(stepDTO.getName());
            stepEntity.setReps(stepDTO.getReps());
            stepEntity.setTimeSeconds(stepDTO.getTimeSeconds());
            stepEntity.setWeight(stepDTO.getWeight());
            stepEntity.setRestSeconds(stepDTO.getRestSeconds());
            stepEntity.setNotes(stepDTO.getNotes());
            existing.getSteps().add(stepEntity);
        }

        existing.getRoundRests().clear();
        for (CircuitRoundRestDTO restDTO : circuitDTO.getRoundRests()) {
            CircuitRoundRestEntity restEntity = new CircuitRoundRestEntity();
            restEntity.setCircuit(existing);
            restEntity.setRoundIndex(restDTO.getRoundIndex());
            restEntity.setRestSeconds(restDTO.getRestSeconds());
            existing.getRoundRests().add(restEntity);
        }

        return toDTO(circuitRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteCircuit(Long circuitId) {
        if (!circuitRepository.existsById(circuitId)) {
            throw new EntityNotFoundException("Circuit se zadaným ID nebyl nalezen.");
        }
        circuitRepository.deleteById(circuitId);
    }

    private ExerciseEntity getAndValidateCircuitExercise(Long exerciseId) {
        ExerciseEntity exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new EntityNotFoundException("Exercise se zadaným ID nebyl nalezen."));

        if (exercise.getExerciseType() != ExerciseType.CIRCUIT) {
            throw new IllegalArgumentException("Exercise musí mít typ CIRCUIT.");
        }
        return exercise;
    }

    private CircuitEntity toEntity(CircuitDTO dto, ExerciseEntity exercise) {
        CircuitEntity entity = new CircuitEntity();
        entity.setExercise(exercise);
        entity.setNotes(dto.getNotes());
        entity.setRounds(dto.getRounds());

        entity.setSteps(dto.getSteps().stream().map(stepDTO -> {
            CircuitStepEntity stepEntity = new CircuitStepEntity();
            stepEntity.setCircuit(entity);
            stepEntity.setOrderIndex(stepDTO.getOrderIndex());
            stepEntity.setName(stepDTO.getName());
            stepEntity.setReps(stepDTO.getReps());
            stepEntity.setTimeSeconds(stepDTO.getTimeSeconds());
            stepEntity.setWeight(stepDTO.getWeight());
            stepEntity.setRestSeconds(stepDTO.getRestSeconds());
            stepEntity.setNotes(stepDTO.getNotes());
            return stepEntity;
        }).toList());

        entity.setRoundRests(dto.getRoundRests().stream().map(restDTO -> {
            CircuitRoundRestEntity restEntity = new CircuitRoundRestEntity();
            restEntity.setCircuit(entity);
            restEntity.setRoundIndex(restDTO.getRoundIndex());
            restEntity.setRestSeconds(restDTO.getRestSeconds());
            return restEntity;
        }).toList());

        return entity;
    }

    private CircuitDTO toDTO(CircuitEntity entity) {
        CircuitDTO dto = new CircuitDTO();
        dto.setCircuitId(entity.getCircuitId());
        dto.setExerciseId(entity.getExercise().getExerciseId());
        dto.setNotes(entity.getNotes());
        dto.setRounds(entity.getRounds());

        List<CircuitStepDTO> steps = entity.getSteps().stream()
                .sorted(Comparator.comparingInt(CircuitStepEntity::getOrderIndex))
                .map(stepEntity -> {
                    CircuitStepDTO stepDTO = new CircuitStepDTO();
                    stepDTO.setCircuitStepId(stepEntity.getCircuitStepId());
                    stepDTO.setOrderIndex(stepEntity.getOrderIndex());
                    stepDTO.setName(stepEntity.getName());
                    stepDTO.setReps(stepEntity.getReps());
                    stepDTO.setTimeSeconds(stepEntity.getTimeSeconds());
                    stepDTO.setWeight(stepEntity.getWeight());
                    stepDTO.setRestSeconds(stepEntity.getRestSeconds());
                    stepDTO.setNotes(stepEntity.getNotes());
                    return stepDTO;
                }).toList();
        dto.setSteps(steps);

        List<CircuitRoundRestDTO> rests = entity.getRoundRests().stream()
                .sorted(Comparator.comparingInt(CircuitRoundRestEntity::getRoundIndex))
                .map(restEntity -> {
                    CircuitRoundRestDTO restDTO = new CircuitRoundRestDTO();
                    restDTO.setCircuitRoundRestId(restEntity.getCircuitRoundRestId());
                    restDTO.setRoundIndex(restEntity.getRoundIndex());
                    restDTO.setRestSeconds(restEntity.getRestSeconds());
                    return restDTO;
                }).toList();
        dto.setRoundRests(rests);

        return dto;
    }
}