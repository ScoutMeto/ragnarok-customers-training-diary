package com.ragnarok.ragnarok_customers_training_diary.dto.mapper;

import com.ragnarok.ragnarok_customers_training_diary.dto.UserDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserEntity toEntity (UserDTO dto);
    UserDTO toDTO (UserEntity entity);
}
