package com.ragnarok.ragnarok_customers_training_diary.dto.mapper;

import com.ragnarok.ragnarok_customers_training_diary.dto.CustomizingExerciseDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity;
import com.ragnarok.ragnarok_customers_training_diary.dto.ExerciseDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ExerciseMapper {


    public ExerciseEntity toEntity(ExerciseDTO dto) {
        ExerciseEntity entity = new ExerciseEntity();
        entity.setExerciseId(dto.getExerciseId());
        entity.setNameOfExercise(dto.getNameOfExercise());
        entity.setNote(dto.getNote());
        entity.setOrderIndex(dto.getOrderIndex());
        entity.setExerciseType(dto.getExerciseType());

        if (dto.getCustomizingExercise() != null) {
            CustomizingExerciseEntity customizing = toCustomizingEntity(dto.getCustomizingExercise());
            customizing.setExercise(entity);
            entity.setCustomizingExercise(customizing);
        }

        return entity;
    }

    public ExerciseDTO toDto(ExerciseEntity entity) {
        ExerciseDTO dto = new ExerciseDTO();
        dto.setExerciseId(entity.getExerciseId());
        if (entity.getTraining() != null) {
            dto.setTrainingId(entity.getTraining().getTrainingId());
        }
        dto.setNameOfExercise(entity.getNameOfExercise());
        dto.setNote(entity.getNote());
        dto.setOrderIndex(entity.getOrderIndex());
        dto.setExerciseType(entity.getExerciseType());

        if (entity.getCustomizingExercise() != null) {
            dto.setCustomizingExercise(toCustomizingDto(entity.getCustomizingExercise()));
        }

        return dto;
    }

    private com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity toCustomizingEntity(CustomizingExerciseDTO dto) {
        com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity entity = new com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity();
        entity.setCustomizingExerciseId(dto.getCustomizingExerciseId());
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
        return entity;
    }

    private CustomizingExerciseDTO toCustomizingDto(com.ragnarok.ragnarok_customers_training_diary.entity.CustomizingExerciseEntity entity) {
        CustomizingExerciseDTO dto = new CustomizingExerciseDTO();
        dto.setCustomizingExerciseId(entity.getCustomizingExerciseId());
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