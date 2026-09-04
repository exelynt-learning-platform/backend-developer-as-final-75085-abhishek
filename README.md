# Resource Booking System

A secure, production-quality RESTful API for booking resources (rooms, vehicles, equipment) built with **Spring Boot 3**, **Java 17**, **Spring Security 6**, **JWT**, and **MySQL**.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT (jjwt 0.12.5) |
| Database | MySQL 8.x (H2 for tests) |
| ORM | Spring Data JPA / Hibernate |
| Documentation | SpringDoc OpenAPI 2.5 (Swagger UI) |
| Build Tool | Maven |
| Password Hashing | BCrypt |

---

## Features

- ✅ **JWT Authentication** — stateless login via `POST /auth/login`
- ✅ **RBAC** — `ROLE_ADMIN` (full access) and `ROLE_USER` (restricted access)
- ✅ **Resource CRUD** — ADMIN manages resources; USERs can read
- ✅ **Reservation Management** — with ownership enforcement from JWT
- ✅ **Reservation Statuses** — `PENDING`, `CONFIRMED`, `CANCELLED`
- ✅ **Price Calculation** — automatically computed from resource price × hours
- ✅ **Filtering** — by `status`, `minPrice`, `maxPrice`
- ✅ **Pagination & Sorting** — via `page`, `size`, `sort` query params
- ✅ **Swagger UI** — interactive API documentation at `/swagger-ui.html`
- ✅ **Seed Data** — admin and user accounts pre-created on startup
- ✅ **Unit + Integration Tests**

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+

---

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd resource-booking-system
```

### 2. Create the MySQL Database

```sql
CREATE DATABASE booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> The application uses `createDatabaseIfNotExist=true` in the default URL so this step is optional.

### 3. Configure Environment Variables

You can configure the application via environment variables or by editing `src/main/resources/application.yml`.

| Environment Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/booking_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | Full JDBC URL |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | Base64-encoded 256-bit secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token validity (ms) — default 24 hours |
| `SERVER_PORT` | `8080` | Application port |
| `SHOW_SQL` | `false` | Show JPA SQL queries |

**Example (bash/PowerShell):**
```bash
export DB_USERNAME=myuser
export DB_PASSWORD=mysecret
```

### 4. Build and Run

```bash
mvn spring-boot:run
```

Or build a JAR:
```bash
mvn clean package -DskipTests
java -jar target/resource-booking-system-1.0.0.jar
```

### 5. Verify Startup

Once running, the seed data initializer will automatically create:

```
Seed: Created admin user (username=admin, password=admin123)
Seed: Created user1 (username=user1, password=user123)
Seed: Created user2 (username=user2, password=user123)
Seed: Created 5 sample resources
```

---

## Swagger / API Documentation

Open your browser and navigate to:

```
http://localhost:8080/swagger-ui.html
```

The OpenAPI spec is also available at:
```
http://localhost:8080/v3/api-docs
```

**To authenticate in Swagger UI:**
1. Call `POST /auth/login` with seed credentials
2. Copy the `token` from the response
3. Click the **Authorize 🔒** button in Swagger UI
4. Enter `<your-token>` (Swagger adds `Bearer ` prefix automatically)

---

## Seed Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` |
| `user1` | `user123` | `ROLE_USER` |
| `user2` | `user123` | `ROLE_USER` |

---

## API Endpoints

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/login` | None | Get JWT token |

**Request body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1...",
  "type": "Bearer",
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

---

### Resources

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/resources` | USER, ADMIN | List all (paginated) |
| GET | `/resources/{id}` | USER, ADMIN | Get one |
| POST | `/resources` | ADMIN only | Create |
| PUT | `/resources/{id}` | ADMIN only | Update |
| DELETE | `/resources/{id}` | ADMIN only | Delete |

**Pagination params:** `page` (0-based), `size`, `sort` (e.g., `?page=0&size=10&sort=name,asc`)

---

