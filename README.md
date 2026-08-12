# Finova - Net Banking Application

A secure, production-shaped net-banking system: accounts, ACID money transfers, transaction history, admin fraud oversight, immutable audit trails, scheduled statements/interest, resilient third-party integrations, 2FA, and full observability — with a React SPA on top.

## Tech Stack

| Concern | Technology |
| --- | --- |
| Language / Framework | Java 21, Spring Boot 3.3 |
| Database | PostgreSQL 16 (schema via Flyway) |
| Caching | Redis (distributed) + Caffeine (in-process) |
| Security | Spring Security, JWT, TOTP 2FA, AES-256-GCM at rest |
| Money | `BigDecimal` (scale 2, HALF_EVEN), Spring `@Transactional` (ACID) |
| Auditing | Hibernate Envers + AspectJ AOP + Logback/SLF4J |
| Scheduling | Quartz (interest + monthly statements) |
| Resilience | Resilience4j (retry, circuit breaker, timeout) |
| Integrations | Stripe/PayPal, Currency, Weather, Maps, Blockchain-sim (sandboxed) |
| Observability | Micrometer + Prometheus + Grafana |
| Frontend | React + TypeScript (Vite) |
| Deployment | Docker + Docker Compose; images for AWS ECS / GCP Cloud Run |

## Quick Start (Docker)

```bash
docker compose up --build
```

Then:

- Web UI: http://localhost:5173
- API health: http://localhost:8080/api/v1/ping
- Swagger UI: http://localhost:8080/swagger-ui.html
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

Default admin: `admin` / `Admin@12345`

## Local Dev

```bash
# Infra
docker compose up -d postgres redis

# API (Postgres is on host port 5433)
cd backend
# set JAVA_HOME if needed, then:
mvn spring-boot:run

# UI
cd frontend
npm install
npm run dev
```

## Load test

With the API running:

```bash
k6 run loadtest/transfers.js
```

## Configuration

All configuration is environment-driven. Copy `.env.example` to `.env` to override defaults. The `sandbox` profile (default) simulates external services — **no real API keys required**.

## Deploy

See [docs/DEPLOY.md](docs/DEPLOY.md) for AWS ECS and GCP Cloud Run notes.
