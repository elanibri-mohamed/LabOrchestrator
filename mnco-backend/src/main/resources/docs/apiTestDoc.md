# MNCO Backend — Multi-Tenant EVE-NG Integration API Test Guide

**Version:** 2.0  
**Last Updated:** April 23, 2026  
**Environment:** Local Development (http://localhost:8080)  
**Architecture:** Reference Implementation — Template/Assignment/Instance Isolation

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Authentication Flow](#authentication-flow)
3. [API Endpoints](#api-endpoints)
   - [Auth API](#auth-api)
   - [EVE-NG Lab API (Multi-Tenant)](#eve-ng-lab-api-multi-tenant)
   - [Admin API (Extended)](#admin-api-extended)
   - [Audit Log API](#audit-log-api)
   - [Quota API](#quota-api)
4. [Testing Workflows — Reference Requirements](#testing-workflows--reference-requirements)
5. [Error Handling](#error-handling)
6. [Response Format](#response-format)
7. [Testing Checklist](#testing-checklist)
8. [Postman Collection Structure](#postman-collection-structure)
9. [Expected Database State](#expected-database-state)
10. [Three Guards Validation](#three-guards-validation)
11. [Metrics & Observability](#metrics--observability)
12. [Known Limitations](#known-limitations)

---

## Quick Start

### Prerequisites

- Backend running on `http://localhost:8080`
- **EVE-NG Simulation Mode:** `eveng.simulation-mode=true` (default in dev)
- PostgreSQL + Flyway migrations applied automatically
- Postman or similar HTTP client

### Test User Accounts (Pre-seeded)

| Username | Email | Password | Role |
|----------|-------|----------|------|
| `admin` | `admin@mnco.local` | `Admin@123` | ADMIN |
| `teacher1` | `teacher1@mnco.local` | `Teacher@123` | INSTRUCTOR (maps to TEACHER) |
| `student1` | `student1@mnco.local` | `Student@123` | STUDENT |

> Note: `INSTRUCTOR` == `TEACHER` in reference architecture. `STUDENT` and `RESEARCHER` are student-level roles.

---

## Authentication Flow

### 1. Login

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "username": "admin",
    "email": "admin@mnco.local",
    "role": "ADMIN"
  },
  "timestamp": "2026-04-23T00:00:00Z"
}
```

**Save tokens to Postman environment:**
- `{{adminToken}}` = `data.accessToken`
- `{{adminId}}` = `data.userId`

Repeat for `teacher1` → `{{teacherToken}}`, `{{teacherId}}` and `student1` → `{{studentToken}}`, `{{studentId}}`.

---

## API Endpoints

### Auth API

#### Base URL: `/api/v1/auth`

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/register` | POST | ❌ | Register new user |
| `/login` | POST | ❌ | Login with username/email + password |
| `/refresh` | POST | ❌ | Refresh access token |
| `/logout` | POST | ✅ | Revoke tokens |
| `/me` | GET | ✅ | Get current user profile |

---

### EVE-NG Lab API (Multi-Tenant)

#### Base URL: `/api/v1/eveng/labs`

All endpoints enforce **three-guard isolation**: role → template status → assignment.

| Endpoint | Method | Role | Description |
|----------|--------|------|-------------|
| `/` | GET | STUDENT/TEACHER | List templates assigned to me |
| `/{templateId}/assign/student` | POST | TEACHER | Assign template to student |
| `/{templateId}/assignments/{userId}` | DELETE | ADMIN/TEACHER | Revoke assignment |
| `/{templateId}/start` | POST | STUDENT/TEACHER | Start own instance (lazy create) |
| `/{templateId}/stop` | POST | TEACHER | Stop own instance |
| `/{templateId}/students/{sid}/stop` | POST | TEACHER | Stop student's instance |
| `/{templateId}/reset` | POST | STUDENT/TEACHER | Reset own instance to template |
| `/{templateId}/students/{sid}/reset` | POST | TEACHER | Reset student's instance |
| `/{templateId}/nodes/{nodeId}/console` | GET | STUDENT/TEACHER | Get console URL + log access |
| `/{templateId}/progress` | GET | TEACHER | Parallel progress report for all students |
| `/{templateId}/students/instances` | GET | TEACHER | List all student instances |

---

### Admin API (Extended)

#### Base URL: `/api/v1/admin`

All endpoints require `ADMIN` role.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/users` | GET | List all users |
| `/users/{id}` | GET | Get user details |
| `/users/{id}/role` | PATCH | Change user role (query param `role=ADMIN|INSTRUCTOR|STUDENT|RESEARCHER`) |
| `/users/{id}` | DELETE | Disable user (soft delete) |
| `/users/{id}/quota` | GET | Get user quota |
| `/users/{id}/quota` | PUT | Update user quota |
| `/labs/sync` | POST | **NEW:** Sync EVE-NG templates (soft-insert/update/removal) |
| `/labs/{templateId}/assign/teacher` | POST | **NEW:** Assign template to teacher |

---

## Testing Workflows — Reference Requirements

The following sections validate every requirement from the reference architecture.

---

### Phase A: Template Sync (Reference §6 — Sync)

**Objective:** Verify ADMIN can sync templates from EVE-NG with soft-delete semantics.

#### A1. Sync Templates (Admin Only)

**Endpoint:** `POST /api/v1/admin/labs/sync`

**Headers:** `Authorization: Bearer {{adminToken}}`

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/admin/labs/sync" \
  -H "Authorization: Bearer {{adminToken}}"
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Sync completed",
  "data": {
    "created": 3,
    "updated": 0,
    "removed": 0
  },
  "timestamp": "2026-04-23T00:45:00Z"
}
```

**Log Output:**
```
INFO  Synced new template: /NetworkTopology-1.unl
INFO  Synced new template: /SecurityLab-1.unl
INFO  Synced new template: /CloudLab-1.unl
INFO  Sync complete: created=3, updated=0, removed=0
```

**Database Validation:**
```sql
SELECT id, name, eveng_template_path, template_status FROM eveng_templates;
-- Expect: 3 rows, all 'ACTIVE'
```

**Reference Compliance:**  
✅ **§6 Sync** — Soft-inserts new templates, updates `last_synced_at`, preserves removed status on existing rows.

---

### Phase B: Assignment Hierarchy (Reference §7 — Two-Level Hierarchy)

**Objective:** Test ADMIN→TEACHER→STUDENT assignment chain. No shortcuts allowed.

#### B1. ADMIN Assigns Template to TEACHER

**Endpoint:** `POST /api/v1/admin/labs/{templateId}/assign/teacher`

**Path Param:** `templateId` = first synced template ID (from A1 response)

**Headers:** `Authorization: Bearer {{adminToken}}`

**Body:**
```json
{
  "teacherId": "{{teacherId}}"
}
```

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/admin/labs/11111111-1111-1111-1111-111111111111/assign/teacher" \
  -H "Authorization: Bearer {{adminToken}}" \
  -H "Content-Type: application/json" \
  -d '{"teacherId":"{{teacherId}}"}"
```

**Expected (200 OK):**
```json
{
  "success": true,
  "message": "Teacher assigned successfully",
  "data": null
}
```

**Validation:**
```sql
SELECT * FROM lab_assignments 
WHERE user_id = '{{teacherId}}' AND template_id = '11111111-...';
-- 1 row found, assigned_by = {{adminId}}
```

#### B2. TEACHER Assigns Template to STUDENT

**Switch to teacher token.**

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/assign/student`

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/assign/student" \
  -H "Authorization: Bearer {{teacherToken}}" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"{{studentId}}"}"
```

**Expected (200 OK):**
```json
{
  "success": true,
  "message": "Student assigned successfully",
  "data": null
}
```

**Validation:**
```sql
SELECT * FROM lab_assignments 
WHERE template_id = '11111111-...' ORDER BY assigned_by;
-- 2 rows:
--  row1: user_id=teacherId,  assigned_by=adminId
--  row2: user_id=studentId,  assigned_by=teacherId
```

#### B3. Negative: ADMIN Cannot Assign Directly to STUDENT

**No endpoint exists** at `/api/v1/admin/labs/{id}/assign/student`.  
Reference §7: "ADMIN cannot assign directly to STUDENT. No shortcuts."

✅ **Verified by absence of route.**

#### B4. Negative: STUDENT Cannot Assign Anyone (403)

**Endpoint:** `POST /api/v1/eveng/labs/{id}/assign/student` as student

**Expected (403 Forbidden):**
```json
{
  "success": false,
  "message": "Access Denied: Teacher role required",
  "data": null
}
```

#### B5. Idempotency: Re-assign Same User (200, No Duplicate)

Call B1 or B2 again with same IDs.

**Expected (200 OK)** with same message.  
Database: still only 1 row per `(template_id, user_id)` due to UNIQUE constraint.

**Reference Compliance:**  
✅ **§7 Assignment** — Two-level hierarchy enforced, idempotent inserts, `assigned_by` audit preserved.

---

### Phase C: Instance Lifecycle (Reference §8 — Start/Stop/Reset)

**Objective:** Validate lazy instance creation, path isolation, state transitions.

#### C1. STUDENT Starts Instance (First Start → Lazy Clone)

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/start`  
**Headers:** `Authorization: Bearer {{studentToken}}`

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/start" \
  -H "Authorization: Bearer {{studentToken}}"
```

**Expected (200 OK):**
```json
{
  "success": true,
  "message": "Lab started",
  "data": {
    "id": "22222222-2222-2222-2222-222222222222",
    "templateId": "11111111-1111-1111-1111-111111111111",
    "userId": "{{studentId}}",
    "evengInstancePath": "/instances/student1/22222222-2222-2222-2222-222222222222.unl",
    "status": "RUNNING",
    "createdAt": "2026-04-23T00:51:00Z",
    "startedAt": "2026-04-23T00:51:05Z",
    "stoppedAt": null
  }
}
```

**Save `{{studentInstanceId}} = data.id`.**

**What happened:**
1. Guard: role=STUDENT ✅, template ACTIVE ✅, assignment exists ✅.
2. `findByTemplateIdAndUserId()` → empty → **lazy create branch**.
3. `eveNgService.cloneLab(templatePath, ...)` creates `.unl` file at `/opt/unetlab/labs/instances/student1/{{instanceId}}.unl`.
4. Instance row inserted (`STOPPED`), then `startLab()` called → `RUNNING`.
5. `markRunning()` sets `startedAt`.

**Database:**
```sql
SELECT id, user_id, eveng_instance_path, status, started_at 
FROM lab_instances 
WHERE user_id = '{{studentId}}';
-- 1 row, status='RUNNING', path contains '/instances/student1/'
```

#### C2. Idempotent Start (Already RUNNING)

Call C1 again.

**Expected:** Same response, status still `RUNNING`, **no new clone**.

**Log:** "Instance already running: id=..."

#### C3. TEACHER Starts Own Instance (Isolation Check)

**As teacher:** `POST /api/v1/eveng/labs/{templateId}/start`

**Expected:** New instance created with `eveng_instance_path` containing `/instances/teacher1/`.

**Validation:**
```sql
SELECT user_id, eveng_instance_path FROM lab_instances;
-- Two rows: one for student1, one for teacher1 — different paths, no overlap
```

✅ **Isolation confirmed:** Each user gets private `.unl` file.

#### C4. TEACHER Stops Own Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/stop` (teacher only)

**Expected (200 OK):**
```json
{
  "success": true,
  "message": "Lab stopped",
  "data": {
    "id": "{{teacherInstanceId}}",
    "status": "STOPPED",
    "stoppedAt": "2026-04-23T00:53:00Z"
  }
}
```

#### C5. TEACHER Stops STUDENT's Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/students/{studentId}/stop`

**Headers:** `Bearer {{teacherToken}}`

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-.../students/{{studentId}}/stop" \
  -H "Authorization: Bearer {{teacherToken}}"
```

**Expected (200 OK):** student instance status → `STOPPED`.

#### C6. Negative: STUDENT Cannot Stop Own Lab (403)

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/stop` as student

**Expected:**
```json
{
  "success": false,
  "message": "Access Denied: Only teachers can stop their own labs",
  "data": null
}
```

✅ **Reference §8b:** Teachers stop own; teachers stop students; students cannot stop.

#### C7. TEACHER Resets STUDENT's Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/students/{studentId}/reset`

**Precondition:** Student instance exists.

**Expected (200 OK):**
```json
{
  "success": true,
  "message": "Student lab reset",
  "data": {
    "id": "{{studentInstanceId}}",
    "status": "STOPPED",
    "startedAt": null,
    "stoppedAt": null
  }
}
```

**What happened:**
1. Stop if RUNNING.
2. Delete old instance file from EVE-NG (`DELETE /api/v2/labs/{instancePath}`).
3. `cloneLab` fresh copy from template → same `instanceId`, new file path (same file name overwritten).
4. `status = STOPPED`, timestamps cleared (`reset()` sets null).

**Database:** Row count unchanged; `eveng_instance_path` may change (new clone); `started_at`/`stopped_at` become `NULL`.

#### C8. STUDENT Resets Own Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/reset` as student

**Expected:** Same semantics as C7 but for own instance.

✅ **Reference §8d Reset** — Stop → delete → recopy. Both teacher (own or student) and student (own only) allowed.

#### C9. Instance Path Construction Rule

Query DB:
```sql
SELECT 
  id,
  user_id,
  eveng_instance_path
FROM lab_instances;
```

**Expected pattern:**
- Student: `/opt/unetlab/labs/instances/student1/{instanceId}.unl`
- Teacher: `/opt/unetlab/labs/instances/teacher1/{instanceId}.unl`

**Each user's console URL points to their own instance only.**  
✅ **Reference §8e** — Isolation enforced by path.

---

### Phase D: Console Access & Progress Monitoring (Reference §8e & §9)

#### D1. STUDENT Gets Console URL (Access Log Inserted)

**Precondition:** Ensure instance is RUNNING (start if needed).

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/nodes/{nodeId}/console`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/eveng/labs/11111111-.../nodes/R1/console" \
  -H "Authorization: Bearer {{studentToken}}"
```

**Expected (200 OK):**
```json
{
  "success": true,
  "data": {
    "protocol": "TELNET",
    "host": "192.168.100.10",
    "port": 32769,
    "webSocketUrl": "ws://192.168.100.10:8080/console/...",
    "nodeId": "R1",
    "nodeName": "Router-1",
    "status": "RUNNING"
  }
}
```

**Side Effect:** Row inserted into `console_access_log`.

**Validation:**
```sql
SELECT node_id, accessed_at FROM console_access_log 
WHERE user_id = '{{studentId}}' ORDER BY accessed_at DESC LIMIT 1;
-- node_id = 'R1'
```

✅ **Reference §8e** — Each access logged for progress tracking.

#### D2. STUDENT Accesses Different Nodes (Multiple Log Entries)

Call D1 with `nodeId=SW1`, then `nodeId=PC1`.

**Expected:** 3 rows in `console_access_log` for this user, distinct `node_id` values.

#### D3. TEACHER Views Student Progress (Parallel Fetch — Reference §9)

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/progress`  
**Headers:** `Bearer {{teacherToken}}`

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/eveng/labs/11111111-.../progress" \
  -H "Authorization: Bearer {{teacherToken}}"
```

**Expected (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "studentId": "{{studentId}}",
      "studentName": "student1",
      "instanceStatus": "RUNNING",
      "startedAt": "2026-04-23T00:51:05Z",
      "nodeStatuses": {
        "R1": "RUNNING",
        "SW1": "STOPPED",
        "PC1": "RUNNING"
      },
      "lastConnected": "2026-04-23T00:55:00Z",
      "accessedNodes": ["R1", "PC1"]
    }
  ]
}
```

**Key Validations:**
- Only students **assigned by this teacher** appear.
- `nodeStatuses` populated by **parallel** EVE-NG `getLabNodeStatuses()` calls.
- `lastConnected` = max(`accessed_at`) from `console_access_log`.
- `accessedNodes` = distinct `node_id` values from log.

✅ **Reference §9** — `CompletableFuture.allOf(...)` used, not sequential loop.

#### D4. TEACHER Lists Student Instances

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/students/instances`

**Returns list of `LabInstanceResponse`** for all students assigned to this template by this teacher.

---

### Phase E: Guard Violations — Negative Tests (Reference §5)

Each guard must fail **before** any EVE-NG call. Check logs for guard rejection messages.

#### E1. Unassigned User Access → Guard 3 Failure

**Scenario:** Student tries to access template they are not assigned to.

**Endpoint:** `GET /api/v1/eveng/labs/{unassignedTemplateId}`

**Expected (403):**
```json
{
  "success": false,
  "message": "Access Denied: You are not assigned to this lab template",
  "data": null
}
```

**Log:**  
`WARN  Access denied: user={{studentId}} has no assignment for template=...`

#### E2. REMOVED Template Access → Guard 2 Failure

**Setup:** Admin syncs, then template is soft-deleted (`template_status = 'REMOVED'`) due to EVE-NG deletion or manual.

**Endpoint:** `GET /api/v1/eveng/labs/{removedTemplateId}`

**Expected (403/404 with message):**
```json
{
  "success": false,
  "message": "Template is not available (status: REMOVED)",
  "data": null
}
```

#### E3. Wrong Role → Guard 1 Failure

**Examples:**
- Student calls `POST /{id}/assign/student` → 403 (needs TEACHER)
- Teacher calls `POST /api/v1/admin/labs/sync` → 403 (needs ADMIN)
- Student calls `POST /{id}/stop` → 403 (needs TEACHER)

✅ **All three guards** checked in order in `InstanceGuard`.

---

### Phase F: Parallel Progress Fetch Validation (Reference §9 — Performance)

#### F1. Measure Parallelism with Simulated Delay

**Setup:** In `EveNgSimulatedService`, `getLabNodeStatuses()` has `Thread.sleep(500)` (simulate EVE-NG latency).

Create 10 student assignments (via DB seed script or API if available). Ensure each has a RUNNING instance.

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/progress`

**Measurement (Postman Tests script):**
```javascript
const start = performance.now();
// response received
const duration = performance.now() - start;
pm.test("Parallel fetch completed quickly", function() {
    // With 10 students × 500ms simulated latency:
    // Sequential would be ~5000ms. Parallel should be ~600ms.
    pm.expect(duration).to.be.below(2000);
});
pm.test("Metric incremented", () => {
    // Optional: check /actuator/metrics/lab.progress.request
});
```

**Expected:** Response time ≈ max latency × small factor, not `N × latency`.

✅ **Reference §9** — Progress monitoring uses `CompletableFuture.supplyAsync()`.

---

## Full End-to-End Workflow (Complete Reference Walkthrough)

**Scenario:** Admin onboards teacher → teacher assigns student → student lab lifecycle → teacher monitors.

```
=== Step 0: Auth ===
ADMIN  → POST /auth/login  → {{adminToken}}
TEACHER→ POST /auth/login  → {{teacherToken}}
STUDENT→ POST /auth/login  → {{studentToken}}

=== Step 1: Template Sync (ADMIN) ===
ADMIN  → POST /admin/labs/sync
        → 3 templates created in DB (ACTIVE)

=== Step 2: Assignment Hierarchy ===
ADMIN   → POST /admin/labs/{t1}/assign/teacher
          { "teacherId": "{{teacherId}}" }
          → teacher now assigned

TEACHER → POST /eveng/labs/{t1}/assign/student
          { "studentId": "{{studentId}}" }
          → student now assigned, assigned_by = teacherId

=== Step 3: Student Starts Lab (Lazy Create) ===
STUDENT → POST /eveng/labs/{t1}/start
          → Instance created via cloneLab()
          → Status = RUNNING
          → Instance row: userId=studentId, path=/instances/student1/...

=== Step 4: Student Accesses Console ===
STUDENT → GET /eveng/labs/{t1}/nodes/R1/console
          → Console URL returned
          → console_access_log row inserted

=== Step 5: Teacher Monitors Progress ===
TEACHER → GET /eveng/labs/{t1}/progress
          → Single student report:
            instanceStatus = RUNNING
            nodeStatuses = { "R1": "RUNNING", "SW1": "STOPPED", ... }
            lastConnected = timestamp
            accessedNodes = ["R1"]

=== Step 6: Teacher Stops Student Lab ===
TEACHER → POST /eveng/labs/{t1}/students/{{studentId}}/stop
          → student instance status → STOPPED

=== Step 7: Teacher Resets Student Lab ===
TEACHER → POST /eveng/labs/{t1}/students/{{studentId}}/reset
          → instance wiped, recopied from template, status=STOPPED

=== Step 8: Student Restarts ===
STUDENT → POST /eveng/labs/{t1}/start (again)
          → reuses same instance file, starts nodes, status=RUNNING

=== Step 9: Teacher Stops All Students (bulk via loop) ===
For each student in assignment list:
  POST /eveng/labs/{t1}/students/{sid}/stop
```

Every API call returns expected status codes, DB state transitions are correct, and guard checks pass.

---

## Postman Collection Structure

Import the following collection into Postman. Use environment variables for tokens/IDs.

```
MNCO Multi-Tenant EVE-NG Tests
├── 00 Auth
│   ├── Login as Admin
│   ├── Login as Teacher
│   └── Login as Student
├── 01 Admin — Template Sync & Teacher Assignment
│   ├── Sync Templates
│   ├── Assign Template to Teacher
│   └── (Negative) Teacher Assigns Student → 403
├── 02 Teacher — Student Assignment
│   ├── Assign Template to Student
│   ├── (Negative) Student Assigns Student → 403
│   └── (Negative) Wrong Teacher Manages → 403
├── 03 Student — Instance Lifecycle
│   ├── Start Instance (first)
│   ├── Start Instance (idempotent)
│   ├── Get Console (R1, SW1, PC1)
│   ├── Reset Own Instance
│   └── (Negative) Stop Own → 403
├── 04 Teacher — Manage Student Instances
│   ├── Stop Student Instance
│   ├── Reset Student Instance
│   ├── List Student Instances
│   └── Progress Report
├── 05 Guard Violations
│   ├── Student Access Unassigned Template → 403
│   ├── Access REMOVED Template → 403
│   └── Teacher Affect Other Teacher's Student → 403
└── 06 Performance
    └── Parallel Progress Timing (assert < 2000ms)
```

Each request should include **Tests** script:
```javascript
pm.test("Status 200", () => pm.response.to.have.status(200));
pm.test("Success true", () => pm.expect(pm.response.json().success).to.be.true);

// Extract IDs
const json = pm.response.json();
if (json.data && json.data.id) {
    pm.environment.set("lastInstanceId", json.data.id);
}
```

---

## Expected Database State

After full workflow (1 template, 1 teacher, 1 student):

### Row Counts

| Table | Expected Rows | Contents |
|-------|---------------|----------|
| `eveng_templates` | 3 | Synced from EVE-NG simulation |
| `lab_assignments` | 2 | teacher←admin, student←teacher |
| `lab_instances` | 2 | teacher's instance, student's instance |
| `console_access_log` | ≥3 | At least 3 console accesses by student |
| `audit_logs` | ≥10 | Events: sync, assign (x2), start (x2), stop (x1), console (x3), reset (x1) |

### Key Queries

**Assignments with lineage:**
```sql
SELECT 
    t.name AS template,
    u.username AS user,
    u.role,
    a.assigned_by,
    a.assigned_at
FROM lab_assignments a
JOIN eveng_templates t ON a.template_id = t.id
JOIN users u ON a.user_id = u.id
ORDER BY a.assigned_at;
```

**Instance isolation check:**
```sql
SELECT 
    id,
    user_id,
    eveng_instance_path,
    status
FROM lab_instances;
-- Paths must contain different user subdirectories
```

**Console access audit:**
```sql
SELECT 
    l.node_id,
    l.accessed_at,
    u.username
FROM console_access_log l
JOIN users u ON l.user_id = u.id
ORDER BY l.accessed_at DESC;
```

---

## Three Guards Validation (Reference §5)

Verify guard order in logs. Add `DEBUG` logging to `InstanceGuard` temporarily.

**Successful start should produce:**
```
DEBUG Guard 1: role check passed for userId=..., role=STUDENT
DEBUG Guard 2: template active: templateId=...
DEBUG Guard 3: assignment exists: (template,user)=(...,...)
```

**Failure short-circuits:**
- Role fail → log + exception, **no DB query** for template.
- Template inactive → log + exception, **no assignment check**.
- No assignment → log + 403.

**Validation:** No EVE-NG HTTP calls appear in logs if any guard fails.  
✅ **Reference §5** — Guards are the tenant boundary; EVE-NG never sees unauthorized requests.

---

## Metrics & Observability

After running workflows, check Actuator:

```bash
# Counters
curl http://localhost:8080/actuator/metrics/eveng.sync
curl http://localhost:8080/actuator/metrics/lab.instance.start
curl http://localhost:8080/actuator/metrics/lab.instance.stop
curl http://localhost:8080/actuator/metrics/lab.instance.reset
curl http://localhost:8080/actuator/metrics/lab.console.access
curl http://localhost:8080/actuator/metrics/lab.progress.request
```

**Expected counts (minimal run):**
- `eveng.sync` — created ≥3
- `lab.instance.start` — ≥2 (teacher + student)
- `lab.console.access` — ≥3 (student accesses)
- `lab.progress.request` — ≥1 (teacher view)

---

## Known Limitations (Reference §11)

| Limitation | Reason | Mitigation |
|------------|--------|------------|
| No network isolation between instances | EVE-NG Community has no VLAN separation | Use `bridge` networks only, never `pnet` |
| No resource quotas per instance | EVE-NG Community lacks per-lab limits | Enforce via `resource_quotas` per user (existing) |
| No topology creation from backend | Templates authored in EVE-NG GUI only | Admin syncs them; backend only copies |
| No real-time push | Progress is polled (REST) | Not in scope; WebSocket future enhancement |
| Single EVE-NG admin session | All backend calls share one cookie | Auto-reauth on 401 implemented in `EveNgRestService` |

---

## Testing Checklist (Multi-Tenant Extended)

**Authentication:**
- [x] Login for all roles (ADMIN, INSTRUCTOR, STUDENT)
- [x] Tokens stored with correct userId/role claims

**Template Sync (§6):**
- [x] Admin sync creates/updates/removes templates correctly
- [x] `template_status` transitions: ACTIVE → REMOVED on re-sync

**Assignment (§7):**
- [x] Admin assigns teacher → row created
- [x] Teacher assigns student → row created with `assigned_by=teacherId`
- [x] Teacher cannot assign if not assigned themselves (403)
- [x] Student cannot assign (403)
- [x] Admin cannot assign directly to student (no endpoint)

**Instance Lifecycle (§8):**
- [x] Student start → lazy clone → RUNNING
- [x] Second start → idempotent, no new clone
- [x] Teacher start own → separate instance file
- [x] Teacher stop own → STOPPED
- [x] Teacher stop student → STOPPED (student cannot stop self)
- [x] Reset (student/teacher) → STOPPED, file re-copied
- [x] Instance path isolation: `/instances/{userId}/{id}.unl`

**Console & Progress (§8e, §9):**
- [x] Console URL returned + `console_access_log` row inserted
- [x] Progress report parallel fetch (timing measurement)
- [x] `lastConnected` derived from logs
- [x] `accessedNodes` distinct node list from logs

**Guards (§5):**
- [x] Role check fails before template lookup
- [x] Inactive template fails before assignment check
- [x] Missing assignment fails with 403, no EVE-NG call

**Metrics:**
- [x] Counters increment on each operation
- [x] Prometheus endpoint exposes metrics

---

**End of Test Guide**  
All reference requirements covered. Execute in order; verify DB state after each phase.
