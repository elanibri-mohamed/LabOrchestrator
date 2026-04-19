# MNCO Backend API Testing Guide

**Version:** 1.0  
**Last Updated:** April 8, 2026  
**Environment:** Local Development (http://localhost:8080)

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Authentication Flow](#authentication-flow)
3. [API Endpoints](#api-endpoints)
   - [Auth API](#auth-api)
   - [Lab API](#lab-api)
   - [Lab Template API](#lab-template-api)
   - [Admin API](#admin-api)
   - [Audit Log API](#audit-log-api)
   - [Quota API](#quota-api)
4. [Testing Workflows](#testing-workflows)
5. [Error Handling](#error-handling)
6. [Response Format](#response-format)

---

## Quick Start

### Prerequisites

- Backend service running on `http://localhost:8080`
- Postman, cURL, or similar HTTP client
- Test user credentials (or register new ones)

### Test User Accounts

Use these credentials for testing:

| Username | Email | Password | Role |
|----------|-------|----------|------|
| `admin` | `admin@mnco.local` | `Admin@123` | ADMIN |
| `instructor1` | `instructor@mnco.local` | `Instr@123` | INSTRUCTOR |
| `student1` | `student@mnco.local` | `Stud@123` | STUDENT |

---

## Authentication Flow

### 1. Register New User

**Endpoint:** `POST /api/v1/auth/register`

**Request Body:**
```json
{
  "username": "testuser123",
  "email": "testuser@mnco.local",
  "password": "TestPass@123"
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "password": "TestPass@123"
  }'
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "role": "STUDENT",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (HttpOnly cookie)"
  },
  "timestamp": "2026-04-08T10:30:00Z"
}
```

**Validation Rules:**
- Username: 3-50 chars, alphanumeric + `-_.`
- Email: Valid email format
- Password: 8-100 chars

---

### 2. Login

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "usernameOrEmail": "testuser123",
  "password": "TestPass@123"
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser123",
    "password": "TestPass@123"
  }' \
  -c cookies.txt
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "role": "STUDENT",
    "refreshToken": "..."
  },
  "timestamp": "2026-04-08T10:31:00Z"
}
```

**Save the `accessToken` for authenticated requests.**

---

### 3. Refresh Token

**Endpoint:** `POST /api/v1/auth/refresh`

**Request Header:**
```
Authorization: Bearer <refreshToken>
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Authorization: Bearer <refreshToken>" \
  -b cookies.txt
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "role": "STUDENT"
  },
  "timestamp": "2026-04-08T10:32:00Z"
}
```

---

### 4. Get Current User

**Endpoint:** `GET /api/v1/auth/me`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User profile retrieved",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "role": "STUDENT",
    "enabled": true,
    "createdAt": "2026-04-08T10:30:00Z"
  },
  "timestamp": "2026-04-08T10:33:00Z"
}
```

---

### 5. Logout

**Endpoint:** `POST /api/v1/auth/logout`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (204 No Content)**

---

## API Endpoints

### Auth API

#### Base URL: `/api/v1/auth`

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/register` | POST | ❌ No | Register new user |
| `/login` | POST | ❌ No | Login with credentials |
| `/refresh` | POST | ❌ No | Refresh access token |
| `/logout` | POST | ✅ Yes | Logout (revoke tokens) |
| `/me` | GET | ✅ Yes | Get current user profile |

---

### Lab API

#### Base URL: `/api/v1/labs`

#### 1. List All Labs (Current User)

**Endpoint:** `GET /api/v1/labs`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/labs \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Labs retrieved successfully",
  "data": [
    {
      "id": "lab-123",
      "name": "Network Basics",
      "description": "Introduction to networking",
      "status": "RUNNING",
      "ownerId": "550e8400-e29b-41d4-a716-446655440000",
      "templateId": "template-456",
      "evengLabId": "eveng-789",
      "cpuAllocated": 4,
      "ramAllocated": 8,
      "storageAllocated": 100,
      "startedAt": "2026-04-08T09:00:00Z",
      "stoppedAt": null,
      "createdAt": "2026-04-08T08:00:00Z"
    }
  ],
  "timestamp": "2026-04-08T10:40:00Z"
}
```

---

#### 2. Create Lab

**Endpoint:** `POST /api/v1/labs`

**Request Header:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Advanced Routing",
  "description": "OSPF and BGP configuration",
  "templateId": "template-456",
  "cpu": 8,
  "ram": 16,
  "storage": 250
}
```

**Validation Rules:**
- name: 3-100 chars, required
- description: max 500 chars, optional
- templateId: optional
- cpu: 1-32 cores
- ram: 1-128 GB
- storage: 10-500 GB

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/labs \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Advanced Routing",
    "description": "OSPF and BGP configuration",
    "cpu": 8,
    "ram": 16,
    "storage": 250
  }'
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Lab created successfully",
  "data": {
    "id": "lab-999",
    "name": "Advanced Routing",
    "description": "OSPF and BGP configuration",
    "status": "CREATED",
    "ownerId": "550e8400-e29b-41d4-a716-446655440000",
    "templateId": "template-456",
    "evengLabId": "eveng-888",
    "cpuAllocated": 8,
    "ramAllocated": 16,
    "storageAllocated": 250,
    "startedAt": null,
    "stoppedAt": null,
    "createdAt": "2026-04-08T10:45:00Z"
  },
  "timestamp": "2026-04-08T10:45:00Z"
}
```

---

#### 3. Get Lab Details

**Endpoint:** `GET /api/v1/labs/{id}`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/labs/lab-999 \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Lab retrieved successfully",
  "data": {
    "id": "lab-999",
    "name": "Advanced Routing",
    "description": "OSPF and BGP configuration",
    "status": "CREATED",
    "ownerId": "550e8400-e29b-41d4-a716-446655440000",
    "templateId": "template-456",
    "evengLabId": "eveng-888",
    "cpuAllocated": 8,
    "ramAllocated": 16,
    "storageAllocated": 250,
    "startedAt": null,
    "stoppedAt": null,
    "createdAt": "2026-04-08T10:45:00Z"
  },
  "timestamp": "2026-04-08T10:46:00Z"
}
```

---

#### 4. Start Lab

**Endpoint:** `POST /api/v1/labs/{id}/start`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/labs/lab-999/start \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Lab started successfully",
  "data": {
    "id": "lab-999",
    "status": "RUNNING",
    "startedAt": "2026-04-08T10:47:00Z",
    "...": "other fields..."
  },
  "timestamp": "2026-04-08T10:47:00Z"
}
```

---

#### 5. Stop Lab

**Endpoint:** `POST /api/v1/labs/{id}/stop`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/labs/lab-999/stop \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Lab stopped successfully",
  "data": {
    "id": "lab-999",
    "status": "STOPPED",
    "stoppedAt": "2026-04-08T10:48:00Z",
    "...": "other fields..."
  },
  "timestamp": "2026-04-08T10:48:00Z"
}
```

---

#### 6. Clone Lab

**Endpoint:** `POST /api/v1/labs/{id}/clone`

**Request Header:**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Advanced Routing - Copy",
  "description": "Clone of original lab"
}
```

**Validation Rules:**
- name: 3-100 chars, required
- description: max 500 chars, optional

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/labs/lab-999/clone \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Advanced Routing - Copy",
    "description": "Clone of original lab"
  }'
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Lab cloned successfully",
  "data": {
    "id": "lab-new-clone",
    "name": "Advanced Routing - Copy",
    "description": "Clone of original lab",
    "status": "CREATED",
    "ownerId": "550e8400-e29b-41d4-a716-446655440000",
    "templateId": "template-456",
    "cpuAllocated": 8,
    "ramAllocated": 16,
    "storageAllocated": 250,
    "createdAt": "2026-04-08T10:50:00Z"
  },
  "timestamp": "2026-04-08T10:50:00Z"
}
```

---

#### 7. Delete Lab

**Endpoint:** `DELETE /api/v1/labs/{id}`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X DELETE http://localhost:8080/api/v1/labs/lab-999 \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (204 No Content)**

---

#### 8. Get Node Console (EVE-NG Integration)

**Endpoint:** `GET /api/v1/labs/{id}/nodes/{nodeId}/console`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/labs/lab-999/nodes/node-1/console \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Console info retrieved",
  "data": {
    "nodeId": "node-1",
    "nodeType": "ROUTER",
    "ip": "192.168.1.100",
    "port": 32768,
    "protocol": "VNC"
  },
  "timestamp": "2026-04-08T10:51:00Z"
}
```

---

#### 9. List All Labs (Admin Only)

**Endpoint:** `GET /api/v1/labs/admin/all`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/labs/admin/all \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "All labs retrieved",
  "data": [
    {
      "id": "lab-123",
      "name": "Network Basics",
      "ownerId": "user-1",
      "status": "RUNNING",
      "...": "other fields..."
    },
    {
      "id": "lab-456",
      "name": "Advanced Routing",
      "ownerId": "user-2",
      "status": "STOPPED",
      "...": "other fields..."
    }
  ],
  "timestamp": "2026-04-08T10:52:00Z"
}
```

---

### Lab Template API

#### Base URL: `/api/v1/templates`

#### 1. List All Public Templates

**Endpoint:** `GET /api/v1/templates`

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/templates
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Templates retrieved successfully",
  "data": [
    {
      "id": "template-456",
      "name": "Cisco Routing Lab",
      "description": "Complete routing topology",
      "version": "2.0",
      "isPublic": true,
      "createdBy": "instructor@mnco.local",
      "createdAt": "2026-03-15T08:00:00Z"
    }
  ],
  "timestamp": "2026-04-08T11:00:00Z"
}
```

