package com.mnco.domain.repository;

import com.mnco.domain.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for User persistence.
 * Implementations live in the infrastructure layer (JPA adapter).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAll();

    void deleteById(UUID id);
}
