package com.mnco;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for all controller integration tests.
 *
 * Uses H2 in-memory database (application-test.yml),
 * simulation-mode EVE-NG, and a full Spring Security context.
 *
 * Extend this to get MockMvc + full application context without
 * duplicating annotations everywhere.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "eveng.simulation-mode=true",
        "jwt.secret=integration-test-secret-key-must-be-at-least-256bits-long-for-hmac-sha",
        "jwt.expiration-ms=3600000",
        "quota.default-max-labs=5",
        "quota.default-max-cpu=16",
        "quota.default-max-ram-gb=32",
        "quota.default-max-storage-gb=100"
})
public abstract class BaseIntegrationTest {
    // Shared configuration. Subclasses inject MockMvc, repos, etc. as needed.
}