---

#### 2. List User's Templates

**Endpoint:** `GET /api/v1/templates/mine`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/templates/mine \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User templates retrieved",
  "data": [
    {
      "id": "template-789",
      "name": "My Custom Lab",
      "description": "My template",
      "version": "1.0",
      "isPublic": false,
      "createdBy": "testuser123",
      "createdAt": "2026-04-08T09:00:00Z"
    }
  ],
  "timestamp": "2026-04-08T11:01:00Z"
}
```

---

#### 3. Get Template Details

**Endpoint:** `GET /api/v1/templates/{id}`

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/templates/template-456
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Template retrieved successfully",
  "data": {
    "id": "template-456",
    "name": "Cisco Routing Lab",
    "description": "Complete routing topology",
    "topologyYaml": "... YAML content ...",
    "version": "2.0",
    "isPublic": true,
    "createdBy": "instructor@mnco.local",
    "createdAt": "2026-03-15T08:00:00Z"
  },
  "timestamp": "2026-04-08T11:02:00Z"
}
```

---

#### 4. Create Template (Instructor/Admin Only)

**Endpoint:** `POST /api/v1/templates`

**Request Header:**
```
Authorization: Bearer <instructorAccessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "BGP Advanced Topics",
  "description": "Advanced BGP configuration lab",
  "topologyYaml": "topology:\n  nodes:\n    - id: r1\n      type: router",
  "version": "1.0",
  "isPublic": true
}
```

