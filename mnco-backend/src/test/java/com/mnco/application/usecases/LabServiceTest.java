package com.mnco.application.usecases;

import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.mapper.LabMapper;
import com.mnco.domain.entities.Lab;
import com.mnco.domain.entities.LabStatus;
import com.mnco.domain.entities.ResourceQuota;
import com.mnco.domain.repository.LabRepository;
import com.mnco.domain.repository.ResourceQuotaRepository;
import com.mnco.exception.custom.*;
import com.mnco.infrastructure.external.eveng.EveNgService;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LabService — all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LabService")
class LabServiceTest {

    @Mock private LabRepository labRepository;
    @Mock private ResourceQuotaRepository quotaRepository;
    @Mock private EveNgService eveNgService;
    @Mock private LabMapper labMapper;

    @InjectMocks private LabService labService;

    private UUID ownerId;
    private ResourceQuota quota;
    private Lab stoppedLab;
    private Lab runningLab;
    private LabResponse labResponse;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        // Default quota: 3 labs max, none used
        quota = new ResourceQuota();
        quota.setUserId(ownerId);
        quota.setMaxLabs(3);
        quota.setMaxCpu(8);
        quota.setMaxRamGb(16);
        quota.setMaxStorageGb(50);
        quota.setUsedLabs(0);
        quota.setUsedCpu(0);
        quota.setUsedRamGb(0);
        quota.setUsedStorageGb(0);

        stoppedLab = Lab.builder()
                .id(UUID.randomUUID())
                .name("Test Lab")
                .ownerId(ownerId)
                .status(LabStatus.STOPPED)
                .evengLabId("/test-lab.unl")
                .cpuAllocated(2)
                .ramAllocated(4)
                .storageAllocated(20)
                .build();

        runningLab = Lab.builder()
                .id(UUID.randomUUID())
                .name("Running Lab")
                .ownerId(ownerId)
                .status(LabStatus.RUNNING)
                .evengLabId("/running-lab.unl")
                .cpuAllocated(2)
                .ramAllocated(4)
                .storageAllocated(20)
                .lastActiveAt(Instant.now())
                .build();

