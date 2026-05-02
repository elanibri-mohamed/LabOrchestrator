package com.mnco.config;

import com.mnco.application.usecases.LabTemplateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically synchronizes lab templates from EVE-NG server.
 * Disabled by default; enable with property: scheduler.lab-sync-enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.lab-sync-enabled", havingValue = "true", matchIfMissing = false)
public class LabSyncScheduler {

    private final LabTemplateUseCase labTemplateUseCase;

    @Scheduled(fixedDelayString = "${scheduler.lab-sync-interval:3600000}")
    public void syncLabsFromEveNg() {
        log.info("Starting scheduled EVE-NG template synchronization");
        try {
            var synced = labTemplateUseCase.discoverTemplatesFromEveNg();
            if (synced.isEmpty()) {
                log.debug("Scheduled sync: no new templates found in EVE-NG");
            } else {
                log.info("Scheduled sync completed: {} templates synced", synced.size());
            }
        } catch (Exception ex) {
            log.error("Scheduled template sync failed: {}", ex.getMessage(), ex);
        }
    }
}
