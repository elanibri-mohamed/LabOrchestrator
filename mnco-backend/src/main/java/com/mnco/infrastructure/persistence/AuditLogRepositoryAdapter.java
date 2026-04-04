package com.mnco.infrastructure.persistence;

import com.mnco.domain.entities.AuditLog;
import com.mnco.domain.repository.AuditLogRepository;
import com.mnco.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.mnco.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    @Override
    public AuditLog save(AuditLog log) {
        var entity = toEntity(log);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AuditLog> findByActorId(UUID actorId) {
        return jpaRepository.findByActorIdOrderByCreatedAtDesc(actorId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByLabId(UUID labId) {
        return jpaRepository.findByLabIdOrderByCreatedAtDesc(labId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        return jpaRepository.findRecent(PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    private AuditLogJpaEntity toEntity(AuditLog log) {
        return AuditLogJpaEntity.builder()
                .eventType(log.getEventType())
                .actorId(log.getActorId())
                .actorUsername(log.getActorUsername())
                .labId(log.getLabId())
                .labName(log.getLabName())
                .result(log.getResult())
                .errorCode(log.getErrorCode())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .build();
    }

    private AuditLog toDomain(AuditLogJpaEntity e) {
        AuditLog log = new AuditLog();
        log.setId(e.getId());
        log.setEventType(e.getEventType());
        log.setActorId(e.getActorId());
        log.setActorUsername(e.getActorUsername());
        log.setLabId(e.getLabId());
        log.setLabName(e.getLabName());
        log.setResult(e.getResult());
        log.setErrorCode(e.getErrorCode());
        log.setIpAddress(e.getIpAddress());
        log.setUserAgent(e.getUserAgent());
        log.setCreatedAt(e.getCreatedAt());
        return log;
    }
}
