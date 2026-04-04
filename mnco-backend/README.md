# MNCO — Multi-Tenant Network & Cybersecurity Lab Orchestrator
### Backend API — Complete Implementation Summary

> **Stack:** Java 21 · Spring Boot 3.2 · Clean Architecture · PostgreSQL 15 · JWT · EVE-NG API v2
> **Total source files:** 97 · **Flyway migrations:** 3 · **Build files:** 4

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Project Structure — All 97 Files](#2-project-structure)
3. [Domain Layer](#3-domain-layer)
4. [Application Layer](#4-application-layer)
5. [Infrastructure Layer](#5-infrastructure-layer)
6. [Presentation Layer — REST API](#6-presentation-layer)
7. [Security](#7-security)
8. [Exception Handling](#8-exception-handling)
9. [Cross-Cutting Concerns](#9-cross-cutting-concerns)
10. [Database — Flyway Migrations](#10-database)
11. [Tests](#11-tests)
12. [Build & Deploy](#12-build--deploy)
13. [Quick Start](#13-quick-start)
14. [Full API Reference](#14-full-api-reference)
15. [SRS Coverage](#15-srs-coverage)
16. [Future Work](#16-future-work)

---

## 1. Architecture

The project follows Clean Architecture with strict layer separation.
Each layer depends only on the layer inside it, never outward.

```
+----------------------------------------------------------+
|  PRESENTATION   Controllers, DTOs, REST endpoints        |
+----------------------------------------------------------+
|  APPLICATION    Use Cases, Service interfaces, Mappers   |
+----------------------------------------------------------+
|  DOMAIN         Entities, Repository interfaces          |
|                 <- Pure Java. Zero framework imports ->  |
+----------------------------------------------------------+
|  INFRASTRUCTURE JPA adapters, EVE-NG client, PostgreSQL  |
+----------------------------------------------------------+
```

Key design principles enforced:
- Domain entities are pure Java: no @Entity, no Spring annotations
- Application layer calls domain repository interfaces (ports), never JPA directly
- Infrastructure adapters implement domain ports (Adapter pattern)
- EVE-NG integration is swappable via @ConditionalOnProperty: real vs simulated
- All use cases are interface-backed for full testability

---

## 2. Project Structure

```
mnco-backend/
+-- pom.xml
+-- Dockerfile
+-- docker-compose.yml
+-- README.md
+-- src/main/resources/
|   +-- application.yml
|   +-- application-test.yml
|   +-- db/migration/
|       +-- V1__init_schema.sql
|       +-- V2__seed_admin.sql
|       +-- V3__audit_and_refresh_tables.sql
+-- src/main/java/com/mnco/
    +-- MncoApplication.java
    +-- domain/
    |   +-- entities/
    |   |   +-- User.java, UserRole.java
    |   |   +-- Lab.java, LabStatus.java
    |   |   +-- LabTemplate.java, Resource.java
    |   |   +-- ResourceQuota.java, AuditLog.java
    |   +-- repository/
    |       +-- UserRepository.java, LabRepository.java
    |       +-- LabTemplateRepository.java
    |       +-- ResourceQuotaRepository.java
    |       +-- AuditLogRepository.java
    +-- application/
    |   +-- usecases/
    |   |   +-- AuthUseCase.java, AuthService.java
    |   |   +-- LabUseCase.java, LabService.java
    |   |   +-- LabTemplateUseCase.java, LabTemplateService.java
    |   |   +-- AuditLogService.java, RefreshTokenService.java
    |   +-- dto/request/
    |   |   +-- RegisterRequest.java, LoginRequest.java
    |   |   +-- CreateLabRequest.java, CloneLabRequest.java
    |   |   +-- CreateLabTemplateRequest.java, UpdateQuotaRequest.java
    |   +-- dto/response/
    |   |   +-- ApiResponse.java, AuthResponse.java
    |   |   +-- UserResponse.java, LabResponse.java
    |   |   +-- LabTemplateResponse.java, QuotaResponse.java
    |   |   +-- AuditLogResponse.java, ErrorResponse.java
    |   +-- mapper/
    |       +-- UserMapper.java, LabMapper.java
    |       +-- LabTemplateMapper.java, ResourceQuotaMapper.java
    +-- infrastructure/
    |   +-- persistence/
    |   |   +-- entity/
    |   |   |   +-- UserJpaEntity.java, LabJpaEntity.java
    |   |   |   +-- LabTemplateJpaEntity.java
    |   |   |   +-- ResourceQuotaJpaEntity.java
    |   |   |   +-- AuditLogJpaEntity.java
    |   |   |   +-- RefreshTokenJpaEntity.java
    |   |   +-- repository/
    |   |   |   +-- UserJpaRepository.java, LabJpaRepository.java
    |   |   |   +-- LabTemplateJpaRepository.java
    |   |   |   +-- ResourceQuotaJpaRepository.java
    |   |   |   +-- AuditLogJpaRepository.java
    |   |   |   +-- RefreshTokenJpaRepository.java
    |   |   +-- UserRepositoryAdapter.java, LabRepositoryAdapter.java
    |   |   +-- LabTemplateRepositoryAdapter.java
    |   |   +-- ResourceQuotaRepositoryAdapter.java
    |   |   +-- AuditLogRepositoryAdapter.java
    |   +-- external/eveng/
    |       +-- EveNgService.java
    |       +-- EveNgRestService.java
    |       +-- EveNgSimulatedService.java
    |       +-- EveNgNodeConsoleInfo.java
    |       +-- model/
    |           +-- EveNgLabResult.java
    |           +-- EveNgCloneResult.java
    |           +-- EveNgNodeStatus.java
    +-- presentation/controller/
    |   +-- AuthController.java, LabController.java
    |   +-- LabTemplateController.java, AdminController.java
    |   +-- QuotaController.java, AuditLogController.java
    +-- security/
    |   +-- config/SecurityConfig.java
    |   +-- filter/JwtAuthenticationFilter.java
    |   +-- service/JwtService.java, UserDetailsServiceImpl.java
    +-- exception/
    |   +-- custom/ (7 typed exceptions)
    |   +-- handler/GlobalExceptionHandler.java
    +-- config/
        +-- LoggingAspect.java
        +-- LabIdleStopScheduler.java
        +-- RequestLoggingFilter.java
```

---

## 3. Domain Layer

Pure Java. Zero framework imports.

### Entities

User: id, username, email, password (hashed), role, enabled
  - isAdmin(), isInstructor(), canManageTemplates(), Builder pattern

UserRole: ADMIN, INSTRUCTOR, STUDENT, RESEARCHER

Lab: id, name, status, ownerId, evengLabId, cpu/ram/storageAllocated
  - isStartable(), isStoppable(), isDeletable(), isRunning(), isStopped()
  - isOwnedBy(userId), markStarted(), markStopped(), markError()
  - Builder pattern

LabStatus: PENDING, CREATING, RUNNING, STOPPING, STOPPED,
           DELETING, ERROR, DELETED (full lifecycle state machine)

LabTemplate: id, name, description, topologyYaml, version, authorId, isPublic

Resource: id, labId, cpu, ram, storage

ResourceQuota: userId, maxLabs/Cpu/RamGb/StorageGb, usedLabs/Cpu/RamGb/StorageGb
  - canAllocate(cpu, ram, storage): checks all 4 dimensions at once
  - allocate(cpu, ram, storage): increments all usage counters
  - release(cpu, ram, storage): decrements safely (never negative)
  - getRemainingLabs/Cpu/RamGb/Storage(): computed remaining capacity

AuditLog: id, eventType, actorId, actorUsername, labId, labName, result, errorCode, ipAddress
  - EventType: LOGIN, LOGIN_FAILED, TOKEN_REFRESH, LOGOUT,
               LAB_CREATED, LAB_STARTED, LAB_STOPPED,
               LAB_CLONED, LAB_DELETED, NODE_STARTED, NODE_STOPPED
  - Result: SUCCESS, FAILURE
  - Factory methods: authEvent(), labEvent()

### Repository Interfaces (Ports — no implementation here)

UserRepository: save, findById, findByUsername, findByEmail,
                existsByUsername, existsByEmail, findAll, deleteById

LabRepository: save, findById, findByOwnerId, findByStatus,
               countActiveLabsByOwner, findRunningLabsIdleSince, deleteById

LabTemplateRepository: save, findById, findAllPublic, findByAuthorId,
                       existsByName, deleteById

ResourceQuotaRepository: save, findByUserId, findOrCreateDefault(userId)

AuditLogRepository: save (append-only), findByActorId,
                    findByLabId, findRecent(limit)

---

## 4. Application Layer

### Use Cases

AuthService (implements AuthUseCase)
  register()   -> validates uniqueness, BCrypt-hashes password, saves STUDENT,
                  writes audit log, returns JWT + refresh token
  login()      -> finds by username OR email, validates password,
                  writes LOGIN or LOGIN_FAILED audit event, returns JWT
  getProfile() -> returns UserResponse for the current user

LabService (implements LabUseCase)
  createLab()          -> check quota (all 4 dimensions), persist lab,
                          allocate quota, call EVE-NG createTopology().
                          On EVE-NG failure: rollback quota + mark ERROR + audit
  startLab()           -> validate STOPPED/PENDING state, EVE-NG start,
                          mark RUNNING, write LAB_STARTED audit
  stopLab()            -> validate RUNNING state, EVE-NG stop,
                          mark STOPPED, write LAB_STOPPED audit
  deleteLab()          -> validate STOPPED/ERROR state, EVE-NG delete,
                          release quota, write LAB_DELETED audit
  cloneLab()           -> source must be STOPPED, quota check for clone,
                          EVE-NG deep copy, write LAB_CLONED audit.
                          On failure: rollback quota + mark ERROR
  getNodeConsoleInfo() -> lab must be RUNNING, fetches console
                          details from EVE-NG (protocol, host, port, wsUrl)
  getLabsByOwner()     -> filters out DELETED labs
  getLabById()         -> ownership check (ADMIN bypasses)
  getAllLabs()          -> ADMIN only

LabTemplateService (implements LabTemplateUseCase)
  createTemplate()     -> INSTRUCTOR/ADMIN, validates unique name
  getPublicTemplates() -> all isPublic=true templates
  getMyTemplates()     -> templates by current user
  getTemplateById()    -> single template by ID
  deleteTemplate()     -> author or ADMIN only

AuditLogService
  All methods are @Async -> audit writes never slow down HTTP requests
  Failures are swallowed and logged as WARN, never propagated
  logLogin(), logLoginFailed() (truncates email to @domain for PII protection)
  logTokenRefresh(), logLogout()
  logLabEvent(), logLabEventFailure()

RefreshTokenService
  issueRefreshToken() -> 256-bit random token, revokes all existing tokens first
  rotate()            -> validate token, revoke it, issue new access+refresh pair
                         (token rotation per RFC 6749 section 10.4)
                         Replay detection: revoked token -> full session wipe + warning
  revokeAllForUser()  -> called on logout
  purgeExpiredTokens()-> @Scheduled nightly at 3am

### DTOs

Request (all validated with @Valid):
  RegisterRequest    -> username (3-50, pattern), email, password (8-100)
  LoginRequest       -> usernameOrEmail, password
  CreateLabRequest   -> name, description, templateId, cpu(1-32), ram(1-128), storage(10-500)
  CloneLabRequest    -> name, description
  CreateLabTemplateRequest -> name, description, topologyYaml, version, isPublic
  UpdateQuotaRequest -> maxLabs(1-50), maxCpu(1-128), maxRamGb(1-512), maxStorageGb(10-2000)

Response:
  ApiResponse<T>         -> { success, message, data, timestamp } (generic envelope)
  AuthResponse           -> accessToken, tokenType("Bearer"), expiresIn, userId,
                            username, email, role, refreshToken (null in body)
  UserResponse           -> id, username, email, role, enabled, createdAt
  LabResponse            -> id, name, description, status, ownerId, templateId,
                            evengLabId, cpuAllocated, ramAllocated, storageAllocated,
                            startedAt, stoppedAt, createdAt
  LabTemplateResponse    -> id, name, description, version, authorId, isPublic, createdAt
  QuotaResponse          -> max/used/remaining for labs, cpu, ramGb, storageGb + updatedAt
  AuditLogResponse       -> id, eventType, actorId, actorUsername, labId, labName,
                            result, errorCode, ipAddress, createdAt
  ErrorResponse          -> status, error, message, path, timestamp, fieldErrors[]

### Mappers (MapStruct — compile-time generated)
  UserMapper, LabMapper, LabTemplateMapper, ResourceQuotaMapper

---

## 5. Infrastructure Layer

### JPA Entities (one per domain entity)

  UserJpaEntity          -> table: users
  LabJpaEntity           -> table: labs (indexed on owner_id, status, eveng_lab_id)
  LabTemplateJpaEntity   -> table: lab_templates
  ResourceQuotaJpaEntity -> table: resource_quotas (UNIQUE on user_id)
  AuditLogJpaEntity      -> table: audit_logs (@Immutable: Hibernate cannot UPDATE)
  RefreshTokenJpaEntity  -> table: refresh_tokens (UNIQUE on token)

### Spring Data JPA Repositories

  UserJpaRepository           -> findByUsername, findByEmail, existsByUsername/Email
  LabJpaRepository            -> findByOwnerId, findByStatus,
                                 countActiveLabsByOwner (JPQL),
                                 findRunningLabsIdleSince (JPQL)
  LabTemplateJpaRepository    -> findByIsPublicTrue, findByAuthorId, existsByName
  ResourceQuotaJpaRepository  -> findByUserId
  AuditLogJpaRepository       -> findByActorId, findByLabId, findRecent (with PageRequest)
  RefreshTokenJpaRepository   -> findByToken, revokeAllForUser, deleteExpiredTokens

### Adapters (implement domain repository ports)

  UserRepositoryAdapter, LabRepositoryAdapter, LabTemplateRepositoryAdapter,
  ResourceQuotaRepositoryAdapter, AuditLogRepositoryAdapter
  Each adapter translates domain entity <-> JPA entity via MapStruct mappers.

### EVE-NG Integration

EveNgService interface:
  createTopology(lab)               -> EveNgLabResult
  startLab(evengLabId)              -> void
  stopLab(evengLabId)               -> void
  deleteLab(evengLabId)             -> void
  cloneLab(sourceId, name, cloneId) -> EveNgCloneResult
  getLabNodeStatuses(evengLabId)    -> List<EveNgNodeStatus>
  getNodeConsoleInfo(labId, nodeId) -> EveNgNodeConsoleInfo

EveNgRestService  (eveng.simulation-mode=false) -> real WebClient HTTP calls
EveNgSimulatedService (eveng.simulation-mode=true) -> in-memory simulation

EVE-NG API mappings:
  createTopology -> POST /api/labs
  startLab       -> PUT  /api/labs/{id}/nodes/start
  stopLab        -> PUT  /api/labs/{id}/nodes/stop
  deleteLab      -> DELETE /api/labs/{id}
  cloneLab       -> GET /api/labs/{id}/export + POST /api/labs/import
  getNodeConsole -> GET /api/labs/{id}/nodes/{nodeId}

EveNgNodeConsoleInfo: protocol (TELNET/VNC/SPICE), host, port,
                      webSocketUrl, nodeId, nodeName, nodeStatus

---

## 6. Presentation Layer — REST API

Base URL: http://localhost:8080/api
All responses: { success, message, data, timestamp }

### AuthController /auth

  POST /auth/register  -> 201 Created, JWT in body, refresh token in HttpOnly cookie
  POST /auth/login     -> 200 OK,      JWT in body, refresh token in HttpOnly cookie
  POST /auth/refresh   -> 200 OK,      rotates token (reads cookie, sets new cookie)
  POST /auth/logout    -> 200 OK,      revokes all tokens, clears cookie
  GET  /auth/me        -> 200 OK,      current user profile (requires JWT)

### LabController /labs

  GET    /labs                           -> list own labs (ADMIN sees all)
  POST   /labs                           -> create lab (quota checked)
  GET    /labs/{id}                      -> get lab detail
  POST   /labs/{id}/start                -> start lab via EVE-NG
  POST   /labs/{id}/stop                 -> stop lab via EVE-NG
  POST   /labs/{id}/clone                -> deep-copy lab (source must be STOPPED)
  DELETE /labs/{id}                      -> delete lab (must be STOPPED or ERROR)
  GET    /labs/{id}/nodes/{nodeId}/console -> get console URL (lab must be RUNNING)
  GET    /labs/admin/all                 -> ADMIN: all labs platform-wide

### LabTemplateController /templates

  GET    /templates                      -> all public templates
  GET    /templates/mine                 -> my authored templates
  GET    /templates/{id}                 -> single template
  POST   /templates                      -> create (INSTRUCTOR or ADMIN only)
  DELETE /templates/{id}                 -> delete (author or ADMIN only)

### AdminController /admin  [ADMIN only]

  GET    /admin/users                    -> list all users
  GET    /admin/users/{id}               -> get user by ID
  PATCH  /admin/users/{id}/role          -> change user role
  DELETE /admin/users/{id}               -> disable user account
  GET    /admin/users/{id}/quota         -> view user quota
  PUT    /admin/users/{id}/quota         -> override user quota limits

### QuotaController /quota

  GET    /quota/me                       -> my quota (max/used/remaining)

### AuditLogController /audit  [ADMIN only]

  GET    /audit/recent?limit=100         -> most recent N audit entries
  GET    /audit/user/{userId}            -> all entries for a user
  GET    /audit/lab/{labId}              -> all entries for a lab

---

## 7. Security

JWT Authentication:
  Algorithm : HMAC-SHA256 (HS256)
  Secret    : min 32 bytes enforced at startup (@PostConstruct)
  Expiry    : 24 hours (configurable via JWT_EXPIRATION_MS)
  Filter    : JwtAuthenticationFilter runs once per request
  Stateless : no HTTP session

Refresh Token System:
  256-bit cryptographically random token (SecureRandom)
  Stored in DB, delivered via HttpOnly + Secure cookie
  Rotated on every use (old token immediately revoked)
  Replay detection: revoked token -> revoke all sessions + warning log
  Nightly DB cleanup of expired tokens

Role-Based Access Control:
  ADMIN      -> full platform access, user mgmt, quota override, audit log
  INSTRUCTOR -> own labs + create/manage own templates
  STUDENT    -> own labs + read public templates
  RESEARCHER -> own labs + elevated quota (assigned by ADMIN)

Password Security:
  BCrypt strength 12 (SRS NFR-SEC-02)
  Never returned in any API response
  UserResponse DTO excludes password field entirely

CORS:
  Allowed origins: localhost:3000, localhost:5173, *.mnco.internal
  X-Trace-Id header exposed to clients

---

## 8. Exception Handling

GlobalExceptionHandler maps all exceptions to structured HTTP responses:

  ResourceNotFoundException    -> 404 Not Found
  DuplicateResourceException   -> 409 Conflict
  InvalidCredentialsException  -> 401 Unauthorized
  UnauthorizedException        -> 403 Forbidden
  QuotaExceededException       -> 422 Unprocessable Entity
  InvalidLabStateException     -> 422 Unprocessable Entity
  EveNgIntegrationException    -> 502 Bad Gateway
  MethodArgumentNotValidException -> 400 Bad Request (with field errors list)
  Exception (catch-all)        -> 500 Internal Server Error (safe message)

---

## 9. Cross-Cutting Concerns

LoggingAspect (AOP)
  Intercepts every method in application/usecases/**
  Logs method name, execution time in ms, and any exceptions
  Applied declaratively: zero pollution of business logic code

RequestLoggingFilter (FR-MO-03)
  Runs on every HTTP request (skips /actuator/health and /actuator/info)
  Emits one structured JSON log line per request/response:
  {
    "timestamp": "...", "level": "INFO", "traceId": "abc123def456",
    "httpMethod": "POST", "requestUri": "/labs",
    "userId": "anas", "httpStatus": 201, "durationMs": 142
  }
  X-Trace-Id header set in every response for client-side correlation

LabIdleStopScheduler
  Runs every 15 minutes (@Scheduled fixedDelay)
  Stops any RUNNING lab idle for more than 120 minutes (configurable)
  Calls EVE-NG stop -> marks STOPPED -> releases quota
  Failed auto-stops: mark ERROR, log warning (never crashes the scheduler)

AuditLogService (@Async)
  All writes are non-blocking (Spring @Async thread pool)
  Audit failures are caught and logged as WARN, never propagated to caller
  Covers: LOGIN, LOGIN_FAILED, TOKEN_REFRESH, LOGOUT,
          LAB_CREATED, LAB_STARTED, LAB_STOPPED, LAB_CLONED, LAB_DELETED

---

## 10. Database

Flyway Migrations (src/main/resources/db/migration/)

V1 — V1__init_schema.sql
  Tables: users, labs, lab_templates, resources, resource_quotas,
          refresh_tokens, audit_logs
  All primary keys: UUID (gen_random_uuid())
  All timestamps: TIMESTAMPTZ (timezone-aware)
  Cascading deletes on user removal
  Indexes: labs(owner_id), labs(status), labs(eveng_lab_id),
           refresh_tokens(token), refresh_tokens(user_id),
           audit_logs(actor_id), audit_logs(lab_id), audit_logs(created_at)
  updated_at auto-trigger function for users, labs, lab_templates, resource_quotas

V2 — V2__seed_admin.sql
  Default admin: username=admin, password=Admin@1234 (BCrypt-12 hash)
  2 public lab templates: Basic Router-Switch Lab, OSPF Multi-Area Lab
  Uses ON CONFLICT DO NOTHING (safe to re-run)

V3 — V3__audit_and_refresh_tables.sql
  Immutability rules for audit_logs at PostgreSQL level:
    CREATE RULE audit_log_no_update -> blocks all UPDATE statements
    CREATE RULE audit_log_no_delete -> blocks all DELETE statements
  Additional performance indexes on audit_logs(event_type)
  cleanup_expired_refresh_tokens() stored function

---

## 11. Tests

Unit Tests (no Spring context, pure Mockito)

AuthServiceTest (6 test cases)
  register: success, duplicate username -> 409, duplicate email -> 409
  login: success by username, success by email fallback,
         wrong password -> 401, unknown user -> 401, disabled account -> 401

LabServiceTest (11 test cases)
  createLab: success + quota allocated, lab quota full -> 422,
             CPU quota exceeded -> 422, EVE-NG failure -> quota rollback + ERROR
  startLab:  success, already RUNNING -> 422, not owner -> 403, not found -> 404
  stopLab:   success, already STOPPED -> 422
  deleteLab: success + quota released, lab is RUNNING -> 422
  getLabsByOwner: DELETED labs are filtered out

JwtServiceTest (7 test cases)
  generate token, extract username, extract role,
  valid token, tampered token -> false, garbage -> false, short secret -> exception

ResourceQuotaTest (7 test cases, pure domain logic)
  canAllocate: within limits, lab count maxed, CPU exceeded, RAM exceeded
  allocate: increments all counters
  release: decrements all counters, never goes negative
  remaining: correct calculations after allocation

Integration Tests (full Spring context, H2 in-memory, EVE-NG simulation)

AuthControllerIT (8 test cases)
  POST /auth/register: 201, 409 duplicate, 400 validation errors
  POST /auth/login: 200 with JWT, 401 wrong password, 401 unknown user
  GET  /auth/me: 200 with profile, 403 without token

LabControllerIT (11 ordered test cases)
  Full lab lifecycle in order:
  create (201) -> list (200) -> get by id (200, 404 random id) ->
  start (200 RUNNING) -> start again (422 already running) ->
  stop (200 STOPPED) -> delete (200) -> get after delete (404)

Test infrastructure:
  BaseIntegrationTest: shared @SpringBootTest with H2, simulation mode, test JWT
  application-test.yml: H2 in-memory, Flyway disabled, simulation=true

---

## 12. Build & Deploy

pom.xml — Key Dependencies
  spring-boot-starter-web, data-jpa, security, validation, webflux, aop, actuator
  jjwt-api + jjwt-impl + jjwt-jackson v0.12.5
  mapstruct + mapstruct-processor v1.5.5 (compile-time)
  postgresql driver, flyway-core + flyway-database-postgresql
  lombok, h2 (test scope)

Dockerfile
  Stage 1: eclipse-temurin:21-jdk-alpine -> mvn package -DskipTests
  Stage 2: eclipse-temurin:21-jre-alpine -> copy JAR, run as non-root user mnco
  JVM flags: UseContainerSupport, MaxRAMPercentage=75, UseG1GC

docker-compose.yml
  postgres: PostgreSQL 15 Alpine, health check, persistent volume
  backend: depends on postgres health, environment variables, log volume
  Default mode: EVENG_SIMULATION=true (no real EVE-NG needed)

Environment Variables
  DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD
  JWT_SECRET (min 32 bytes) / JWT_EXPIRATION_MS (default 86400000 = 24h)
  JWT_REFRESH_EXPIRATION_MS (default 604800000 = 7 days)
  EVENG_BASE_URL / EVENG_USERNAME / EVENG_PASSWORD / EVENG_SIMULATION
  SERVER_PORT (default 8080)

---

## 13. Quick Start

# Start full stack (PostgreSQL + backend, EVE-NG simulated)
docker compose up -d

# Check health
curl http://localhost:8080/api/actuator/health

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"anas","email":"anas@mnco.dev","password":"SecurePass1!"}'

# Login (save the accessToken)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"anas","password":"SecurePass1!"}'

# Create a lab
curl -X POST http://localhost:8080/api/labs \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"My OSPF Lab","cpu":2,"ram":4,"storage":20}'

# Start the lab
curl -X POST http://localhost:8080/api/labs/<labId>/start \
  -H "Authorization: Bearer <token>"

# Get console URL for a node
curl http://localhost:8080/api/labs/<labId>/nodes/1/console \
  -H "Authorization: Bearer <token>"

Default admin credentials: admin / Admin@1234

---

## 14. Full API Reference

Lab State Machine:
  PENDING -> CREATING -> STOPPED -> RUNNING -> STOPPING -> STOPPED
                 |                     |
                 +---> ERROR <----------+
  STOPPED -> DELETING -> DELETED

Console Info Response (FR-LM-09):
  { "protocol": "TELNET", "host": "192.168.1.100", "port": 32769,
    "webSocketUrl": "ws://192.168.1.100:8080/console/1",
    "nodeId": "1", "nodeName": "Router-1", "nodeStatus": "RUNNING" }

Quota Response:
  { "maxLabs": 3, "usedLabs": 1, "remainingLabs": 2,
    "maxCpu": 8,  "usedCpu": 2,  "remainingCpu": 6,
    "maxRamGb": 16, "usedRamGb": 4, "remainingRamGb": 12,
    "maxStorageGb": 50, "usedStorageGb": 20, "remainingStorageGb": 30 }

Error Response (validation):
  { "status": 400, "error": "Validation Failed",
    "message": "Validation failed", "path": "/auth/register",
    "timestamp": "...",
    "fieldErrors": [{ "field": "username", "message": "Username is required" }] }

---

## 15. SRS Coverage

FR-AA Authentication:
  FR-AA-01 Register endpoint          -> POST /auth/register         DONE
  FR-AA-02 Login endpoint             -> POST /auth/login            DONE
  FR-AA-03 RBAC (4 roles)             -> SecurityConfig + @PreAuthorize DONE
  FR-AA-04 Refresh token rotation     -> POST /auth/refresh          DONE
  FR-AA-07 Auth audit logging         -> AuditLogService             DONE
  FR-AA-08 All endpoints protected    -> SecurityConfig              DONE

FR-LM Lab Lifecycle:
  FR-LM-01 Create lab                 -> POST /labs                  DONE
  FR-LM-02 List labs                  -> GET /labs                   DONE
  FR-LM-03 Start lab                  -> POST /labs/{id}/start       DONE
  FR-LM-04 Stop lab                   -> POST /labs/{id}/stop        DONE
  FR-LM-05 Delete lab                 -> DELETE /labs/{id}           DONE
  FR-LM-06 Clone lab                  -> POST /labs/{id}/clone       DONE
  FR-LM-09 Node console URL           -> GET /labs/{id}/nodes/{n}/console DONE
  FR-LM-10 Lab lifecycle audit log    -> AuditLogService             DONE

FR-RM Resource Management:
  FR-RM-01 Quota entity per user      -> ResourceQuota + migration   DONE
  FR-RM-02 Quota enforcement          -> LabService.createLab()      DONE
  FR-RM-04 Auto-stop idle labs        -> LabIdleStopScheduler        DONE
  FR-RM-05 GET quota endpoint         -> GET /quota/me               DONE

FR-MT Multi-Tenancy:
  FR-MT-01 UUID-based lab isolation   -> Lab.ownerId + ownership checks DONE
  FR-MT-03 Tenant-scoped queries      -> JPA WHERE owner_id = :id    DONE
  FR-MT-04 404 on cross-access        -> getLabById() ownership check DONE
  FR-MT-05 Admin endpoint RBAC        -> @PreAuthorize hasRole ADMIN  DONE

FR-TM Templates:
  FR-TM-01 Template entity/catalog    -> LabTemplate + /templates    DONE
  FR-TM-03 INSTRUCTOR create template -> POST /templates (authorized) DONE
  FR-TM-04 Deploy from template       -> templateId in CreateLabRequest DONE

FR-MO Monitoring:
  FR-MO-01 Health check               -> /actuator/health            DONE
  FR-MO-03 Structured JSON logging    -> RequestLoggingFilter        DONE
  FR-MO-04 Prometheus metrics         -> /actuator/metrics           DONE

NFR Security:
  NFR-SEC-02 BCrypt strength 12       -> SecurityConfig.passwordEncoder() DONE
  NFR-SEC-04 Parameterized queries    -> Spring Data JPA             DONE

NFR Maintainability:
  NFR-MNT-03 Flyway migrations        -> V1, V2, V3 migration files  DONE

---

## 16. Future Work

The following requirements are planned for future implementation:

FR-AA-05 Rate limiting on /auth/login (block IP after 5 fails in 10min)
  -> Needs Bucket4j or Spring Cloud Gateway rate limiter
  -> Priority: HIGH

FR-AA-06 Password reset via email
  -> Generate SHA-256 token, store hash, 15-min expiry, send email link
  -> Priority: MEDIUM

FR-LM-07 Enriched lab detail response
  -> Include all nodes, interfaces, console ports in GET /labs/{id}
  -> Priority: HIGH

FR-LM-08 Per-node start/stop
  -> POST /labs/{id}/nodes/{nodeId}/start and /stop
  -> Priority: MEDIUM

FR-OE-03 Dedicated async thread pool for orchestration
  -> corePoolSize=10, maxPoolSize=50, queueCapacity=200
  -> Priority: HIGH

FR-OE-04 JSON Schema validation of topology definitions
  -> Validate before any EVE-NG call
  -> Priority: HIGH

FR-OE-05 Retry with exponential backoff on EVE-NG failures
  -> maxAttempts=3, initialInterval=2000ms, multiplier=2
  -> For HTTP 429, 500, 502, 503, 504
  -> Priority: MEDIUM

FR-OE-06 30-second EVE-NG status polling cache
  -> Avoid hammering EVE-NG under concurrent users
  -> Priority: MEDIUM

FR-RM-03 5-minute resource usage snapshots
  -> Per running lab, for billing and capacity planning
  -> Priority: MEDIUM

FR-MT-02 EVE-NG bridge isolation verification
  -> Platform-level check that each lab gets unique bridges
  -> Priority: HIGH

FR-TM-02 4 factory-seeded templates (currently 2)
  -> Add BGP Lab and Firewall/DMZ Lab templates
  -> Priority: MEDIUM

FR-TM-05 Template version auto-increment on PUT /templates/{id}
  -> Store previous version for rollback
  -> Priority: LOW

FR-MO-02 Admin metrics dashboard endpoint
  -> Active labs count, total CPU/RAM in use, user counts by role
  -> Priority: MEDIUM

---

Generated for MNCO PFE Project
Multi-Tenant Network & Cybersecurity Lab Orchestrator
Architecture: Clean Architecture
Stack: Java 21 + Spring Boot 3.2 + PostgreSQL 15 + EVE-NG API v2
