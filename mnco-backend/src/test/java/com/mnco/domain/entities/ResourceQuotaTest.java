package com.mnco.domain.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Pure domain tests for ResourceQuota — no Spring, no mocks.
 * Verifies the allocation/release domain logic in isolation.
 */
@DisplayName("ResourceQuota domain entity")
class ResourceQuotaTest {

    private ResourceQuota quota;

    @BeforeEach
    void setUp() {
        quota = new ResourceQuota();
        quota.setUserId(UUID.randomUUID());
        quota.setMaxLabs(3);
        quota.setMaxCpu(8);
        quota.setMaxRamGb(16);
        quota.setMaxStorageGb(50);
        quota.setUsedLabs(0);
        quota.setUsedCpu(0);
        quota.setUsedRamGb(0);
        quota.setUsedStorageGb(0);
    }

    @Test
    @DisplayName("canAllocate returns true when all resources within limits")
    void shouldAllowAllocationWithinLimits() {
        assertThat(quota.canAllocate(2, 4, 20)).isTrue();
    }

    @Test
    @DisplayName("canAllocate returns false when lab count is at maximum")
    void shouldRejectWhenLabCountMaxed() {
        quota.setUsedLabs(3);
        assertThat(quota.canAllocate(1, 1, 10)).isFalse();
    }

    @Test
    @DisplayName("canAllocate returns false when CPU would exceed max")
    void shouldRejectWhenCpuExceeded() {
        quota.setUsedCpu(7);
        assertThat(quota.canAllocate(2, 1, 10)).isFalse(); // 7 + 2 = 9 > 8
    }

    @Test
    @DisplayName("canAllocate returns false when RAM would exceed max")
    void shouldRejectWhenRamExceeded() {
        quota.setUsedRamGb(14);
        assertThat(quota.canAllocate(1, 4, 10)).isFalse(); // 14 + 4 = 18 > 16
    }

    @Test
    @DisplayName("allocate increments all usage counters correctly")
    void shouldIncrementCountersOnAllocate() {
        quota.allocate(2, 4, 20);

        assertThat(quota.getUsedLabs()).isEqualTo(1);
        assertThat(quota.getUsedCpu()).isEqualTo(2);
        assertThat(quota.getUsedRamGb()).isEqualTo(4);
        assertThat(quota.getUsedStorageGb()).isEqualTo(20);
    }

    @Test
    @DisplayName("release decrements all usage counters correctly")
    void shouldDecrementCountersOnRelease() {
        quota.allocate(2, 4, 20);
        quota.release(2, 4, 20);

        assertThat(quota.getUsedLabs()).isEqualTo(0);
        assertThat(quota.getUsedCpu()).isEqualTo(0);
        assertThat(quota.getUsedRamGb()).isEqualTo(0);
        assertThat(quota.getUsedStorageGb()).isEqualTo(0);
    }

    @Test
    @DisplayName("release never produces negative usage counters")
    void shouldNeverGoNegativeOnRelease() {
        // Release without prior allocation — counters already at 0
        quota.release(5, 8, 30);

        assertThat(quota.getUsedLabs()).isZero();
        assertThat(quota.getUsedCpu()).isZero();
        assertThat(quota.getUsedRamGb()).isZero();
        assertThat(quota.getUsedStorageGb()).isZero();
    }

    @Test
    @DisplayName("remaining calculations are correct after allocation")
    void shouldCalculateRemainingCorrectly() {
        quota.allocate(2, 4, 20);

        assertThat(quota.getRemainingLabs()).isEqualTo(2);     // 3 - 1
        assertThat(quota.getRemainingCpu()).isEqualTo(6);      // 8 - 2
        assertThat(quota.getRemainingRamGb()).isEqualTo(12);   // 16 - 4
        assertThat(quota.getRemainingStorage()).isEqualTo(30); // 50 - 20
    }
}
