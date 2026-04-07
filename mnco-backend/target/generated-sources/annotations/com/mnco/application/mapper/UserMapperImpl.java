package com.mnco.application.mapper;

import com.mnco.application.dto.response.UserResponse;
import com.mnco.domain.entities.User;
import com.mnco.domain.entities.UserRole;
import com.mnco.infrastructure.persistence.entity.UserJpaEntity;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-08T00:31:48+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Amazon.com Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UUID id = null;
        String username = null;
        String email = null;
        UserRole role = null;
        boolean enabled = false;
        Instant createdAt = null;

        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        role = user.getRole();
        enabled = user.isEnabled();
        createdAt = user.getCreatedAt();

        UserResponse userResponse = new UserResponse( id, username, email, role, enabled, createdAt );

        return userResponse;
    }

    @Override
    public User toDomain(UserJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        User.Builder user = User.builder();

        user.password( entity.getPassword() );
        user.id( entity.getId() );
        user.username( entity.getUsername() );
        user.email( entity.getEmail() );
        user.role( entity.getRole() );
        user.enabled( entity.isEnabled() );
        user.createdAt( entity.getCreatedAt() );
        user.updatedAt( entity.getUpdatedAt() );

        return user.build();
    }

    @Override
    public UserJpaEntity toJpaEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserJpaEntity.UserJpaEntityBuilder userJpaEntity = UserJpaEntity.builder();

        userJpaEntity.password( user.getPassword() );
        userJpaEntity.id( user.getId() );
        userJpaEntity.username( user.getUsername() );
        userJpaEntity.email( user.getEmail() );
        userJpaEntity.role( user.getRole() );
        userJpaEntity.enabled( user.isEnabled() );
        userJpaEntity.createdAt( user.getCreatedAt() );
        userJpaEntity.updatedAt( user.getUpdatedAt() );

        return userJpaEntity.build();
    }
}
