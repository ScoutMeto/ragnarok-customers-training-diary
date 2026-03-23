package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.CustomizingExerciseDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.CustomizingExerciseRepository;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.ExerciseRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomizingExerciseServiceImpl implements CustomizingExerciseService {

    private final CustomizingExerciseRepository customizingExerciseRepository;
    private final ExerciseRepository exerciseRepository;

    public CustomizingExerciseServiceImpl(
            CustomizingExerciseRepository customizingExerciseRepository,
            ExerciseRepository exerciseRepository
    ) {
        this.customizingExerciseRepository = customizingExerciseRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    @Transactional
    public CustomizingExerciseDTO createCustomizingExercise(CustomizingExerciseDTO customizingExerciseDTO) {
        ExerciseEntity exercise = getAndValidateCustomExercise(customizingExerciseDTO.getExerciseId());
        CustomizingExerciseEntity entity = toEntity(customizingExerciseDTO, exercise);
        return toDTO(customizingExerciseRepository.save(entity));
    }

    @Override
    public CustomizingExerciseDTO getCustomizingExerciseById(Long customizingExerciseId) {
        CustomizingExerciseEntity entity = customizingExerciseRepository.findById(customizingExerciseId)
                .orElseThrow(() -> new EntityNotFoundException("Customizing exercise se zadaným ID nebyl nalezen."));
        return toDTO(entity);
    }

    @Override
    public List<CustomizingExerciseDTO> getCustomizingExercisesByTrainingId(Long trainingId) {
        return customizingExerciseRepository.findByExerciseTrainingTrainingId(trainingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public CustomizingExerciseDTO updateCustomizingExercise(Long customizingExerciseId, CustomizingExerciseDTO customizingExerciseDTO) {
        CustomizingExerciseEntity existing = customizingExerciseRepository.findById(customizingExerciseId)
                .orElseThrow(() -> new EntityNotFoundException("Customizing exercise se zadaným ID nebyl nalezen."));

        ExerciseEntity exercise = getAndValidateCustomExercise(customizingExerciseDTO.getExerciseId());
        existing.setExercise(exercise);
        applyValues(existing, customizingExerciseDTO);

        return toDTO(customizingExerciseRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteCustomizingExercise(Long customizingExerciseId) {
        if (!customizingExerciseRepository.existsById(customizingExerciseId)) {
            throw new EntityNotFoundException("Customizing exercise se zadaným ID nebyl nalezen.");
        }
        customizingExerciseRepository.deleteById(customizingExerciseId);
    }

    private ExerciseEntity getAndValidateCustomExercise(Long exerciseId) {
        ExerciseEntity exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new EntityNotFoundException("Exercise se zadaným ID nebyl nalezen."));

        if (exercise.getExerciseType() != ExerciseType.CUSTOMIZING_EXERCISE) {
            throw new IllegalArgumentException("Exercise musí mít typ CUSTOMIZING_EXERCISE.");
        }
        return exercise;
    }

    private CustomizingExerciseEntity toEntity(CustomizingExerciseDTO dto, ExerciseEntity exercise) {
        CustomizingExerciseEntity entity = new CustomizingExerciseEntity();
        entity.setExercise(exercise);
        applyValues(entity, dto);
        return entity;
    }

    private void applyValues(CustomizingExerciseEntity entity, CustomizingExerciseDTO dto) {
        entity.setDuration(toDuration(dto.getMinutes(), dto.getSeconds()));
        entity.setDescriptionOfResistance(dto.getDescriptionOfResistance());
        entity.setWeightOrResistance(dto.getWeightOrResistance());
        entity.setReps(dto.getReps());
        entity.setSets(dto.getSets());
        entity.setRest(dto.getRest());
        entity.setLeftSide(dto.getLeftSide());
        entity.setRightSide(dto.getRightSide());
        entity.setBilateral(dto.getBilateral());
        entity.setFullBody(dto.isFullBody());
        entity.setUpperBody(dto.isUpperBody());
        entity.setLowerBody(dto.isLowerBody());
        entity.setSpecificPart(dto.isSpecificPart());
        entity.setPush(dto.isPush());
        entity.setPull(dto.isPull());
        entity.setRotation(dto.isRotation());
        entity.setLunge(dto.isLunge());
        entity.setIsometric(dto.isIsometric());
        entity.setCarry(dto.isCarry());
        entity.setGait(dto.isGait());
        entity.setPlyometrics(dto.isPlyometrics());
        entity.setCrawling(dto.isCrawling());
        entity.setJumping(dto.isJumping());
        entity.setCoreAndAbs(dto.isCoreAndAbs());
        entity.setAnotherMovementPattern(dto.isAnotherMovementPattern());
    }

    private CustomizingExerciseDTO toDTO(CustomizingExerciseEntity entity) {
        CustomizingExerciseDTO dto = new CustomizingExerciseDTO();
        dto.setCustomizingExerciseId(entity.getCustomizingExerciseId());
        dto.setExerciseId(entity.getExercise().getExerciseId());
        dto.setMinutes(toMinutes(entity.getDuration()));
        dto.setSeconds(toSeconds(entity.getDuration()));
        dto.setDescriptionOfResistance(entity.getDescriptionOfResistance());
        dto.setWeightOrResistance(entity.getWeightOrResistance());
        dto.setReps(entity.getReps());
        dto.setSets(entity.getSets());
        dto.setRest(entity.getRest());
        dto.setLeftSide(entity.getLeftSide());
        dto.setRightSide(entity.getRightSide());
        dto.setBilateral(entity.getBilateral());
        dto.setFullBody(entity.isFullBody());
        dto.setUpperBody(entity.isUpperBody());
        dto.setLowerBody(entity.isLowerBody());
        dto.setSpecificPart(entity.isSpecificPart());
        dto.setPush(entity.isPush());
        dto.setPull(entity.isPull());
        dto.setRotation(entity.isRotation());
        dto.setLunge(entity.isLunge());
        dto.setIsometric(entity.isIsometric());
        dto.setCarry(entity.isCarry());
        dto.setGait(entity.isGait());
        dto.setPlyometrics(entity.isPlyometrics());
        dto.setCrawling(entity.isCrawling());
        dto.setJumping(entity.isJumping());
        dto.setCoreAndAbs(entity.isCoreAndAbs());
        dto.setAnotherMovementPattern(entity.isAnotherMovementPattern());
        return dto;
    }

    private Duration toDuration(Integer minutes, Integer seconds) {
        int m = minutes == null ? 0 : minutes;
        int s = seconds == null ? 0 : seconds;
        return Duration.ofMinutes(m).plusSeconds(s);
    }

    private int toMinutes(Duration duration) {
        return duration == null ? 0 : (int) duration.toMinutes();
    }

    private int toSeconds(Duration duration) {
        if (duration == null) {
            return 0;
        }
        long minutes = duration.toMinutes();
        return (int) duration.minusMinutes(minutes).getSeconds();
    }
}