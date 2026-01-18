package com.ragnarok.ragnarok_customers_training_diary.dto.mapper;

import com.ragnarok.ragnarok_customers_training_diary.dto.ExerciseDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.ExerciseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Duration;

@Mapper(componentModel = "spring")
public interface ExerciseMapper {
//    ExerciseEntity toEntity(ExerciseDTO dto);
//    ExerciseDTO toDto(ExerciseEntity entity);
//}

    //min + sec save to "duration"
    @Mapping(target = "duration", expression = "java(toDuration(dto.getMinutes(), dto.getSeconds()))")
    ExerciseEntity toEntity(ExerciseDTO dto);

    //split "duration" to min and sec
    @Mapping(target = "minutes", expression = "java(toMinutes(entity.getDuration()))")
    @Mapping(target = "seconds", expression = "java(toSeconds(entity.getDuration()))")
    ExerciseDTO toDto(ExerciseEntity entity);

    // Help methods for "duration" logic
    default Duration toDuration(Integer minutes, Integer seconds) {
        int m = minutes == null ? 0 : minutes;
        int s = seconds == null ? 0 : seconds;
        return Duration.ofMinutes(m).plusSeconds(s);
    }

    default int toMinutes(Duration duration) {
        return duration == null ? 0 : (int) duration.toMinutes();
    }

    default int toSeconds(Duration duration) {
        if (duration == null) return 0;
        long m = duration.toMinutes();
        return (int) duration.minusMinutes(m).getSeconds();
    }
}
