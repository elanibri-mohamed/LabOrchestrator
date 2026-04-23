package com.mnco.domain.repository;

import com.mnco.domain.entities.ConsoleAccessLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting console access logs (console_access_log table).
 * Used for progress monitoring and audit.
 */
public interface ConsoleAccessLogRepository {

    void save(ConsoleAccessLog log);

    List<ConsoleAccessLog> findByInstanceId(UUID instanceId);

    List<ConsoleAccessLog> findByUserId(UUID userId);

    Optional<ConsoleAccessLog> findTopByInstanceIdAndUserIdOrderByAccessedAtDesc(UUID instanceId, UUID userId);

    List<String> findDistinctNodeIdByInstanceIdAndUserId(UUID instanceId, UUID userId);
}
