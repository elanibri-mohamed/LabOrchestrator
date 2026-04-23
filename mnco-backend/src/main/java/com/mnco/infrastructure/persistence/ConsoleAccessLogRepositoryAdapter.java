package com.mnco.infrastructure.persistence;

import com.mnco.domain.entities.ConsoleAccessLog;
import com.mnco.domain.repository.ConsoleAccessLogRepository;
import com.mnco.infrastructure.persistence.entity.ConsoleAccessLogJpaEntity;
import com.mnco.infrastructure.persistence.repository.ConsoleAccessLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter for ConsoleAccessLogRepository using JPA.
 */
@Component
@RequiredArgsConstructor
public class ConsoleAccessLogRepositoryAdapter implements ConsoleAccessLogRepository {

    private final ConsoleAccessLogJpaRepository jpaRepository;

    @Override
    public void save(ConsoleAccessLog log) {
        ConsoleAccessLogJpaEntity entity = toJpaEntity(log);
        jpaRepository.save(entity);
    }

    @Override
    public List<ConsoleAccessLog> findByInstanceId(UUID instanceId) {
        return jpaRepository.findByInstanceId(instanceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsoleAccessLog> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ConsoleAccessLog> findTopByInstanceIdAndUserIdOrderByAccessedAtDesc(UUID instanceId, UUID userId) {
        return jpaRepository.findTopByInstanceIdAndUserIdOrderByAccessedAtDesc(instanceId, userId)
                .map(this::toDomain);
    }

    @Override
    public List<String> findDistinctNodeIdByInstanceIdAndUserId(UUID instanceId, UUID userId) {
        return jpaRepository.findDistinctNodeIdByInstanceIdAndUserId(instanceId, userId);
    }

    private ConsoleAccessLog toDomain(ConsoleAccessLogJpaEntity entity) {
        return ConsoleAccessLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .instanceId(entity.getInstanceId())
                .nodeId(entity.getNodeId())
                .accessedAt(entity.getAccessedAt())
                .build();
    }

    private ConsoleAccessLogJpaEntity toJpaEntity(ConsoleAccessLog domain) {
        return ConsoleAccessLogJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .instanceId(domain.getInstanceId())
                .nodeId(domain.getNodeId())
                .accessedAt(domain.getAccessedAt())
                .build();
    }
}
