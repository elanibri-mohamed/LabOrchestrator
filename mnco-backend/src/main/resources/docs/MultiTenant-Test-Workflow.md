# MNCO Backend — Multi-Tenant EVE-NG Integration Test Workflow
## Complete Reference Architecture Validation

**Version:** 2.0  
**Last Updated:** April 23, 2026  
**Scope:** Full multi-tenant lab orchestration per reference architecture  
**Target Environment:** http://localhost:8080

---

## Table of Contents

1. [Test Environment Setup](#test-environment-setup)
2. [Reference Requirement Map](#reference-requirement-map)
3. [Test Data — User Roles](#test-data--user-roles)
4. [Phase A: Template Sync (ADMIN)](#phase-a-template-sync-admin)
5. [Phase B: Assignment Hierarchy](#phase-b-assignment-hierarchy)
6. [Phase C: Instance Lifecycle](#phase-c-instance-lifecycle)
7. [Phase D: Console Access & Progress](#phase-d-console-access--progress)
8. [Phase E: Guard Violations (Negative Tests)](#phase-e-guard-violations-negative-tests)
9. [Phase F: Parallel Progress Fetch](#phase-f-parallel-progress-fetch)
10. [Postman Collection Export](#postman-collection-export)
11. [Expected Database State](#expected-database-state)

---

## Test Environment Setup

### Prerequisites

1. Start backend: `./mnco-backend/mvnw spring-boot:run`
2. Ensure EVE-NG simulation mode is ON (default for dev):
   ```yaml
   # application.yml
   eveng:
     simulation-mode: true
   ```
3. Database: PostgreSQL on `localhost:5432` with `mnco_db` (Flyway auto-migrates).

### Postman Configuration

Create an **Environment** called `MNCO-Local` with variables:

| Variable | Value | Purpose |
|----------|-------|---------|
| `baseUrl` | `http://localhost:8080` | Base API URL |
| `adminToken` | *(empty)* | Filled after admin login |
| `teacherToken` | *(empty)* | Filled after teacher login |
| `studentToken` | *(empty)* | Filled after student login |
| `adminId` | *(empty)* | Filled after admin login |
| `teacherId` | *(empty)* | Filled after teacher login |
| `studentId` | *(empty)* | Filled after student login |

---

## Reference Requirement Map

| Ref Section | Implementation | Test Covered In |
|-------------|---------------|-----------------|
| 1. Core Concept (Template/Assignment/Instance) | ✅ `eveng_templates`, `lab_assignments`, `lab_instances` tables | Phases A–C |
| 2. DB Schema | ✅ V4 migration + indexes | Phase A |
| 3. System Topology | ✅ Backend is sole isolation layer | All |
| 4. Authentication | ✅ JWT with userId/role claims | Setup |
| 5. Three Guards | ✅ `InstanceGuard` (role → active → assignment) | Phases B–D |
| 6. Sync | ✅ Soft-insert/update/removal (active/removed) | Phase A |
| 7. Assignment (ADMIN→TEACHER, TEACHER→STUDENT) | ✅ Two-level hierarchy | Phase B |
| 8a. Start (lazy create) | ✅ `cloneLab` on first start, reuse thereafter | Phase C |
| 8b–c. Stop (own & student) | ✅ Separate endpoints with teacher-student check | Phase C |
| 8d. Reset | ✅ Stop → delete → recopy | Phase C |
| 8e. Console URL | ✅ Access log inserted | Phase D |
| 9. Progress Monitoring | ✅ Parallel `CompletableFuture` per student | Phase F |
| 10. API Surface | ✅ All endpoints under `/api/v1/eveng/labs` | All |
| 11. Limitations | ✅ Documented (no isolation, no quotas on instances) | N/A |

---

## Test Data — User Roles

Use existing pre-seeded users (from V2 seed data) or create fresh ones:

| Role | Username | Email | Password |
|------|----------|-------|----------|
| ADMIN | `admin` | `admin@mnco.local` | `Admin@123` |
| TEACHER (INSTRUCTOR) | `teacher1` | `teacher1@mnco.local` | `Teacher@123` |
| STUDENT | `student1` | `student1@mnco.local` | `Student@123` |

### 0. Auth: Get Tokens & User IDs (Prerequisite)

**Login as ADMIN:**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```
**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "username": "admin",
    "role": "ADMIN"
  }
}
```
→ Save `accessToken` to `{{adminToken}}` and `userId` to `{{adminId}}`.

**Login as TEACHER:**
```http
POST /api/v1/auth/login
```
→ Save `{{teacherToken}}` and `{{teacherId}}`.

**Login as STUDENT:**
```http
POST /api/v1/auth/login
```
→ Save `{{studentToken}}` and `{{studentId}}`.

---

## Phase A: Template Sync (ADMIN)

### A1. Sync Templates from EVE-NG

**Endpoint:** `POST /api/v1/admin/labs/sync`

**Headers:**
```
Authorization: Bearer {{adminToken}}
```

**Request:** (empty body)

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/admin/labs/sync" \
  -H "Authorization: Bearer {{adminToken}}"
```

**Success Response (200 OK):**
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

**Validation:**
- Check DB: `SELECT * FROM eveng_templates;` → 3 rows (ACTIVE)
- Check DB: `SELECT * FROM lab_assignments;` → 0 rows (no assignments yet)

### A2. List Accessible Templates (TEACHER & STUDENT see none initially)

**Endpoint:** `GET /api/v1/eveng/labs`

**Headers (as teacher):**
```
Authorization: Bearer {{teacherToken}}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": [],
  "timestamp": "2026-04-23T00:46:00Z"
}
```
→ No templates visible because no assignment yet.

---

## Phase B: Assignment Hierarchy (Two-Level: ADMIN → TEACHER → STUDENT)

### B1. ADMIN Assigns Template to TEACHER

**Endpoint:** `POST /api/v1/admin/labs/{templateId}/assign/teacher`

**Replace `{templateId}`** with first synced template ID (from A1 response or DB query).

**Headers:**
```
Authorization: Bearer {{adminToken}}
```

**Request Body:**
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

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Teacher assigned successfully",
  "data": null,
  "timestamp": "2026-04-23T00:47:00Z"
}
```

**Validation:**
- DB: `SELECT * FROM lab_assignments WHERE user_id = '{{teacherId}}';` → 1 row
- Teacher can now see the template.

### B2. TEACHER Views Assigned Templates

**Endpoint:** `GET /api/v1/eveng/labs` (as teacher)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "name": "NetworkTopology-1",
      "evengTemplatePath": "/NetworkTopology-1.unl",
      "status": "ACTIVE",
      "lastSyncedAt": "2026-04-22T23:00:00Z",
      "createdAt": "2026-04-22T23:00:00Z",
      "description": "Basic network lab"
    }
  ],
  "timestamp": "2026-04-23T00:48:00Z"
}
```

### B3. TEACHER Assigns Template to STUDENT

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/assign/student`

**Headers:**
```
Authorization: Bearer {{teacherToken}}
```

**Request Body:**
```json
{
  "studentId": "{{studentId}}"
}
```

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/assign/student" \
  -H "Authorization: Bearer {{teacherToken}}" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"{{studentId}}"}"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Student assigned successfully",
  "data": null,
  "timestamp": "2026-04-23T00:49:00Z"
}
```

**Validation:**
- DB: `SELECT * FROM lab_assignments;` → 2 rows (teacher + student)
- Assignment row has `assigned_by = {{teacherId}}` for student.

### B4. Negative: STUDENT Attempts to Assign Another Student (403)

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/assign/student` as student

**Response (403 Forbidden):**
```json
{
  "success": false,
  "message": "Access Denied: Teacher role required",
  "data": null,
  "timestamp": "2026-04-23T00:50:00Z"
}
```

### B5. Negative: ADMIN Assigns Directly to STUDENT (403)

**Endpoint:** `POST /api/v1/admin/labs/{templateId}/assign/student` (NO_SUCH_ENDPOINT — by design)

Reference states: "ADMIN cannot assign directly to STUDENT." There is no endpoint. Test that it doesn't exist.

---

## Phase C: Instance Lifecycle (Lazy Creation + Isolation)

### C1. STUDENT Starts Instance (First Start → Lazy Create)

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/start`

**Headers:**
```
Authorization: Bearer {{studentToken}}
```

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/start" \
  -H "Authorization: Bearer {{studentToken}}"
```

**Success Response (200 OK):**
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
  },
  "timestamp": "2026-04-23T00:51:05Z"
}
```

**What happened under the hood:**
1. Guard checked: role=STUDENT ✅, template ACTIVE ✅, assignment exists ✅.
2. No instance row found → `cloneLab` called (EVE-NG export+import) → new `.unl` file at `/opt/unetlab/labs/instances/{{studentId}}/{{instanceId}}.unl`.
3. Instance row inserted with `STOPPED`, then `startLab` called → status → `RUNNING`.
4. `startedAt` populated by `markRunning()`.

**Validation:**
- DB `lab_instances`: 1 row with `user_id = {{studentId}}`, `status = 'RUNNING'`, `eveng_instance_path` unique.
- File exists on EVE-NG server at predicted path (in simulation, just logged).

### C2. STUDENT Starts Again (Idempotent — Already RUNNING)

**Same endpoint as C1.**

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Lab started",
  "data": { ... same as C1, status still RUNNING ... },
  "timestamp": "2026-04-23T00:52:00Z"
}
```
→ No new instance created, no EVE-NG start called again.

### C3. TEACHER Starts Own Instance (First Time)

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/start` as teacher

**Response:** Instance created for teacher, status RUNNING, path `/instances/teacher1/...`.

**Validation:**
- DB `lab_instances`: now 2 rows (student + teacher).
- Paths are isolated: different `user_id` → different directories.

### C4. TEACHER Stops Own Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/stop` (teacher only)

**Response (200 OK):**
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

**Note:** Students **cannot** stop their own labs (business rule: only teachers stop own).

### C5. TEACHER Stops STUDENT's Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/students/{studentId}/stop`

**Headers:** `Bearer {{teacherToken}}`

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/students/{{studentId}}/stop" \
  -H "Authorization: Bearer {{teacherToken}}"
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Student lab stopped",
  "data": {
    "id": "{{studentInstanceId}}",
    "status": "STOPPED"
  }
}
```

**Validation:** Student instance status changes to STOPPED. `stoppedAt` set.

### C6. TEACHER Resets Student Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/students/{studentId}/reset`

**Precondition:** Student instance exists (any state).

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/students/{{studentId}}/reset" \
  -H "Authorization: Bearer {{teacherToken}}"
```

**Success Response:**
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
1. If RUNNING → stop.
2. Delete old instance file from EVE-NG.
3. `cloneLab` fresh copy → same `instanceId`, new `eveng_instance_path` (same base name).
4. Status set to STOPPED, timestamps cleared.

### C7. STUDENT Resets Own Instance

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/reset` (as student)

**Response:** Instance reset to STOPPED clean state.

### C8. Negative: STUDENT Stop Own Lab (403 Forbidden)

**Endpoint:** `POST /api/v1/eveng/labs/{templateId}/stop` as student

**Response:**
```json
{
  "success": false,
  "message": "Access Denied: Only teachers can stop their own labs",
  "data": null
}
```

---

## Phase D: Console Access & Progress Tracking

### D1. STUDENT Gets Console URL (Triggers Access Log)

**Precondition:** Instance must be RUNNING. Start it first if needed.

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/nodes/{nodeId}/console`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/nodes/R1/console" \
  -H "Authorization: Bearer {{studentToken}}"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "protocol": "TELNET",
    "host": "192.168.100.10",
    "port": 32769,
    "webSocketUrl": "ws://192.168.100.10:8080/api/labs/instances/student1/.../nodes/R1/console",
    "nodeId": "R1",
    "nodeName": "Router-1",
    "status": "RUNNING"
  }
}
```

**Side Effect:**
- Row inserted into `console_access_log` with `user_id`, `instance_id`, `node_id`, `accessed_at`.

**Validation:**
```sql
SELECT node_id, accessed_at FROM console_access_log 
WHERE user_id = '{{studentId}}' ORDER BY accessed_at DESC LIMIT 1;
-- Returns: R1, <timestamp>
```

### D2. STUDENT Accesses Same Node Again (Multiple Entries)

Call D1 again → second log row created. Each access is logged (not deduped).

### D3. TEACHER Views Student Progress

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/progress`

**Headers:** `Bearer {{teacherToken}}`

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/eveng/labs/11111111-1111-1111-1111-111111111111/progress" \
  -H "Authorization: Bearer {{teacherToken}}"
```

**Success Response (200 OK):**
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
    },
    {
      "studentId": "{{teacherId}}",  /* if teacher self is also a "user" of template? no */
      "studentName": "otherStudent",
      "instanceStatus": "STOPPED",
      "startedAt": "2026-04-23T00:51:05Z",
      "nodeStatuses": null,
      "lastConnected": "2026-04-23T00:54:00Z",
      "accessedNodes": ["R1"]
    }
  ],
  "timestamp": "2026-04-23T00:56:00Z"
}
```

**Note:** Only students **assigned by this teacher** appear in the report.

### D4. TEACHER Lists Student Instances

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/students/instances`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "{{studentInstanceId}}",
      "templateId": "11111111-...",
      "userId": "{{studentId}}",
      "evengInstancePath": "/instances/student1/...unl",
      "status": "RUNNING",
      "createdAt": "...",
      "startedAt": "...",
      "stoppedAt": null
    }
  ]
}
```

---

## Phase E: Guard Violations (Negative Tests)

### E1. STUDENT Access Template Without Assignment (403)

**Setup:** Ensure student has no assignment row for a template.

**Endpoint:** `GET /api/v1/eveng/labs` (unassigned template ID)

**Response:** (Forbidden — assignment guard fails)
```json
{
  "success": false,
  "message": "Access Denied: You are not assigned to this lab template",
  "data": null
}
```

### E2. Access REMOVED Template (404/403 After Soft-Delete)

**Setup:** ADMIN syncs then manually sets template status = REMOVED (or delete from EVE-NG and re-sync).

**Endpoint:** `GET /api/v1/eveng/labs/{removedTemplateId}`

**Response:**
```json
{
  "success": false,
  "message": "Template is not available (status: REMOVED)",
  "data": null
}
```

### E3. TEACHER Access Student Instance Without Being Assigning Teacher (403)

**Setup:** Teacher A assigns student; Teacher B tries to stop that student's instance.

**Endpoint:** `POST /api/v1/eveng/labs/{id}/students/{studentId}/stop` as Teacher B

**Response:**
```json
{
  "success": false,
  "message": "Access Denied: You are not the assigning teacher for this student",
  "data": null
}
```

### E4. Unauthorized Guard — STUDENT Role on Teacher Endpoint

**Endpoint:** `POST /api/v1/eveng/labs/{id}/assign/student` as student

**Response:** 403 Forbidden.

### E5. Template Not Found (404)

**Endpoint:** `GET /api/v1/eveng/labs/00000000-0000-0000-0000-000000000000/start`

**Response:**
```json
{
  "success": false,
  "message": "Template not found: 00000000-...",
  "data": null
}
```

---

## Phase F: Parallel Progress Fetch (Performance Validation)

### F1. Teacher with 30 Students — Progress Endpoint Timing

**Setup:** Create 30 student assignments for same template (scripted via API or direct DB). Start a few instances.

**Endpoint:** `GET /api/v1/eveng/labs/{templateId}/progress`

**Expected Behavior:**
- Response time ≈ max(EVE-NG node-status latency) not `N × latency`.
- All student reports returned with correct `nodeStatuses`.
- Micrometer metric `lab.progress.request` incremented by 1.

**Check logs:**
```
[INFO] Parallel fetch launched for 30 students...
[INFO] All student reports collected in 1500ms (not 30 × 1500ms)
```

### F2. Verify Parallelism via Mock EveNgService

Switch to simulated mode, set artificial delays (1s per node-status call). With 10 students:
- Sequential would take ~10s.
- Parallel should take ~1s.

Measure with Postman **Tests** script:
```javascript
const start = new Date();
// response received
const duration = new Date() - start;
pm.test("Parallel progress fetch completed in < 2000ms", function() {
    pm.expect(duration).to.be.below(2000);
});
```

---

## Postman Collection Export Structure

Create a collection **"MNCO Multi-Tenant EVE-NG Tests"** with these folders:

1. **00 Auth** — login (store tokens in env vars)
2. **01 Admin — Template Sync & Teacher Assignment**
3. **02 Teacher — Student Assignment**
4. **03 Student — Instance Lifecycle**
5. **04 Teacher — Manage Student Instances**
6. **05 Console & Progress**
7. **06 Negative Tests — Guard Violations**
8. **07 Performance — Parallel Progress**

Each request should have **Tests** tab with assertions:

```javascript
// Common test script
pm.test("Status is 200", () => pm.response.to.have.status(200));
pm.test("Response has success=true", () => pm.expect(pm.response.json().success).to.be.true);

