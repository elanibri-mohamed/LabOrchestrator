package com.mnco.application.usecases;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Legacy test — disabled. LabService was replaced by LabInstanceService
 * and LabTemplateService in the multi-tenant refactor.
 * TODO: Write new tests for LabInstanceService.
 */
@Disabled("LabService replaced by LabInstanceService in multi-tenant refactor")
@ExtendWith(MockitoExtension.class)
@DisplayName("LabService (Legacy)")
class LabServiceTest {

    @Test
    void placeholder() {
        // No-op: tests to be rewritten for LabInstanceService
    }
}
