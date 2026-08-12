# Finova - Net Banking Application

A secure, production-shaped net-banking system: accounts, ACID money transfers, transaction history, admin fraud oversight, immutable audit trails, scheduled statements/interest, resilient third-party integrations, and full observability.

## Tech Stack

| Concern | Technology |
| --- | --- |
| Language / Framework | Java 21, Spring Boot 3.3 |
| Database | PostgreSQL 16 (schema via Flyway) |
| Caching | Redis (distributed) + Caffeine (in-process) |
| Security | Spring Security, JWT, TOTP 2FA, AES-256-GCM at rest |
| Money | `BigDecimal` (scale 2, HALF_EVEN), Spring `@Transactional` (ACID) |
| Auditing | Hibernate Envers + AspectJ AOP + Logback/SLF4J |
| Scheduling | Spring Scheduler + Quartz |
| Resilience | Resilience4j (retry, circuit breaker, timeout) |
| Integrations | Stripe/PayPal, Currency, Weather, Maps, Blockchain-sim (all sandboxed) |
| Observability | Micrometer + Prometheus + Grafana |
| Frontend | React + TypeScript (Vite) |
| Deployment | Docker + Docker Compose; images for AWS ECS / GCP Cloud Run |

## Quick Start (Docker)

```bash
docker compose up --build
```

Then:

- API health: http://localhost:8080/api/v1/ping
- Actuator: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

## Local Backend Dev

```bash
cd backend
./mvnw spring-boot:run
```

Requires PostgreSQL and Redis running (e.g. `docker compose up postgres redis`). Postgres is exposed on host port **5433** to avoid clashing with a native PostgreSQL on 5432.

## Configuration

All configuration is environment-driven (12-factor). Copy `.env.example` to `.env` to override defaults. The `sandbox` profile (default) simulates all external services, so the app runs with **no real API keys required**.

## Project Status

Built in phases; see the commit history for feature milestones. Current phase: **Phase 1 - Scaffold & Infrastructure**.