// Extract IDs for chaining
if (pm.response.code === 200) {
    const json = pm.response.json();
    if (json.data && json.data.id) {
        pm.environment.set("lastInstanceId", json.data.id);
    }
}
```

---

## Expected Database State After Full Flow

### Tables & Row Counts (after Phases A–D with 1 template, 1 teacher, 1 student):

| Table | Rows | Purpose |
|-------|------|---------|
| `eveng_templates` | 3 | Synced from EVE-NG |
| `lab_assignments` | 2 | teacher1 + student1 assigned |
| `lab_instances` | 2 | teacher1's instance + student1's instance |
| `console_access_log` | ≥1 | At least 1 console access by student |
| `audit_logs` | ≥8 | Events: sync, assign (x2), start (x2), stop (x1), console access, reset |

### Key Queries to Validate State

**1. All active templates:**
```sql
SELECT id, name, template_status FROM eveng_templates;
```

**2. Assignment tree:**
```sql
SELECT a.id, t.name, u.username AS user, a.assigned_by
FROM lab_assignments a
JOIN eveng_templates t ON a.template_id = t.id
JOIN users u ON a.user_id = u.id;
-- Expected: teacher row (assigned_by=admin), student row (assigned_by=teacher)
```

**3. Instance isolation:**
```sql
SELECT id, user_id, eveng_instance_path, status 
FROM lab_instances 
ORDER BY user_id;
-- Paths should contain different user IDs
```

**4. Console access log:**
```sql
SELECT l.node_id, l.accessed_at, u.username
FROM console_access_log l
JOIN users u ON l.user_id = u.id;
```

---

## Guard Pattern Validation Checklist

For each operation, confirm that **all three guards** are hit in order:

| Operation | Guard 1 (Role) | Guard 2 (Template Active) | Guard 3 (Assignment) |
|-----------|---------------|---------------------------|----------------------|
| `GET /eveng/labs` (list) | ✅ TEACHER/STUDENT | ✅ | ✅ |
| `POST /{id}/start` | ✅ STUDENT/TEACHER | ✅ | ✅ |
| `POST /{id}/stop` (own) | ✅ TEACHER only | ✅ | ✅ |
| `POST /{id}/students/{sid}/stop` | ✅ TEACHER | ✅ | ✅ + assigned_by match |
| `POST /{id}/reset` (own) | ✅ STUDENT/TEACHER | ✅ | ✅ |
| Console URL | ✅ STUDENT/TEACHER | ✅ | ✅ |
| Progress | ✅ TEACHER only | ✅ | ✅ |

If any guard fails → 403, no EVE-NG call made. Check logs for "Guard" messages.

---

## Three Guards Implementation Verification

In `InstanceGuard` class, verify calls appear in order:

```java
// Expected entry log sequence for successful startInstance:
log.debug("Guard 1: role check passed for userId={}, role={}", userId, role);
log.debug("Guard 2: template active: templateId={}", templateId);
log.debug("Guard 3: assignment exists: (template,user)=({},{})", templateId, userId);
```

Add `DEBUG` logging to `InstanceGuard` temporarily to confirm ordering.

---

## Error Handling Reference

All errors follow this envelope:

```json
{
  "success": false,
  "message": "Human-readable error",
  "data": null,
  "timestamp": "2026-04-23T00:45:29Z"
}
```

| HTTP Code | Condition |
|-----------|-----------|
| 400 | Validation failure |
| 401 | Unauthenticated (no/malformed token) |
| 403 | Guard failure (role/template/assignment) |
| 404 | Resource not found (template/instance/user) |
| 409 | Assignment already exists (idempotent — returns 200) |
| 500 | EVE-NG integration error |

---

## API Surface Summary (New Multi-Tenant Endpoints)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET` | `/api/v1/eveng/labs` | STUDENT/TEACHER | List assignable templates |
| `POST` | `/api/v1/admin/labs/sync` | ADMIN | Sync templates from EVE-NG |
| `POST` | `/api/v1/admin/labs/{id}/assign/teacher` | ADMIN | Assign template to teacher |
| `POST` | `/api/v1/eveng/labs/{id}/assign/student` | TEACHER | Assign template to student |
| `DELETE` | `/api/v1/eveng/labs/{id}/assignments/{userId}` | ADMIN/TEACHER | Revoke assignment |
| `POST` | `/api/v1/eveng/labs/{id}/start` | STUDENT/TEACHER | Start own instance (lazy create) |
| `POST` | `/api/v1/eveng/labs/{id}/stop` | TEACHER | Stop own instance |
| `POST` | `/api/v1/eveng/labs/{id}/students/{sid}/stop` | TEACHER | Stop student's instance |
| `POST` | `/api/v1/eveng/labs/{id}/reset` | STUDENT/TEACHER | Reset own instance |
| `POST` | `/api/v1/eveng/labs/{id}/students/{sid}/reset` | TEACHER | Reset student's instance |
| `GET` | `/api/v1/eveng/labs/{id}/nodes/{nid}/console` | STUDENT/TEACHER | Get console URL + log |
| `GET` | `/api/v1/eveng/labs/{id}/progress` | TEACHER | Parallel progress report |
| `GET` | `/api/v1/eveng/labs/{id}/students/instances` | TEACHER | List all student instances |