### Reservations

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/reservations` | USER, ADMIN | List (ADMIN sees all; USER sees own) |
| GET | `/reservations/{id}` | USER, ADMIN | Get one (ownership enforced) |
| POST | `/reservations` | USER, ADMIN | Create (user from JWT) |
| PUT | `/reservations/{id}` | USER, ADMIN | Update (ownership enforced) |
| PATCH | `/reservations/{id}/status` | ADMIN only | Update status |
| DELETE | `/reservations/{id}` | USER, ADMIN | Delete/cancel (ownership enforced) |

**Filter & Pagination params:**
```
GET /reservations?status=PENDING&minPrice=50&maxPrice=200&page=0&size=10&sort=totalPrice,asc
```

**Create/Update request body:**
```json
{
  "resourceId": 1,
  "startTime": "2026-09-10T09:00:00",
  "endTime": "2026-09-10T11:00:00",
  "notes": "Team meeting"
}
```
> ⚠️ `userId` is **not** part of the request — it's always taken from the JWT token.

**Status update (ADMIN only):**
```json
{ "status": "CONFIRMED" }
```

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthControllerTest
mvn test -Dtest=ReservationServiceTest
```

Tests use an **H2 in-memory database** — no MySQL required for testing.

---

## Project Structure

```
src/
├── main/java/com/booking/
│   ├── BookingApplication.java          # Main entry point
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security 6 + JWT filter chain
│   │   └── OpenApiConfig.java           # Swagger/OpenAPI JWT config
│   ├── security/
│   │   ├── JwtTokenProvider.java        # JWT generate / validate / extract
│   │   ├── JwtAuthenticationFilter.java # Bearer token filter
│   │   └── UserDetailsServiceImpl.java  # DB-backed UserDetailsService
│   ├── entity/
│   │   ├── User.java
│   │   ├── Resource.java
│   │   └── Reservation.java
│   ├── enums/
│   │   ├── Role.java                    # ROLE_ADMIN, ROLE_USER
│   │   └── ReservationStatus.java       # PENDING, CONFIRMED, CANCELLED
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ResourceRepository.java
│   │   └── ReservationRepository.java   # JPQL filtering queries
│   ├── dto/
│   │   ├── request/                     # LoginRequest, ResourceRequest, etc.
│   │   └── response/                    # AuthResponse, ResourceResponse, etc.
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── ResourceService.java
│   │   └── ReservationService.java      # Ownership + pricing logic
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ResourceController.java
│   │   └── ReservationController.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # Centralized error responses
│   │   ├── ResourceNotFoundException.java
│   │   └── UnauthorizedException.java
│   └── seed/
│       └── DataInitializer.java         # Seed users + resources on startup
└── test/java/com/booking/
    ├── controller/
    │   ├── AuthControllerTest.java       # Integration tests
    │   └── ReservationControllerTest.java
    └── service/
        ├── ResourceServiceTest.java      # Unit tests with Mockito
        └── ReservationServiceTest.java
```

---

## Security Design

- **Stateless authentication** — no sessions, JWT validated on every request
- **BCrypt** password hashing (strength 10)
- **CSRF disabled** — appropriate for stateless REST APIs
- **Ownership enforcement** — USER identity always resolved from JWT, never from request body
- **Method-level security** — `@PreAuthorize` on ADMIN-only endpoints as secondary guard

---

## Error Response Format

All errors return a consistent JSON structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2026-09-01T10:00:00",
  "details": ["End time must be after start time"]
}
```

| HTTP Status | Scenario |
|---|---|
| 200 OK | Successful GET/PUT/PATCH |
| 201 Created | Successful POST |
| 204 No Content | Successful DELETE |
| 400 Bad Request | Validation failure, invalid arguments |
| 401 Unauthorized | Missing/invalid/expired JWT |
| 403 Forbidden | Insufficient role or ownership violation |
| 404 Not Found | Resource/Reservation not found |
| 500 Internal Server Error | Unexpected errors |

---

## PostgreSQL Support

To switch to PostgreSQL, update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

And replace the MySQL connector in `pom.xml` with:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
