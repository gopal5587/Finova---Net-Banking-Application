<div align="center">

# Finova — Net Banking Application

**A secure, production-shaped net-banking platform built with Spring Boot and React.**

[![Live Demo](https://img.shields.io/badge/Live_Demo-finova--cik.pages.dev-0f6b6b?style=for-the-badge)](https://finova-cik.pages.dev)
[![API](https://img.shields.io/badge/API-Render-5c3ee0?style=for-the-badge)](https://finova-net-banking-application.onrender.com/api/v1/ping)

[Live App](https://finova-cik.pages.dev) · [API Health](https://finova-net-banking-application.onrender.com/actuator/health) · [Swagger](https://finova-net-banking-application.onrender.com/swagger-ui.html) · [Source](https://github.com/gopal5587/Finova---Net-Banking-Application)

</div>

---

## Overview

Finova is a full-stack net-banking system inspired by real-world retail banking flows. It covers account management, ACID money transfers, transaction history, admin fraud oversight, immutable audit trails, scheduled statements and interest, resilient third-party integrations, TOTP two-factor authentication, and observability — all behind a React single-page application.

Base currency: **INR**.

---

## Live deployment

| Service | Platform | URL |
| --- | --- | --- |
| **Web application** | Cloudflare Pages | [https://finova-cik.pages.dev](https://finova-cik.pages.dev) |
| **REST API** | Render | [https://finova-net-banking-application.onrender.com](https://finova-net-banking-application.onrender.com) |
| **API ping** | Render | [https://finova-net-banking-application.onrender.com/api/v1/ping](https://finova-net-banking-application.onrender.com/api/v1/ping) |
| **Health check** | Render | [https://finova-net-banking-application.onrender.com/actuator/health](https://finova-net-banking-application.onrender.com/actuator/health) |
| **Swagger UI** | Render | [https://finova-net-banking-application.onrender.com/swagger-ui.html](https://finova-net-banking-application.onrender.com/swagger-ui.html) |
| **Source code** | GitHub | [gopal5587/Finova---Net-Banking-Application](https://github.com/gopal5587/Finova---Net-Banking-Application) |

### Cloud architecture

```
Browser
   │
   ▼
Cloudflare Pages          React + TypeScript (Vite)
   │
   │  HTTPS
   ▼
Render                    Spring Boot 3.3 API
   │
   ├── Neon                PostgreSQL 16 (Flyway migrations)
   └── Upstash             Redis (balance cache)
```

> **Note:** The Render free tier sleeps after ~15 minutes of inactivity. The first request after idle may take 30–60 seconds to respond.

---

## Features

| Area | Capabilities |
| --- | --- |
| **Accounts** | Savings & current accounts, masked PAN, AES-256-GCM encryption at rest |
| **Transfers** | Deposits, withdrawals, P2P transfers with pessimistic locking & ACID guarantees |
| **Security** | JWT auth, BCrypt passwords, TOTP 2FA, role-based access (USER / ADMIN) |
| **Admin** | Account freeze/unfreeze, fraud flag review, on-demand job triggers |
| **Fraud** | Advisory rules — large amounts, velocity bursts, odd-hour transfers |
| **Auditing** | Hibernate Envers, `@Auditable` AOP, structured JSON audit logs |
| **Scheduling** | Monthly savings interest & account statements via Quartz |
| **Integrations** | Sandbox Stripe/PayPal, FX rates, weather, maps, hash-linked ledger sim |
| **Observability** | Micrometer, Prometheus metrics, Grafana dashboards (local stack) |
| **Frontend** | Login/MFA, dashboard, accounts, transfers, history, admin panel |

---

## Tech stack

| Layer | Technology |
| --- | --- |
| **Backend** | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 16, Flyway migrations |
| **Cache** | Redis (distributed) + Caffeine (in-process) |
| **Money** | `BigDecimal` (scale 2, HALF_EVEN), `@Transactional` |
| **Security** | JWT, BCrypt, TOTP 2FA, AES-256-GCM |
| **Auditing** | Hibernate Envers, AspectJ AOP, Logback |
| **Scheduling** | Quartz |
| **Resilience** | Resilience4j (retry, circuit breaker) |
| **Frontend** | React 19, TypeScript, Vite, React Router |
| **Local infra** | Docker Compose (Postgres, Redis, Prometheus, Grafana) |
| **Production** | Render · Neon · Upstash · Cloudflare Pages |

---

## Quick start (local)

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Java 21 + Maven (only if running backend outside Docker)
- Node.js 20+ (only if running frontend outside Docker)

### Run the full stack

```bash
git clone https://github.com/gopal5587/Finova---Net-Banking-Application.git
cd Finova---Net-Banking-Application
docker compose up --build postgres redis app frontend
```

| Service | URL |
| --- | --- |
| Web UI | http://localhost:5173 |
| API | http://localhost:8080/api/v1/ping |
| Swagger | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (`admin` / `admin`) |

**Default admin (local only):** `admin` / `Admin@12345`

### Local development (infra in Docker, apps on host)

```bash
docker compose up -d postgres redis

# Backend — Postgres on host port 5433
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

---

## Load testing

With the API running locally:

```bash
k6 run loadtest/transfers.js
```

---

## Configuration

All settings are environment-driven. Copy [`.env.example`](.env.example) to `.env` for local overrides.

The `sandbox` profile (default) simulates all external integrations — **no real API keys required**.

Key variables for production:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC PostgreSQL connection string |
| `REDIS_URL` | Redis URL (`rediss://` for Upstash) |
| `JWT_SECRET` | Base64-encoded signing key |
| `AES_SECRET` | Base64-encoded 32-byte AES key |
| `ADMIN_PASSWORD` | Seeded admin account password |
| `FINOVA_CORS_ORIGINS` | Allowed frontend origins (comma-separated) |
| `VITE_API_BASE_URL` | Render API URL (Cloudflare Pages build-time) |

---

## Deployment guides

| Guide | Stack |
| --- | --- |
| [docs/DEPLOY-CLOUD.md](docs/DEPLOY-CLOUD.md) | **Render + Neon + Upstash + Cloudflare Pages** (free tier) |
| [docs/DEPLOY.md](docs/DEPLOY.md) | AWS ECS / GCP Cloud Run |

---

## Project structure

```
├── backend/          Spring Boot API, Flyway migrations, Docker image
├── frontend/         React SPA (Vite)
├── docs/             Deployment guides
├── loadtest/         k6 transfer stress script
├── monitoring/       Prometheus & Grafana provisioning
└── docker-compose.yml
```

---

## Author

**Gopal Yadav** — [GitHub](https://github.com/gopal5587)

---

<div align="center">

Built as a portfolio-grade net-banking system — from local Docker to cloud deployment.

</div>
