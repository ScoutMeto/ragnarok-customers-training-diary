package com.ragnarok.ragnarok_customers_training_diary.dto.mapper;

import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.TrainingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrainingMapper {

    TrainingEntity toEntity(TrainingDTO dto);

    TrainingDTO toDTO(TrainingEntity entity);

}