package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.LabMapper;
import com.mnco.domain.entities.Lab;
import com.mnco.domain.entities.LabStatus;
import com.mnco.domain.repository.LabRepository;
import com.mnco.infrastructure.persistence.repository.LabJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the domain LabRepository port using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class LabRepositoryAdapter implements LabRepository {

    private final LabJpaRepository jpaRepository;
    private final LabMapper labMapper;

    @Override
    public Lab save(Lab lab) {
        var entity = labMapper.toJpaEntity(lab);
        var saved = jpaRepository.save(entity);
        return labMapper.toDomain(saved);
    }

    @Override
    public Optional<Lab> findById(UUID id) {
        return jpaRepository.findById(id).map(labMapper::toDomain);
    }

    @Override
    public List<Lab> findByOwnerId(UUID ownerId) {
        return jpaRepository.findByOwnerId(ownerId).stream()
                .map(labMapper::toDomain)
                .toList();
    }

    @Override
    public List<Lab> findByStatus(LabStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(labMapper::toDomain)
                .toList();
    }

    @Override
    public List<Lab> findAll() {
        return jpaRepository.findAll().stream()
                .map(labMapper::toDomain)
                .toList();
    }

    @Override
    public long countActiveLabsByOwner(UUID ownerId) {
        return jpaRepository.countActiveLabsByOwner(ownerId);
    }

    @Override
    public List<Lab> findRunningLabsIdleSince(Instant threshold) {
        return jpaRepository.findRunningLabsIdleSince(threshold).stream()
                .map(labMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