**Validation Rules:**
- name: 3-100 chars, required
- description: max 1000 chars, optional
- topologyYaml: required, YAML format
- version: max 20 chars, optional
- isPublic: boolean

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer <instructorAccessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "BGP Advanced Topics",
    "description": "Advanced BGP configuration lab",
    "topologyYaml": "topology:\n  nodes:\n    - id: r1",
    "version": "1.0",
    "isPublic": true
  }'
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Template created successfully",
  "data": {
    "id": "template-new",
    "name": "BGP Advanced Topics",
    "version": "1.0",
    "isPublic": true,
    "createdBy": "instructor@mnco.local",
    "createdAt": "2026-04-08T11:05:00Z"
  },
  "timestamp": "2026-04-08T11:05:00Z"
}
```

---

#### 5. Delete Template

**Endpoint:** `DELETE /api/v1/templates/{id}`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**Note:** Only template author or ADMIN can delete.

**cURL Command:**
```bash
curl -X DELETE http://localhost:8080/api/v1/templates/template-new \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (204 No Content)**

---

### Admin API

#### Base URL: `/api/v1/admin`

**Global Authorization:** ADMIN role required for ALL endpoints

#### 1. List All Users

**Endpoint:** `GET /api/v1/admin/users`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/admin/users \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "username": "testuser123",
      "email": "testuser@mnco.local",
      "role": "STUDENT",
      "enabled": true,
      "createdAt": "2026-04-08T10:30:00Z"
    }
  ],
  "timestamp": "2026-04-08T11:10:00Z"
}
```

---

#### 2. Get User Details

**Endpoint:** `GET /api/v1/admin/users/{userId}`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/admin/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "role": "STUDENT",
    "enabled": true,
    "createdAt": "2026-04-08T10:30:00Z"
  },
  "timestamp": "2026-04-08T11:11:00Z"
}
```

