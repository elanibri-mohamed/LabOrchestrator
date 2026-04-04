package com.mnco.config;

import com.mnco.domain.entities.Lab;
import com.mnco.domain.repository.LabRepository;
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
 * Scheduled job that auto-stops labs that have been idle beyond the configured threshold.
 * This enforces the resource governance requirement from the SRS (FR-RM-003).
 *
 * Runs every 15 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabIdleStopScheduler {

    private final LabRepository labRepository;
    private final EveNgService eveNgService;

    @Value("${quota.lab-idle-timeout-minutes:120}")
    private int idleTimeoutMinutes;

    @Scheduled(fixedDelayString = "900000") // every 15 minutes
    @Transactional
    public void stopIdleLabs() {
        Instant idleThreshold = Instant.now().minus(idleTimeoutMinutes, ChronoUnit.MINUTES);
        List<Lab> idleLabs = labRepository.findRunningLabsIdleSince(idleThreshold);

        if (idleLabs.isEmpty()) {
            log.debug("Idle lab check: no labs exceeded {}min idle threshold", idleTimeoutMinutes);
            return;
        }

        log.info("Auto-stopping {} idle lab(s) (idle > {}min)", idleLabs.size(), idleTimeoutMinutes);

        for (Lab lab : idleLabs) {
            try {
                log.info("Auto-stopping idle lab: id={}, name='{}', lastActive={}",
                        lab.getId(), lab.getName(), lab.getLastActiveAt());

                if (lab.getEvengLabId() != null) {
                    eveNgService.stopLab(lab.getEvengLabId());
                }
                lab.markStopped();
                labRepository.save(lab);

                log.info("Auto-stopped lab id={}", lab.getId());
            } catch (Exception ex) {
                log.error("Failed to auto-stop lab id={}: {}", lab.getId(), ex.getMessage());
                lab.markError();
                labRepository.save(lab);
            }
        }
    }
}
