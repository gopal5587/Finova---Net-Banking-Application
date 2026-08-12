# Deploying Finova

The same container images built locally are what you push to a registry and run on AWS ECS or GCP Cloud Run. Configuration is entirely environment-driven — no rebuild for secrets.

## Images

```bash
# Backend
docker build -t finova-api:latest ./backend

# Frontend (nginx + static SPA, proxies /api to the app service)
docker build -t finova-web:latest ./frontend
```

## Required environment variables (API)

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials |
| `REDIS_HOST` / `REDIS_PORT` | Redis endpoint |
| `JWT_SECRET` | Base64 256-bit+ signing key |
| `AES_SECRET` | Base64 32-byte AES-GCM key |
| `ADMIN_PASSWORD` | Seeded admin password (first boot) |
| `SPRING_PROFILES_ACTIVE` | Use `sandbox` unless wiring live integrations |

## AWS ECS (Fargate) sketch

1. Push images to ECR.
2. Create a Postgres instance (RDS) and ElastiCache Redis.
3. Define an ECS task for `finova-api` with the env vars above, CPU/memory limits, and a health check on `/actuator/health`.
4. Define a second service for `finova-web` behind an Application Load Balancer on port 80.
5. Point the ALB at the frontend; the nginx config already proxies `/api` to the API task DNS name (set `proxy_pass` host via a custom nginx config if the service discovery name differs from `app`).

## GCP Cloud Run sketch

1. Push images to Artifact Registry.
2. Provision Cloud SQL (Postgres) and Memorystore (Redis).
3. Deploy `finova-api` as a Cloud Run service with the env vars and Cloud SQL connector.
4. Deploy `finova-web` as a second Cloud Run service; set its nginx upstream to the API service URL (or terminate API on a separate path via an HTTPS load balancer).

## Local full stack

```bash
docker compose up --build
```

- Web UI: http://localhost:5173
- API: http://localhost:8080
- Grafana: http://localhost:3000 (admin/admin)