---

#### 3. Update User Role

**Endpoint:** `PATCH /api/v1/admin/users/{userId}/role?role=INSTRUCTOR`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**Query Parameters:**
- `role`: `STUDENT`, `INSTRUCTOR`, or `ADMIN`

**cURL Command:**
```bash
curl -X PATCH "http://localhost:8080/api/v1/admin/users/550e8400-e29b-41d4-a716-446655440000/role?role=INSTRUCTOR" \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User role updated successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "testuser123",
    "email": "testuser@mnco.local",
    "role": "INSTRUCTOR",
    "enabled": true,
    "createdAt": "2026-04-08T10:30:00Z"
  },
  "timestamp": "2026-04-08T11:12:00Z"
}
```

---

#### 4. Delete User

**Endpoint:** `DELETE /api/v1/admin/users/{userId}`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (204 No Content)**

---

#### 5. Get User Quota

**Endpoint:** `GET /api/v1/admin/users/{userId}/quota`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/admin/users/550e8400-e29b-41d4-a716-446655440000/quota \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User quota retrieved",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "maxLabs": 5,
    "usedLabs": 2,
    "remainingLabs": 3,
    "maxCpu": 16,
    "usedCpu": 8,
    "remainingCpu": 8,
    "maxRamGb": 64,
    "usedRamGb": 24,
    "remainingRamGb": 40,
    "maxStorageGb": 500,
    "usedStorageGb": 250,
    "remainingStorageGb": 250,
    "updatedAt": "2026-04-08T11:13:00Z"
  },
  "timestamp": "2026-04-08T11:13:00Z"
}
```

---

#### 6. Update User Quota

**Endpoint:** `PUT /api/v1/admin/users/{userId}/quota`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

**Request Body:**
```json
{
  "maxLabs": 10,
  "maxCpu": 32,
  "maxRamGb": 128,
  "maxStorageGb": 1000
}
```

**Validation Rules:**
- maxLabs: 1-50
- maxCpu: 1-128
- maxRamGb: 1-512
- maxStorageGb: 10-2000

**cURL Command:**
```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/550e8400-e29b-41d4-a716-446655440000/quota \
  -H "Authorization: Bearer <adminAccessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "maxLabs": 10,
    "maxCpu": 32,
    "maxRamGb": 128,
    "maxStorageGb": 1000
  }'
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User quota updated successfully",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "maxLabs": 10,
    "usedLabs": 2,
    "remainingLabs": 8,
    "maxCpu": 32,
    "usedCpu": 8,
    "remainingCpu": 24,
    "maxRamGb": 128,
    "usedRamGb": 24,
    "remainingRamGb": 104,
    "maxStorageGb": 1000,
    "usedStorageGb": 250,
    "remainingStorageGb": 750,
    "updatedAt": "2026-04-08T11:14:00Z"
  },
  "timestamp": "2026-04-08T11:14:00Z"
}
```

---

### Audit Log API

#### Base URL: `/api/v1/audit`

**Global Authorization:** ADMIN role required for ALL endpoints

#### 1. Get Recent Audit Logs

**Endpoint:** `GET /api/v1/audit/recent?limit=50`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**Query Parameters:**
- `limit`: Number of logs to retrieve (default: 100, max: 500)

**cURL Command:**
```bash
curl -X GET "http://localhost:8080/api/v1/audit/recent?limit=50" \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Recent audit logs retrieved",
  "data": [
    {
      "id": "audit-123",
      "eventType": "LOGIN",
      "actorId": "550e8400-e29b-41d4-a716-446655440000",
      "actorUsername": "testuser123",
      "labId": null,
      "labName": null,
      "result": "SUCCESS",
      "errorCode": null,
      "ipAddress": "192.168.1.100",
      "createdAt": "2026-04-08T11:15:00Z"
    },
    {
      "id": "audit-124",
      "eventType": "LAB_CREATED",
      "actorId": "550e8400-e29b-41d4-a716-446655440000",
      "actorUsername": "testuser123",
      "labId": "lab-999",
      "labName": "Advanced Routing",
      "result": "SUCCESS",
      "errorCode": null,
      "ipAddress": "192.168.1.100",
      "createdAt": "2026-04-08T11:15:30Z"
    }
  ],
  "timestamp": "2026-04-08T11:15:00Z"
}
```

---

#### 2. Get User's Audit Logs

**Endpoint:** `GET /api/v1/audit/user/{userId}`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/audit/user/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User audit logs retrieved",
  "data": [
    {
      "id": "audit-123",
      "eventType": "LOGIN",
      "actorId": "550e8400-e29b-41d4-a716-446655440000",
      "actorUsername": "testuser123",
      "labId": null,
      "labName": null,
      "result": "SUCCESS",
      "errorCode": null,
      "ipAddress": "192.168.1.100",
      "createdAt": "2026-04-08T11:15:00Z"
    }
  ],
  "timestamp": "2026-04-08T11:16:00Z"
}
```