---

## Full Workflow Example (End-to-End)

### Scenario: Admin assigns lab → Teacher assigns student → Student starts → Teacher monitors

```
Step 1: ADMIN logs in → gets {{adminToken}}
Step 2: ADMIN calls POST /admin/labs/sync
        → 3 templates created in DB (status=ACTIVE)

Step 3: ADMIN calls POST /admin/labs/{t1}/assign/teacher
        Body: {"teacherId":"{{teacherId}}"}
        → Assignment row created

Step 4: TEACHER logs in → gets {{teacherToken}}
Step 5: TEACHER calls POST /eveng/labs/{t1}/assign/student
        Body: {"studentId":"{{studentId}}"}
        → Student assignment row created (assigned_by=teacherId)

Step 6: STUDENT logs in → gets {{studentToken}}
Step 7: STUDENT calls POST /eveng/labs/{t1}/start
        → Instance created (clone), then started
        → Response: status=RUNNING, instancePath=/instances/student1/...

Step 8: STUDENT calls GET /eveng/labs/{t1}/nodes/R1/console
        → Console URL returned, access_log row inserted

Step 9: TEACHER calls GET /eveng/labs/{t1}/progress
        → Single student report with nodeStatuses (parallel fetched)
        → lastConnected=R1 timestamp, accessedNodes=["R1"]

Step 10: TEACHER calls POST /eveng/labs/{t1}/students/{{studentId}}/stop
         → Student instance stopped, status=STOPPED

Step 11: TEACHER calls POST /eveng/labs/{t1}/students/{{studentId}}/reset
         → Instance wiped and recopied from template, status=STOPPED

Step 12: STUDENT calls POST /eveng/labs/{t1}/start again (restart)
         → Reuses existing instance file, starts nodes
```

All steps pass with expected status codes and DB state changes.

---

## Metrics & Observability

After running the workflow, check:

```bash
# View metrics endpoint
curl http://localhost:8080/actuator/metrics/lab.instance.start
curl http://localhost:8080/actuator/metrics/lab.console.access
curl http://localhost:8080/actuator/metrics/eveng.sync
```

Expected counts:
- `lab.instance.start` ≥ 2 (student + teacher)
- `lab.console.access` ≥ 1
- `eveng.sync` created = 3

---

## Known Limitations (Per Reference §11)

- No network isolation between instances (Community EVE-NG limitation).
- No resource quotas per instance (only per user in existing quota table).
- Students cannot stop own labs (by design — teacher control).
- Single EVE-NG session shared across all backend calls.
- Progress is pull-based (not WebSocket push); parallelized but still polling.

---

**End of Test Workflow**
Next: Import into Postman, run in order, verify DB after key steps.
