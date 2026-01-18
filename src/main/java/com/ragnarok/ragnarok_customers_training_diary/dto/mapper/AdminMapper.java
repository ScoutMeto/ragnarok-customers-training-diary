package com.ragnarok.ragnarok_customers_training_diary.dto.mapper;

import com.ragnarok.ragnarok_customers_training_diary.dto.AdminDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.AdminEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(target = "authorities", ignore = true)
    AdminEntity toEntity(AdminDTO source);

    AdminDTO toDTO (AdminEntity source);

}