---

#### 3. Get Lab's Audit Logs

**Endpoint:** `GET /api/v1/audit/lab/{labId}`

**Request Header:**
```
Authorization: Bearer <adminAccessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/audit/lab/lab-999 \
  -H "Authorization: Bearer <adminAccessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Lab audit logs retrieved",
  "data": [
    {
      "id": "audit-124",
      "eventType": "LAB_CREATED",
      "actorId": "550e8400-e29b-41d4-a716-446655440000",
      "actorUsername": "testuser123",
      "labId": "lab-999",
      "labName": "Advanced Routing",
      "result": "SUCCESS",
      "errorCode": null,
      "ipAddress": "192.168.1.100",
      "createdAt": "2026-04-08T11:15:30Z"
    },
    {
      "id": "audit-125",
      "eventType": "LAB_STARTED",
      "actorId": "550e8400-e29b-41d4-a716-446655440000",
      "actorUsername": "testuser123",
      "labId": "lab-999",
      "labName": "Advanced Routing",
      "result": "SUCCESS",
      "errorCode": null,
      "ipAddress": "192.168.1.100",
      "createdAt": "2026-04-08T11:15:45Z"
    }
  ],
  "timestamp": "2026-04-08T11:17:00Z"
}
```

---

### Quota API

#### Base URL: `/api/v1/quota`

#### 1. Get My Quota

**Endpoint:** `GET /api/v1/quota/me`

**Request Header:**
```
Authorization: Bearer <accessToken>
```

**cURL Command:**
```bash
curl -X GET http://localhost:8080/api/v1/quota/me \
  -H "Authorization: Bearer <accessToken>"
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User quota retrieved",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "maxLabs": 5,
    "usedLabs": 2,
    "remainingLabs": 3,
    "maxCpu": 16,
    "usedCpu": 8,
    "remainingCpu": 8,
    "maxRamGb": 64,
    "usedRamGb": 24,
    "remainingRamGb": 40,
    "maxStorageGb": 500,
    "usedStorageGb": 250,
    "remainingStorageGb": 250,
    "updatedAt": "2026-04-08T11:18:00Z"
  },
  "timestamp": "2026-04-08T11:18:00Z"
}
```

