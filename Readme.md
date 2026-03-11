# BukuBesarKami — General Ledger API

> Mencatat arus keuangan proyek via double-entry bookkeeping.
 
**Stack:** Spring Boot · Java 21 · PostgreSQL · JWT · Maven

---

## Arsitektur

```
src/main/java/com/bukubesarkami/
├── BukuBesarKamiApplication.java
│
├── common/
│   ├── exception/
│   │   ├── AppException.java
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       ├── ApiResponse.java
│       ├── EntryNumberGenerator.java
│       └── SecurityUtil.java
│
├── config/
│   ├── JwtProperties.java
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
│
├── core/
│   ├── entity/
│   │   ├── User.java
│   │   ├── Project.java
│   │   ├── UserProject.java
│   │   ├── Account.java
│   │   ├── JournalEntry.java
│   │   ├── JournalLine.java
│   │   ├── AuditLog.java
│   │   └── RefreshToken.java
│   └── repository/
│       └── [7 repositories, extend JpaRepository]
│
└── features/
    ├── auth/
    ├── adminpusat/
    └── adminproject/
```

---

## Quick Start

### 1. Pre-Requesite
- Java 21+, Maven, PostgreSQL

### 2. Setup Database
```sql
CREATE DATABASE bukubesarkami;
```

### 3. Configuration Environment
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
export JWT_SECRET=YourSecretKeyMinimal32CharsLongSecureKey!
```

### 4. Run App
```bash
mvn spring-boot:run
```

Flyway otomatis jalankan migrasi dan seed data awal.

### 5. Default credentials
```
username: adminpusat
password: Admin@123
```

---

## API Endpoints

### Auth
| Method | Endpoint                     | Akses         |
|--------|------------------------------|---------------|
| POST   | `/api/v1/auth/register`      | Public        |
| POST   | `/api/v1/auth/login`         | Public        |
| POST   | `/api/v1/auth/refresh-token` | Public        |
| POST   | `/api/v1/auth/logout`        | Authenticated |
| GET    | `/api/v1/auth/me`            | Authenticated |

### Admin Pusat
| Method | Endpoint                                               | Keterangan                 |
|--------|--------------------------------------------------------|----------------------------|
| POST   | `/api/v1/admin-pusat/users`                            | Buat Admin Project         |
| GET    | `/api/v1/admin-pusat/users`                            | Daftar user                |
| PATCH  | `/api/v1/admin-pusat/users/{id}/toggle-status`         | Aktif/nonaktif             |
| POST   | `/api/v1/admin-pusat/projects`                         | Buat proyek + assign admin |
| GET    | `/api/v1/admin-pusat/projects`                         | Daftar proyek              |
| PUT    | `/api/v1/admin-pusat/projects/{id}`                    | Update proyek              |
| POST   | `/api/v1/admin-pusat/projects/{id}/assign-admin`       | Tambah admin               |
| DELETE | `/api/v1/admin-pusat/projects/{id}/remove-admin/{uid}` | Hapus admin                |
| POST   | `/api/v1/admin-pusat/accounts`                         | Buat akun COA              |
| GET    | `/api/v1/admin-pusat/accounts/global`                  | COA global                 |
| GET    | `/api/v1/admin-pusat/accounts/project/{id}`            | COA per proyek             |
| GET    | `/api/v1/admin-pusat/reports/profit-loss`              | P&L semua proyek           |
| GET    | `/api/v1/admin-pusat/reports/profit-loss/{id}`         | P&L per proyek             |
| GET    | `/api/v1/admin-pusat/reports/trial-balance/{id}`       | Neraca saldo               |
| GET    | `/api/v1/admin-pusat/audit-logs`                       | Seluruh aktivitas          |
| GET    | `/api/v1/admin-pusat/audit-logs/project/{id}`          | Aktivitas per proyek       |

### Admin Project
| Method | Endpoint                                | Keterangan                 |
|--------|-----------------------------------------|----------------------------|
| POST   | `/api/v1/project/journals`              | Buat jurnal (double-entry) |
| GET    | `/api/v1/project/journals/project/{id}` | Riwayat transaksi          |
| GET    | `/api/v1/project/journals/{id}`         | Detail jurnal              |
| PUT    | `/api/v1/project/journals/{id}`         | Update jurnal (DRAFT saja) |
| POST   | `/api/v1/project/journals/{id}/post`    | Post jurnal                |
| POST   | `/api/v1/project/journals/{id}/void`    | Batalkan jurnal            |
| GET    | `/api/v1/project/budget/{projectId}`    | Cek saldo anggaran         |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## Keamanan & Integritas

### Double-Entry Check
Setiap jurnal wajib `total_debit == total_credit` dan minimum 2 baris. Dicek di:
1. Saat pembuatan jurnal (`createEntry`)
2. Saat update jurnal (`updateEntry`)
3. Saat posting jurnal (`postEntry`) — final gate

### JWT Security
- Access token: 1 jam
- Refresh token: 24 jam dengan **rotation** (setiap refresh, token lama direvoke)
- Semua token lama direvoke saat login ulang

### Role-Based Access Control
```
ADMIN_PUSAT  → Akses semua proyek + manajemen sistem
ADMIN_PROJECT → Akses proyek yang di-assign saja (isolasi data antar proyek)
```

### Data Integrity
- Semua operasi finansial dalam `@Transactional`
- Audit log async `@Transactional(propagation = REQUIRES_NEW)` — tidak mengganggu transaksi utama
- Jurnal yang sudah POSTED tidak bisa dihapus, hanya bisa VOID dengan alasan
- `amount` menggunakan `BigDecimal` (presisi 19,2)
- Database constraint: `total_debit = total_credit` saat status POSTED (DB-level safety net)

---

M. Zaid Anshori — m.zaidanshori04@gmail.com  
https://github.com/zaidnshr1/BukuBesarKita