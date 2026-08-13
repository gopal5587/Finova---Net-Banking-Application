# Deploy Finova (free tier): Render + Neon + Upstash + Cloudflare Pages

Split stack: **API on Render**, **Postgres on Neon**, **Redis on Upstash**, **React UI on Cloudflare Pages**.

Verify locally first (see [Local check](#local-check)), then follow the cloud steps below.

---

## Architecture

```
Browser → Cloudflare Pages (static SPA)
              ↓ HTTPS  VITE_API_BASE_URL
         Render (Spring Boot API)
              ↓              ↓
         Neon Postgres    Upstash Redis
```

---

## Local check

### Option A — Full Docker stack (recommended)

```powershell
cd "d:\Finova - Net Banking Application"
docker compose up --build postgres redis app frontend
```

| Service | URL |
| --- | --- |
| Web UI | http://localhost:5173 |
| API ping | http://localhost:8080/api/v1/ping |
| Health | http://localhost:8080/actuator/health |

**Login:** `admin` / `Admin@12345`

First `app` image build can take **15–30+ minutes** (Maven dependencies inside Docker).

**Smoke test:**

1. Open http://localhost:5173 → log in as admin.
2. Create a savings account from the dashboard.
3. Run a small transfer between two accounts (or deposit via API).
4. Check transaction history and admin fraud panel.

### Option B — Infra in Docker, API/UI on host

```powershell
docker compose up -d postgres redis
cd backend
# JAVA_HOME + mvn spring-boot:run (see README)
cd ..\frontend
npm install
npm run dev
```

Postgres is on host port **5433** (not 5432).

---

## 1. Neon (PostgreSQL)

1. Create a project at [neon.tech](https://neon.tech).
2. Copy the **connection string** (pooled or direct).
3. Convert to JDBC for Spring:

   ```
   jdbc:postgresql://ep-xxxx.region.aws.neon.tech/neondb?sslmode=require
   ```

4. Note **username**, **password**, and database name (often `neondb`).

Flyway runs automatically on first API start.

---

## 2. Upstash (Redis)

1. Create a Redis database at [upstash.com](https://upstash.com).
2. Copy the **Redis URL** (TLS), e.g.:

   ```
   rediss://default:AXXX...@us1-xxxx.upstash.io:6379
   ```

3. Set `REDIS_URL` on Render (Spring Boot reads this directly).

---

## 3. Render (Spring Boot API)

### Via Blueprint (`render.yaml`)

1. Push this repo to GitHub.
2. Render → **New** → **Blueprint** → select the repo.
3. After the service is created, set these **secret** env vars in the dashboard:

| Variable | Example / notes |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://ep-....neon.tech/neondb?sslmode=require` |
| `DB_USERNAME` | Neon user |
| `DB_PASSWORD` | Neon password |
| `REDIS_URL` | `rediss://default:...@....upstash.io:6379` |
| `JWT_SECRET` | Base64 256-bit secret (generate new; do not use dev default) |
| `AES_SECRET` | Base64 32-byte key for AES-GCM |
| `FINOVA_CORS_ORIGINS` | `https://your-app.pages.dev` (comma-separate multiple origins) |
| `ADMIN_PASSWORD` | Strong production admin password |

`SPRING_PROFILES_ACTIVE=sandbox` is set in `render.yaml` (simulated integrations, no external API keys).

### Manual web service

- **Root directory:** `backend`
- **Runtime:** Docker
- **Health check path:** `/actuator/health`
- **Plan:** Free (cold starts after ~15 min idle; first request may take 30–60s)

Copy your Render URL, e.g. `https://finova-api.onrender.com`.

**Verify API:**

```bash
curl https://finova-api.onrender.com/api/v1/ping
curl https://finova-api.onrender.com/actuator/health
```

---

## 4. Cloudflare Pages (React UI)

1. Cloudflare Dashboard → **Workers & Pages** → **Create** → **Pages** → Connect Git.
2. Build settings:

| Setting | Value |
| --- | --- |
| Root directory | `frontend` |
| Build command | `npm ci && npm run build` |
| Build output directory | `dist` |
| Node version | 20 (or 22) |

3. **Environment variables** (Production):

   ```
   VITE_API_BASE_URL=https://finova-api.onrender.com
   ```

   No trailing slash. Rebuild after changing this value.

4. Deploy. Note the Pages URL, e.g. `https://finova.pages.dev`.

5. Update Render env `FINOVA_CORS_ORIGINS` with the exact Pages URL and **redeploy** the API.

`frontend/public/_redirects` enables client-side routing (`/* → index.html`).

---

## 5. Post-deploy checklist

- [ ] API `/api/v1/ping` returns OK from Render URL
- [ ] UI loads on Cloudflare Pages
- [ ] Login works (CORS + `VITE_API_BASE_URL` correct)
- [ ] Create account + transfer works
- [ ] `JWT_SECRET` and `AES_SECRET` are **not** dev defaults
- [ ] `ADMIN_PASSWORD` changed from `Admin@12345`

---

## Generate production secrets

**JWT secret (Base64, 32+ bytes):**

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

**AES key (exactly 32 bytes, Base64):**

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

---

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| CORS error in browser | Set `FINOVA_CORS_ORIGINS` to exact Pages origin (scheme + host, no path) |
| API calls go to wrong host | Rebuild Pages after setting `VITE_API_BASE_URL` |
| 502 / timeout on first request | Render free tier cold start — wait and retry |
| Redis connection failed | Use full Upstash `rediss://` URL in `REDIS_URL` |
| DB SSL error | Append `?sslmode=require` to Neon JDBC URL |
| Docker `app` never healthy | Rebuild backend image (includes `curl` for healthcheck) |

---

## Cost notes (free tier)

- **Render:** Web service sleeps when idle; limited CPU/RAM.
- **Neon:** Storage and compute caps; fine for demos.
- **Upstash:** Request/day limits on free Redis.
- **Cloudflare Pages:** Generous free static hosting.

For production traffic, upgrade Render/Neon or move to a VPS path (see `docs/DEPLOY.md`).
