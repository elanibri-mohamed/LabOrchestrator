package com.mnco.application.mapper;

import com.mnco.application.dto.response.UserResponse;
import com.mnco.domain.entities.User;
import com.mnco.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper bridging domain User ↔ JPA entity ↔ DTO.
 * componentModel = "spring" makes it injectable as a Spring bean.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    // Domain → DTO
    UserResponse toResponse(User user);

    // JPA entity → Domain
    @Mapping(target = "password", source = "password")
    User toDomain(UserJpaEntity entity);

    // Domain → JPA entity
    @Mapping(target = "password", source = "password")
    UserJpaEntity toJpaEntity(User user);
}
