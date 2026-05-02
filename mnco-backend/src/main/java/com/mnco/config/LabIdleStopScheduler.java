package com.mnco.config;

import com.mnco.domain.entities.InstanceStatus;
import com.mnco.domain.entities.LabInstance;
import com.mnco.domain.repository.LabInstanceRepository;
import com.mnco.infrastructure.external.eveng.EveNgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that auto-stops lab instances that have been running beyond
 * the configured idle threshold (FR-RM-003).
 *
 * Runs every 15 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabIdleStopScheduler {

    private final LabInstanceRepository labInstanceRepository;
    private final EveNgService eveNgService;

    @Value("${quota.lab-idle-timeout-minutes:120}")
    private int idleTimeoutMinutes;

    @Scheduled(fixedDelayString = "60000") // every minute
    @Transactional
    public void stopIdleLabs() {
        stopExpiredLabs();

        Instant idleThreshold = Instant.now().minus(idleTimeoutMinutes, ChronoUnit.MINUTES);
        List<LabInstance> idleInstances = labInstanceRepository.findRunningLabsIdleSince(idleThreshold);

        if (idleInstances.isEmpty()) {
            log.debug("Idle lab check: no instances exceeded {}min idle threshold", idleTimeoutMinutes);
            return;
        }

        log.info("Auto-stopping {} idle instance(s) (idle > {}min)", idleInstances.size(), idleTimeoutMinutes);

        for (LabInstance instance : idleInstances) {
            try {
                log.info("Auto-stopping idle instance: id={}, startedAt={}", instance.getId(), instance.getStartedAt());
                eveNgService.stopLab(instance.getEveInstancePath());
                instance.setStatus(InstanceStatus.STOPPED);
                instance.setStoppedAt(Instant.now());
                instance.setExpiresAt(null);
                labInstanceRepository.save(instance);
                log.info("Auto-stopped instance id={}", instance.getId());
            } catch (Exception ex) {
                log.error("Failed to auto-stop instance id={}: {}", instance.getId(), ex.getMessage());
                instance.setStatus(InstanceStatus.ERROR);
                instance.setExpiresAt(null);
                labInstanceRepository.save(instance);
            }
        }
    }

    private void stopExpiredLabs() {
        Instant now = Instant.now();
        List<LabInstance> expiredInstances = labInstanceRepository.findRunningLabsExpiredAtOrBefore(now);

        if (expiredInstances.isEmpty()) {
            return;
        }

        log.info("Auto-stopping {} expired instance(s)", expiredInstances.size());

        for (LabInstance instance : expiredInstances) {
            try {
                log.info("Auto-stopping expired instance: id={}, expiresAt={}", instance.getId(), instance.getExpiresAt());
                eveNgService.stopLab(instance.getEveInstancePath());
                instance.setStatus(InstanceStatus.STOPPED);
                instance.setStoppedAt(now);
                instance.setExpiresAt(null);
                labInstanceRepository.save(instance);
                log.info("Auto-stopped expired instance id={}", instance.getId());
            } catch (Exception ex) {
                log.error("Failed to auto-stop expired instance id={}: {}", instance.getId(), ex.getMessage());
                instance.setStatus(InstanceStatus.ERROR);
                instance.setExpiresAt(null);
                labInstanceRepository.save(instance);
            }
        }
    }
}