---

## Testing Workflows

### Workflow 1: Complete Student Lab Experience

1. **Register as student**
   ```bash
   POST /api/v1/auth/register
   ```

2. **Login**
   ```bash
   POST /api/v1/auth/login
   ```

3. **Check personal quota**
   ```bash
   GET /api/v1/quota/me
   ```

4. **View available templates**
   ```bash
   GET /api/v1/templates
   ```

5. **Create lab from template**
   ```bash
   POST /api/v1/labs
   ```

6. **Start the lab**
   ```bash
   POST /api/v1/labs/{id}/start
   ```

7. **Access node console**
   ```bash
   GET /api/v1/labs/{id}/nodes/{nodeId}/console
   ```

8. **Stop the lab**
   ```bash
   POST /api/v1/labs/{id}/stop
   ```

9. **Get current user info**
   ```bash
   GET /api/v1/auth/me
   ```

10. **Logout**
    ```bash
    POST /api/v1/auth/logout
    ```

---

### Workflow 2: Instructor Template Management

1. **Login as instructor**
   ```bash
   POST /api/v1/auth/login (with instructor credentials)
   ```

2. **View public templates**
   ```bash
   GET /api/v1/templates
   ```

3. **View my templates**
   ```bash
   GET /api/v1/templates/mine
   ```

4. **Create new template**
   ```bash
   POST /api/v1/templates
   ```

5. **Verify template created**
   ```bash
   GET /api/v1/templates/{id}
   ```

6. **Update to private**
   ```bash
   DELETE /api/v1/templates/{id}
   (Then create new with isPublic: false)
   ```

---

### Workflow 3: Administrator User Management

1. **Login as admin**
   ```bash
   POST /api/v1/auth/login (with admin credentials)
   ```

2. **List all users**
   ```bash
   GET /api/v1/admin/users
   ```

3. **Promote student to instructor**
   ```bash
   PATCH /api/v1/admin/users/{userId}/role?role=INSTRUCTOR
   ```

4. **Update user quota**
   ```bash
   PUT /api/v1/admin/users/{userId}/quota
   ```

5. **View user audit logs**
   ```bash
   GET /api/v1/audit/user/{userId}
   ```

6. **View recent system logs**
   ```bash
   GET /api/v1/audit/recent?limit=100
   ```

7. **Delete inactive user**
   ```bash
   DELETE /api/v1/admin/users/{userId}
   ```

---

### Workflow 4: Lab Lifecycle

1. **Create lab**
   ```bash
   POST /api/v1/labs
   ```

2. **Verify creation**
   ```bash
   GET /api/v1/labs/{id}
   ```

3. **Start lab**
   ```bash
   POST /api/v1/labs/{id}/start
   ```

4. **Clone running lab**
   ```bash
   POST /api/v1/labs/{id}/clone
   ```

5. **Stop original lab**
   ```bash
   POST /api/v1/labs/{id}/stop
   ```

6. **List all user labs**
   ```bash
   GET /api/v1/labs
   ```

7. **Delete lab**
   ```bash
   DELETE /api/v1/labs/{id}
   ```

---

## Error Handling

### Common HTTP Status Codes

| Status | Meaning | Example |
|--------|---------|---------|
| 200 | OK | Successful GET, successful operation |
| 201 | Created | Resource created (POST, PUT) |
| 204 | No Content | Successful DELETE, logout |
| 400 | Bad Request | Validation error, malformed JSON |
| 401 | Unauthorized | Missing or invalid token |
| 403 | Forbidden | Insufficient permissions (not ADMIN) |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate username/email, quota exceeded |
| 500 | Server Error | Internal server error |

---

### Error Response Format

