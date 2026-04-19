package com.mnco.config;

import com.mnco.application.usecases.LabUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically synchronizes labs from EVE-NG server.
 * This enables read-only mode where labs are managed exclusively in EVE-NG
 * and the app discovers and syncs them automatically.
 *
 * Runs every hour (configurable via property).
 * Disabled by default; enable with property: scheduler.lab-sync-enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.lab-sync-enabled", havingValue = "true", matchIfMissing = false)
public class LabSyncScheduler {

    private final LabUseCase labUseCase;

    @Scheduled(fixedDelayString = "${scheduler.lab-sync-interval:3600000}") // every 1 hour by default
    public void syncLabsFromEveNg() {
        log.info("Starting scheduled EVE-NG lab synchronization");

        try {
            var synced = labUseCase.discoverLabsFromEveNg();
            if (synced.isEmpty()) {
                log.debug("Scheduled sync: no new labs found in EVE-NG");
            } else {
                log.info("Scheduled sync completed: {} labs synced from EVE-NG", synced.size());
            }
        } catch (Exception ex) {
            log.error("Scheduled lab sync failed: {}", ex.getMessage(), ex);
            // Continue running on next cycle - do not stop scheduler
        }
    }
}
