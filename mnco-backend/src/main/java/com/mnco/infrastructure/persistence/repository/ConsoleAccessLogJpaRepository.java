package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.ConsoleAccessLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsoleAccessLogJpaRepository extends JpaRepository<ConsoleAccessLogJpaEntity, UUID> {

    List<ConsoleAccessLogJpaEntity> findByInstanceId(UUID instanceId);

    List<ConsoleAccessLogJpaEntity> findByUserId(UUID userId);

    Optional<ConsoleAccessLogJpaEntity> findTopByInstanceIdAndUserIdOrderByAccessedAtDesc(UUID instanceId, UUID userId);

    List<String> findDistinctNodeIdByInstanceIdAndUserId(UUID instanceId, UUID userId);
}