**Validation Error (400):**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "errors": [
      {
        "field": "username",
        "message": "Username must be 3-50 characters"
      },
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ]
  },
  "timestamp": "2026-04-08T11:20:00Z"
}
```

**Unauthorized (401):**
```json
{
  "success": false,
  "message": "Unauthorized",
  "data": {
    "error": "Missing or invalid authentication token"
  },
  "timestamp": "2026-04-08T11:20:00Z"
}
```

**Forbidden (403):**
```json
{
  "success": false,
  "message": "Access Denied",
  "data": {
    "error": "You do not have permission to perform this action"
  },
  "timestamp": "2026-04-08T11:20:00Z"
}
```

**Not Found (404):**
```json
{
  "success": false,
  "message": "Resource Not Found",
  "data": {
    "error": "Lab with id 'lab-999' not found"
  },
  "timestamp": "2026-04-08T11:20:00Z"
}
```

**Conflict (409):**
```json
{
  "success": false,
  "message": "Duplicate Resource",
  "data": {
    "error": "Username already taken: testuser123"
  },
  "timestamp": "2026-04-08T11:20:00Z"
}
```

---

## Response Format

### Standard Success Response

All successful API responses follow this format:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    // Response-specific data
  },
  "timestamp": "2026-04-08T11:20:00Z"
}
```

### Global Response Wrapper

- **success**: `boolean` - Whether the operation was successful
- **message**: `string` - Human-readable message
- **data**: `object|array|null` - Response payload (varies by endpoint)
- **timestamp**: `string` - ISO 8601 timestamp of response

---

## Testing Checklist

### Authentication Tests
- [ ] Register new user with valid credentials
- [ ] Register with duplicate username (should fail)
- [ ] Register with invalid email format
- [ ] Register with weak password
- [ ] Login with correct credentials
- [ ] Login with wrong password
- [ ] Login with non-existent user
- [ ] Refresh token successfully
- [ ] Logout successfully
- [ ] Access protected endpoint without token (401)
- [ ] Access protected endpoint with expired token

### Lab Tests
- [ ] Create lab successfully
- [ ] Create lab with invalid CPU range
- [ ] List labs (empty and non-empty)
- [ ] Get lab details
- [ ] Start lab
- [ ] Start already running lab
- [ ] Stop lab
- [ ] Stop already stopped lab
- [ ] Clone lab
- [ ] Delete lab
- [ ] Delete non-existent lab
- [ ] Get node console

### Template Tests
- [ ] List public templates
- [ ] List user templates (with and without templates)
- [ ] Get template details
- [ ] Create template (instructor)
- [ ] Create template as student (should fail)
- [ ] Delete own template
- [ ] Delete another user's template (should fail)

### Admin Tests
- [ ] List all users
- [ ] Get user details
- [ ] Update user role
- [ ] Delete user
- [ ] Get user quota
- [ ] Update user quota
- [ ] View audit logs (recent)
- [ ] View user-specific audit logs
- [ ] View lab-specific audit logs

---

## Performance & Load Testing

### Recommended Tools
- **Apache JMeter** - Load testing
- **Postman Collections** - Automated test suites
- **curl** - Manual testing
- **Artillery** - Performance testing

### Sample Load Test Scenarios
1. 10 concurrent user registrations
2. 20 concurrent lab creations
3. 50 concurrent template views
4. Admin querying 1000 audit logs

---

## Notes & Tips

1. **Token Management:**
   - Access token expires in 1 hour
   - Refresh token used to get new access token
   - Always include `Bearer ` prefix in Authorization header

2. **Pagination:**
   - Not implemented yet; all results returned
   - Consider adding `page` and `size` query params

3. **Rate Limiting:**
   - Not implemented yet
   - Consider adding per-user rate limits

4. **CORS:**
   - Configure CORS headers based on frontend domain
   - Credentials should be included in requests

5. **Validation:**
   - All validation rules are enforced server-side
   - Client-side validation mirrors server-side

---

**Document Version:** 1.0  
**Last Updated:** April 8, 2026  
**Next Review:** April 15, 2026
