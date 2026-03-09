# Multi-Tenant Network & Cybersecurity Lab Orchestrator

## Project Overview

This project aims to develop a **multi-tenant orchestration platform for network and cybersecurity laboratories** built on top of EVE-NG.

The platform allows multiple users (students, researchers, or engineers) to **create, manage, and run isolated virtual network labs simultaneously** through a centralized web interface.

The system automates the deployment of complex network topologies (routers, firewalls, servers, cybersecurity tools) and manages computing resources such as CPU, RAM, and storage.

The project is designed to run on a virtualized infrastructure powered by Proxmox VE and uses a modern backend developed with Spring Boot.

---

# Objectives

The main objective of this project is to build a **secure and scalable platform** capable of:

* Automating the deployment of network and cybersecurity labs
* Supporting **multiple simultaneous users (multi-tenant architecture)**
* Providing **isolated environments for each user**
* Managing virtual network resources dynamically
* Simplifying practical training in networking and cybersecurity

The platform is intended for:

* universities
* cybersecurity training centers
* network engineering labs
* research environments

---

# System Architecture

The platform follows a **three-tier architecture**:

1. Presentation Layer (Frontend)
2. Application Layer (Backend API)
3. Infrastructure Layer (Virtual Lab Environment)

## Global Architecture

```
Users (Students / Administrators)
            │
            │
      Web Interface (React)
            │
            │ REST API
            │
       Spring Boot Backend
            │
            │
     Orchestration Engine
            │
            │
        EVE-NG Server
            │
            │
 Virtual Network Devices
 (Routers, Firewalls, VMs)
            │
            │
     Proxmox Virtualization
```

---

# Technology Stack

## Frontend

* React
* HTML / CSS / JavaScript
* REST API integration

## Backend

* Spring Boot
* Spring Security
* Hibernate / JPA
* REST API

## Database

* PostgreSQL

## Infrastructure

* EVE-NG

## Automation

* Python scripts
* Bash scripts
* EVE-NG API

---

# Functional Requirements

## User Management

The system must allow:

* user registration
* authentication
* role-based access control

Roles may include:

* administrator
* instructor
* student

---

## Lab Management

Users should be able to:

* create new labs
* start labs
* stop labs
* delete labs
* view running labs
* clone existing labs

Each lab consists of a **network topology composed of multiple virtual nodes**.

Example nodes:

* routers
* switches
* firewalls
* Linux virtual machines
* penetration testing tools

---

## Multi-Tenant Isolation

Each user must have an **isolated environment** to prevent interference between labs.

Isolation mechanisms include:

* virtual network segmentation
* VLANs or virtual bridges
* separated lab directories

---

## Resource Management

The platform must control resource allocation for each user.

Example constraints:

* maximum number of labs per user
* CPU limits
* RAM limits
* storage limits

---

## Lab Templates

The system may include predefined lab templates such as:

* routing and switching lab
* firewall configuration lab
* penetration testing lab
* intrusion detection lab

Templates allow users to deploy complete environments automatically.

---

# Non-Functional Requirements

The system must satisfy the following requirements.

## Security

* secure authentication
* encrypted communication
* isolation between users

## Scalability

The platform must support multiple concurrent users and labs.

## Reliability

The system must ensure stable lab deployment and proper resource management.

## Performance

Lab creation and startup time should be optimized to provide a smooth user experience.

---

# Backend API Design

Example REST endpoints:

```
POST /api/auth/login
POST /api/auth/register

GET /api/labs
POST /api/labs
POST /api/labs/{id}/start
POST /api/labs/{id}/stop
DELETE /api/labs/{id}

GET /api/templates
```

---

# Database Model

Main entities:

User

```
id
username
email
password
role
```

Lab

```
id
name
topology
status
owner_id
created_at
```

LabTemplate

```
id
name
description
topology_file
```

---

# Project Structure

Example backend structure:

```
lab-orchestrator
│
├── controller
│   ├── AuthController
│   └── LabController
│
├── service
│   ├── LabService
│   └── EveNgService
│
├── repository
│   ├── UserRepository
│   └── LabRepository
│
├── entity
│   ├── User
│   └── Lab
│
├── security
│   └── SecurityConfig
│
└── automation
    └── EveAutomation
```

---

# Example Use Case

1. A user logs into the platform.
2. The user selects a lab template.
3. The platform deploys the topology in the EVE-NG environment.
4. Virtual nodes are created and started.
5. The user accesses the lab via console or SSH.

---

# Deployment Architecture

The system runs on a server infrastructure based on:

* EVE-NG network emulator
* Backend API server
* Web frontend

Multiple virtual labs run simultaneously on the same infrastructure.

---

# Future Improvements

Possible enhancements include:

* monitoring dashboard
* resource usage analytics
* integration with CI/CD pipelines
* automatic lab cleanup
* attack simulation scenarios
* integration with security monitoring tools

---

# Educational Value

This project demonstrates skills in:

* network virtualization
* backend development
* system orchestration
* cybersecurity lab automation
* infrastructure management

It can be used as a **training platform for networking and cybersecurity education**.

---

# License

This project is released under the MIT License.
