package com.mnco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MNCO — Multi-Tenant Network & Cybersecurity Lab Orchestrator
 *
 * Architecture: Clean Architecture (Domain / Application / Infrastructure / Presentation)
 * Stack: Java 21 · Spring Boot 3.2 · PostgreSQL · JWT · EVE-NG API v2
 *
 * @EnableAsync — required for AuditLogService async writes (FR-AA-07, FR-LM-10)
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
public class MncoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MncoApplication.class, args);
    }
}