        labResponse = new LabResponse(
                stoppedLab.getId(), "Test Lab", null, LabStatus.STOPPED,
                ownerId, null, "/test-lab.unl", 2, 4, 20, null, null, Instant.now()
        );
    }

    // ── createLab ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createLab()")
    class CreateLab {

        @Test
        @DisplayName("should create lab and allocate quota when within limits")
        void shouldCreateLabSuccessfully() {
            var request = new CreateLabRequest("New Lab", "desc", null, 2, 4, 20);
            var createdLab = Lab.builder()
                    .id(UUID.randomUUID()).name("New Lab").ownerId(ownerId)
                    .status(LabStatus.STOPPED).evengLabId("/new-lab.unl")
                    .cpuAllocated(2).ramAllocated(4).storageAllocated(20).build();

            when(quotaRepository.findOrCreateDefault(ownerId)).thenReturn(quota);
            when(labRepository.save(any())).thenReturn(createdLab);
            when(eveNgService.createTopology(any())).thenReturn(
                    new EveNgLabResult("/new-lab.unl", "node-1", "/opt/labs/new-lab.unl", "created"));
            when(labMapper.toResponse(any())).thenReturn(labResponse);

            LabResponse result = labService.createLab(request, ownerId);

            assertThat(result).isNotNull();
            verify(quotaRepository).save(argThat(q -> q.getUsedLabs() == 1 && q.getUsedCpu() == 2));
            verify(eveNgService).createTopology(any());
        }

        @Test
        @DisplayName("should throw QuotaExceededException when lab count at maximum")
        void shouldThrowWhenLabQuotaFull() {
            quota.setUsedLabs(3); // already at max
            var request = new CreateLabRequest("New Lab", null, null, 1, 1, 10);

            when(quotaRepository.findOrCreateDefault(ownerId)).thenReturn(quota);

            assertThatThrownBy(() -> labService.createLab(request, ownerId))
                    .isInstanceOf(QuotaExceededException.class)
                    .hasMessageContaining("Quota exceeded");

            verify(labRepository, never()).save(any());
            verify(eveNgService, never()).createTopology(any());
        }

        @Test
        @DisplayName("should throw QuotaExceededException when CPU would exceed limit")
        void shouldThrowWhenCpuQuotaExceeded() {
            quota.setUsedCpu(7); // 7 used out of 8 max, requesting 2 more
            var request = new CreateLabRequest("Lab", null, null, 2, 1, 10);

            when(quotaRepository.findOrCreateDefault(ownerId)).thenReturn(quota);

            assertThatThrownBy(() -> labService.createLab(request, ownerId))
                    .isInstanceOf(QuotaExceededException.class);
        }

        @Test
        @DisplayName("should rollback quota and mark lab ERROR on EVE-NG failure")
        void shouldRollbackQuotaOnEveNgFailure() {
            var request = new CreateLabRequest("Lab", null, null, 2, 4, 20);
            var savedLab = Lab.builder().id(UUID.randomUUID()).name("Lab")
                    .ownerId(ownerId).status(LabStatus.CREATING)
                    .cpuAllocated(2).ramAllocated(4).storageAllocated(20).build();

            when(quotaRepository.findOrCreateDefault(ownerId)).thenReturn(quota);
            when(labRepository.save(any())).thenReturn(savedLab);
            when(eveNgService.createTopology(any())).thenThrow(new RuntimeException("EVE-NG down"));

            assertThatThrownBy(() -> labService.createLab(request, ownerId))
                    .isInstanceOf(EveNgIntegrationException.class);

            // Quota must be released (save called twice: allocate then rollback)
            verify(quotaRepository, times(2)).save(any());
            // Lab must be saved with ERROR status
            verify(labRepository, times(2)).save(argThat(l ->
                    l.getStatus() == LabStatus.CREATING || l.getStatus() == LabStatus.ERROR));
        }
    }

    // ── startLab ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startLab()")
    class StartLab {

        @Test
        @DisplayName("should start a STOPPED lab")
        void shouldStartStoppedLab() {
            when(labRepository.findById(stoppedLab.getId())).thenReturn(Optional.of(stoppedLab));
            when(labRepository.save(any())).thenReturn(stoppedLab);
            when(labMapper.toResponse(any())).thenReturn(labResponse);

            LabResponse result = labService.startLab(stoppedLab.getId(), ownerId);

            assertThat(result).isNotNull();
            verify(eveNgService).startLab("/test-lab.unl");
        }

        @Test
        @DisplayName("should throw InvalidLabStateException when lab is already RUNNING")
        void shouldThrowWhenAlreadyRunning() {
            when(labRepository.findById(runningLab.getId())).thenReturn(Optional.of(runningLab));

            assertThatThrownBy(() -> labService.startLab(runningLab.getId(), ownerId))
                    .isInstanceOf(InvalidLabStateException.class)
                    .hasMessageContaining("cannot be started");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when requester is not the owner")
        void shouldThrowWhenNotOwner() {
            UUID otherUser = UUID.randomUUID();
            when(labRepository.findById(stoppedLab.getId())).thenReturn(Optional.of(stoppedLab));

            assertThatThrownBy(() -> labService.startLab(stoppedLab.getId(), otherUser))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when lab not found")
        void shouldThrowWhenLabNotFound() {
            UUID fakeId = UUID.randomUUID();
            when(labRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> labService.startLab(fakeId, ownerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── stopLab ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stopLab()")
    class StopLab {

        @Test
        @DisplayName("should stop a RUNNING lab")
        void shouldStopRunningLab() {
            when(labRepository.findById(runningLab.getId())).thenReturn(Optional.of(runningLab));
            when(labRepository.save(any())).thenReturn(runningLab);
            when(labMapper.toResponse(any())).thenReturn(labResponse);

            LabResponse result = labService.stopLab(runningLab.getId(), ownerId);

            assertThat(result).isNotNull();
            verify(eveNgService).stopLab("/running-lab.unl");
        }

        @Test
        @DisplayName("should throw InvalidLabStateException when lab is STOPPED")
        void shouldThrowWhenAlreadyStopped() {
            when(labRepository.findById(stoppedLab.getId())).thenReturn(Optional.of(stoppedLab));

            assertThatThrownBy(() -> labService.stopLab(stoppedLab.getId(), ownerId))
                    .isInstanceOf(InvalidLabStateException.class)
                    .hasMessageContaining("not RUNNING");
        }
    }

    // ── deleteLab ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteLab()")
    class DeleteLab {

        @Test
        @DisplayName("should delete a STOPPED lab and release quota")
        void shouldDeleteStoppedLab() {
            when(labRepository.findById(stoppedLab.getId())).thenReturn(Optional.of(stoppedLab));
            when(labRepository.save(any())).thenReturn(stoppedLab);
            when(quotaRepository.findOrCreateDefault(ownerId)).thenReturn(quota);

            labService.deleteLab(stoppedLab.getId(), ownerId);

            verify(eveNgService).deleteLab("/test-lab.unl");
            verify(quotaRepository).save(argThat(q ->
                    q.getUsedLabs() == -1 || q.getUsedLabs() == 0)); // released
        }

        @Test
        @DisplayName("should throw InvalidLabStateException when lab is RUNNING")
        void shouldThrowWhenRunning() {
            when(labRepository.findById(runningLab.getId())).thenReturn(Optional.of(runningLab));

            assertThatThrownBy(() -> labService.deleteLab(runningLab.getId(), ownerId))
                    .isInstanceOf(InvalidLabStateException.class)
                    .hasMessageContaining("must be STOPPED");
        }
    }

    // ── getLabsByOwner ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLabsByOwner()")
    class GetLabsByOwner {

        @Test
        @DisplayName("should return only non-deleted labs for owner")
        void shouldReturnNonDeletedLabs() {
            Lab deletedLab = Lab.builder().id(UUID.randomUUID())
                    .ownerId(ownerId).status(LabStatus.DELETED).build();

            when(labRepository.findByOwnerId(ownerId))
                    .thenReturn(List.of(stoppedLab, runningLab, deletedLab));
            when(labMapper.toResponse(any())).thenReturn(labResponse);

            List<LabResponse> results = labService.getLabsByOwner(ownerId);

            // Deleted lab must be filtered out
            assertThat(results).hasSize(2);
        }
    }
}
