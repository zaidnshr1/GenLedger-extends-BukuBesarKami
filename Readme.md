# BukuBesarKami — General Ledger API

A RESTful API for project-based financial recording using **double-entry bookkeeping**.  
Built to ensure accurate, traceable, and secure cash flow management across multiple projects.
  
**Stack:** Spring Boot 3.5.11 · Java 21 · PostgreSQL 16 · Redis 7 · Docker · JWT · Junit5 & Mockito 

**Try It Out** [bit.ly/gen-ledger-v1](https://bit.ly/gen-ledger-v1)

**Or** [https://multiple-brooke-zaid-anshori-e3835b81.koyeb.app/swagger-ui/index.html](https://multiple-brooke-zaid-anshori-e3835b81.koyeb.app/swagger-ui/index.html)

---

## Overview

BukuBesarKami is a backend system for managing a general ledger (Buku Besar) across multiple projects under one organization. It supports two roles: a central admin who manages users, projects, and reports, and a project admin who handles daily transactions for their assigned project.

---

### System Flow

```mermaid
flowchart TD
    Client -->|HTTP Request| API[Spring Boot API :8080]
    API --> Auth[JWT Auth Filter]
    Auth -->|Valid Token| Router{Role Check}
    Router -->|ADMIN_PUSAT| AP[Admin Pusat Features]
    Router -->|ADMIN_PROJECT| APR[Admin Project Features]
    AP --> DB[(PostgreSQL)]
    APR --> DB
    API --> Redis[(Redis)]
    Redis -->|COA Cache| AP
    Redis -->|Rate Limit - login| Auth
    Redis -->|Idempotency Key| APR
```

### Database Schema

```mermaid
erDiagram
    users ||--o{ user_projects : "assigned to"
    users ||--o{ refresh_tokens : "has"
    users ||--o{ journal_entries : "created by"
    projects ||--o{ user_projects : "has admins"
    projects ||--o{ journal_entries : "has"
    projects ||--o{ accounts : "owns"
    accounts ||--o{ journal_lines : "used in"
    journal_entries ||--o{ journal_lines : "contains"
    journal_entries ||--o{ audit_logs : "tracked by"
```

### Journal Entry Lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Create Journal
    DRAFT --> DRAFT : Update (editable)
    DRAFT --> POSTED : Post (locked, final)
    DRAFT --> VOIDED : Void + reason
    POSTED --> VOIDED : Void + reason
    VOIDED --> [*]
```

---

## Key Features

| Feature | Implementation |
|---------|---------------|
| Double-entry validation | Checked at service layer (create, update, post) + DB constraint |
| Role-based access control | `@PreAuthorize` — project admins isolated to assigned projects only |
| Redis COA cache | `@Cacheable` on Chart of Accounts — reduces DB load on frequent reads |
| Rate limiting | Bucket4j + Redis — 5 requests/60s per IP on `/auth/login` |
| Idempotency key | `X-Idempotency-Key` header on journal creation, stored 24h in Redis |
| Pessimistic locking | `@Lock(PESSIMISTIC_WRITE)` on journal post/update — prevents race conditions |
| Refresh token rotation | Old token revoked on every refresh; device metadata stored |
| Async audit log | `@Async` + `REQUIRES_NEW` — every data change is logged (who, what, when) |
| Pagination | All list endpoints use `Page<T>` + `Pageable` — no unbounded queries |
| N+1 prevention | `@EntityGraph` on journal and account queries |
| SQL injection prevention | All queries use JPQL named parameters, no string concatenation |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Security | Spring Security · JWT (JJWT 0.12.6) |
| Database | PostgreSQL 16 |
| Cache & Rate Limit | Redis 7 · Bucket4j 8.10.1 |
| ORM | Spring Data JPA · Hibernate |
| Migration | Flyway |
| Documentation | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Containerization | Docker · Docker Compose |
| Utilities | Lombok |

---

## Getting Started

### Docker

**Prerequisites:** Docker + Docker Compose installed.

```bash
# 1. Clone the repository
git clone https://github.com/zaidnshr1/BukuBesarKita.git
cd BukuBesarKita

# 2. Set up environment variables
cp .env.example .env
# Edit .env and fill in your passwords and secrets

# 3. Build the JAR
mvn clean package -DskipTests

# 4. Start all services (PostgreSQL, Redis, pgAdmin, App)
docker compose up -d

# 5. Check status
docker compose ps
docker compose logs app --follow
```

**Services after startup:**

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| pgAdmin4 | http://localhost:5050 |
| Health check | http://localhost:8080/actuator/health |

---

### Environment Variables

Copy `.env.example` to `.env` and fill in the values. **Never commit `.env` to Git.**

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bukubesarkami
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

JWT_SECRET=your_jwt_secret_minimum_32_characters
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=86400000

PGADMIN_EMAIL=admin@example.com
PGADMIN_PASSWORD=your_pgadmin_password
```

---

### Default Credentials (from seed data)

```
username: adminpusat
password: Admin@123
```

> **Change this immediately on any non-local environment.**

---

## API Endpoints

### Auth — Public
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register Admin Pusat |
| POST | `/api/v1/auth/login` | Login (rate limited: 5 req/60s per IP) |
| POST | `/api/v1/auth/refresh-token` | Refresh access token |
| POST | `/api/v1/auth/logout` | Revoke all tokens |
| GET  | `/api/v1/auth/me` | Current user info |

### Admin Pusat — `ADMIN_PUSAT` role required
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin-pusat/users` | Create project admin |
| GET  | `/api/v1/admin-pusat/users` | List all users |
| PATCH | `/api/v1/admin-pusat/users/{id}/toggle-status` | Enable / disable user |
| POST | `/api/v1/admin-pusat/projects` | Create project |
| GET  | `/api/v1/admin-pusat/projects` | List all projects |
| PUT  | `/api/v1/admin-pusat/projects/{id}` | Update project |
| POST | `/api/v1/admin-pusat/projects/{id}/assign-admin` | Assign admin to project |
| DELETE | `/api/v1/admin-pusat/projects/{id}/remove-admin/{uid}` | Remove admin from project |
| POST | `/api/v1/admin-pusat/accounts` | Create COA account |
| GET  | `/api/v1/admin-pusat/accounts/global` | Global accounts (cached) |
| GET  | `/api/v1/admin-pusat/accounts/project/{id}` | Accounts for project (paginated) |
| GET  | `/api/v1/admin-pusat/reports/profit-loss` | P&L across all projects |
| GET  | `/api/v1/admin-pusat/reports/profit-loss/{id}` | P&L per project |
| GET  | `/api/v1/admin-pusat/reports/trial-balance/{id}` | Trial balance |
| GET  | `/api/v1/admin-pusat/audit-logs` | All activity logs |
| GET  | `/api/v1/admin-pusat/audit-logs/project/{id}` | Activity logs per project |

### Admin Project — `ADMIN_PROJECT` or `ADMIN_PUSAT`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/project/journals` | Create journal entry (requires `X-Idempotency-Key`) |
| GET  | `/api/v1/project/journals/project/{id}` | Transaction history (paginated) |
| GET  | `/api/v1/project/journals/{id}` | Journal detail with all lines |
| PUT  | `/api/v1/project/journals/{id}` | Update journal (DRAFT only) |
| POST | `/api/v1/project/journals/{id}/post` | Post journal (DRAFT → POSTED) |
| POST | `/api/v1/project/journals/{id}/void` | Void journal with reason |
| GET  | `/api/v1/project/budget/{projectId}` | Project budget summary |

---
