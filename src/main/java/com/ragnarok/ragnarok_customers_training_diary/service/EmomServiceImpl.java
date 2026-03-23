package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.EmomDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.EmomMinuteOverrideDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.EmomEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.EmomMinuteOverrideEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.EmomRepository;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.ExerciseRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmomServiceImpl implements EmomService {

    private final EmomRepository emomRepository;
    private final ExerciseRepository exerciseRepository;

    public EmomServiceImpl(EmomRepository emomRepository, ExerciseRepository exerciseRepository) {
        this.emomRepository = emomRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    @Transactional
    public EmomDTO createEmom(EmomDTO emomDTO) {
        validateEmom(emomDTO);

        ExerciseEntity exercise = exerciseRepository.findById(emomDTO.getExerciseId())
                .orElseThrow(() -> new EntityNotFoundException("Exercise se zadaným ID nebyl nalezen."));

        if (exercise.getExerciseType() != ExerciseType.EMOM) {
            throw new IllegalArgumentException("Exercise musí mít typ EMOM.");
        }

        EmomEntity entity = toEntity(emomDTO, exercise);
        return toDTO(emomRepository.save(entity));
    }

    @Override
    public EmomDTO getEmomById(Long emomId) {
        EmomEntity entity = emomRepository.findById(emomId)
                .orElseThrow(() -> new EntityNotFoundException("EMOM se zadaným ID nebyl nalezen."));
        return toDTO(entity);
    }

    @Override
    public List<EmomDTO> getEmomsByTrainingId(Long trainingId) {
        return emomRepository.findByExerciseTrainingTrainingId(trainingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public EmomDTO updateEmom(Long emomId, EmomDTO emomDTO) {
        validateEmom(emomDTO);

        EmomEntity existing = emomRepository.findById(emomId)
                .orElseThrow(() -> new EntityNotFoundException("EMOM se zadaným ID nebyl nalezen."));

        ExerciseEntity exercise = exerciseRepository.findById(emomDTO.getExerciseId())
                .orElseThrow(() -> new EntityNotFoundException("Exercise se zadaným ID nebyl nalezen."));

        if (exercise.getExerciseType() != ExerciseType.EMOM) {
            throw new IllegalArgumentException("Exercise musí mít typ EMOM.");
        }

        existing.setExercise(exercise);
        existing.setNotes(emomDTO.getNotes());
        existing.setTotalMinutes(emomDTO.getTotalMinutes());
        existing.setIntervalSeconds(emomDTO.getIntervalSeconds());
        existing.setDefaultReps(emomDTO.getDefaultReps());
        existing.setDefaultWeight(emomDTO.getDefaultWeight());

        existing.getMinuteOverrides().clear();
        for (EmomMinuteOverrideDTO overrideDTO : emomDTO.getMinuteOverrides()) {
            EmomMinuteOverrideEntity overrideEntity = new EmomMinuteOverrideEntity();
            overrideEntity.setEmom(existing);
            overrideEntity.setMinuteIndex(overrideDTO.getMinuteIndex());
            overrideEntity.setReps(overrideDTO.getReps());
            overrideEntity.setWeight(overrideDTO.getWeight());
            overrideEntity.setNotes(overrideDTO.getNotes());
            existing.getMinuteOverrides().add(overrideEntity);
        }

        return toDTO(emomRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteEmom(Long emomId) {
        if (!emomRepository.existsById(emomId)) {
            throw new EntityNotFoundException("EMOM se zadaným ID nebyl nalezen.");
        }
        emomRepository.deleteById(emomId);
    }

    private void validateEmom(EmomDTO emomDTO) {
        if (emomDTO.getTotalMinutes() < 1) {
            throw new IllegalArgumentException("Počet minut EMOM musí být větší než 0.");
        }

        if (emomDTO.getIntervalSeconds() < 1) {
            throw new IllegalArgumentException("Interval v sekundách musí být větší než 0.");
        }

        Set<Integer> minuteIndexes = new HashSet<>();
        for (EmomMinuteOverrideDTO override : emomDTO.getMinuteOverrides()) {
            if (override.getMinuteIndex() < 1 || override.getMinuteIndex() > emomDTO.getTotalMinutes()) {
                throw new IllegalArgumentException("Minute override musí být v rozsahu 1 až totalMinutes.");
            }
            if (!minuteIndexes.add(override.getMinuteIndex())) {
                throw new IllegalArgumentException("Minute override obsahuje duplicitní minuteIndex.");
            }
        }
    }

    private EmomEntity toEntity(EmomDTO dto, ExerciseEntity exercise) {
        EmomEntity entity = new EmomEntity();
        entity.setExercise(exercise);
        entity.setNotes(dto.getNotes());
        entity.setTotalMinutes(dto.getTotalMinutes());
        entity.setIntervalSeconds(dto.getIntervalSeconds());
        entity.setDefaultReps(dto.getDefaultReps());
        entity.setDefaultWeight(dto.getDefaultWeight());

        List<EmomMinuteOverrideEntity> overrides = dto.getMinuteOverrides().stream()
                .map(overrideDTO -> {
                    EmomMinuteOverrideEntity overrideEntity = new EmomMinuteOverrideEntity();
                    overrideEntity.setEmom(entity);
                    overrideEntity.setMinuteIndex(overrideDTO.getMinuteIndex());
                    overrideEntity.setReps(overrideDTO.getReps());
                    overrideEntity.setWeight(overrideDTO.getWeight());
                    overrideEntity.setNotes(overrideDTO.getNotes());
                    return overrideEntity;
                })
                .collect(Collectors.toList());
        entity.setMinuteOverrides(overrides);

        return entity;
    }

    private EmomDTO toDTO(EmomEntity entity) {
        EmomDTO dto = new EmomDTO();
        dto.setEmomId(entity.getEmomId());
        dto.setExerciseId(entity.getExercise().getExerciseId());
        dto.setNotes(entity.getNotes());
        dto.setTotalMinutes(entity.getTotalMinutes());
        dto.setIntervalSeconds(entity.getIntervalSeconds());
        dto.setDefaultReps(entity.getDefaultReps());
        dto.setDefaultWeight(entity.getDefaultWeight());

        List<EmomMinuteOverrideDTO> overrides = entity.getMinuteOverrides().stream()
                .sorted(Comparator.comparingInt(EmomMinuteOverrideEntity::getMinuteIndex))
                .map(overrideEntity -> {
                    EmomMinuteOverrideDTO overrideDTO = new EmomMinuteOverrideDTO();
                    overrideDTO.setEmomMinuteOverrideId(overrideEntity.getEmomMinuteOverrideId());
                    overrideDTO.setMinuteIndex(overrideEntity.getMinuteIndex());
                    overrideDTO.setReps(overrideEntity.getReps());
                    overrideDTO.setWeight(overrideEntity.getWeight());
                    overrideDTO.setNotes(overrideEntity.getNotes());
                    return overrideDTO;
                }).toList();
        dto.setMinuteOverrides(overrides);

        return dto;
    }
}