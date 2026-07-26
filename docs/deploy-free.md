# Free public deploy (GitHub Pages + Neon + Render)

HardwareHub cannot run entirely on GitHub Pages: Pages is **static only**. This setup uses:

| Piece | Free service | Role |
| --- | --- | --- |
| Frontend | **GitHub Pages** | Angular UI |
| Database | **Neon** | PostgreSQL |
| API | **Render** (free web service) | Spring Boot |

Public UI (after Pages is enabled): `https://skyexec-sh.github.io/hardwarehub/`

## 1. Create free Postgres (Neon)

1. Sign up at [https://neon.tech](https://neon.tech) and create a project (e.g. `hardwarehub`).
2. Open **Dashboard → Connection details**.
3. Copy:
   - Connection string (URI), or host / database / user / password
4. Prefer the **pooled** connection host if Neon shows one.

You can paste either:

- Neon URI: `postgresql://USER:PASSWORD@HOST/DB?sslmode=require`  
  (the API normalizes this to JDBC automatically), or
- Separate JDBC pieces:
  - `DATABASE_URL=jdbc:postgresql://HOST/DB?sslmode=require`
  - `DATABASE_USERNAME=...`
  - `DATABASE_PASSWORD=...`

## 2. Deploy free API (Render)

1. Sign up at [https://render.com](https://render.com) and connect the `skyexec-sh/hardwarehub` GitHub repo.
2. **New → Blueprint** and select this repo (uses `render.yaml`), **or** create a **Web Service**:
   - Root directory / Docker context: `backend`
   - Dockerfile: `backend/Dockerfile`
   - Instance type: **Free**
3. Set environment variables:

| Variable | Value |
| --- | --- |
| `DATABASE_URL` | Neon URI or `jdbc:postgresql://...` |
| `DATABASE_USERNAME` | Neon user (if not embedded in URI) |
| `DATABASE_PASSWORD` | Neon password (if not embedded in URI) |
| `JWT_SECRET` | long random string (≥32 chars) |
| `CORS_ALLOWED_ORIGINS` | `https://skyexec-sh.github.io` |

4. Deploy and wait until health is green: `https://YOUR-SERVICE.onrender.com/actuator/health`
5. Copy the service URL (no trailing slash), e.g. `https://hardwarehub-api.onrender.com`

**Note:** Render free services **sleep after idle**. The first request after sleep can take 30–60s while Spring Boot starts.

## 3. Point GitHub Pages UI at the API

1. In the GitHub repo: **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. **Settings → Secrets and variables → Actions → Variables**:
   - Name: `API_BASE_URL`
   - Value: your Render URL, e.g. `https://hardwarehub-api.onrender.com`
3. Run workflow **Deploy frontend to GitHub Pages** (Actions → Run workflow), or push to `main`.
4. Open `https://skyexec-sh.github.io/hardwarehub/` and log in (`owner` / `Owner@123`). Change that password after first login.

## Local vs free cloud

| Mode | Frontend API URL |
| --- | --- |
| Docker Compose | `/api/v1` (nginx proxies to backend) |
| GitHub Pages | `https://YOUR-API/api/v1` (injected at build time) |

## Troubleshooting

- **CORS errors**: ensure `CORS_ALLOWED_ORIGINS` includes `https://skyexec-sh.github.io` (no path).
- **Blank / wrong API**: confirm repo variable `API_BASE_URL` and re-run the Pages workflow.
- **DB SSL / connection refused**: use Neon’s connection string with `sslmode=require`.
- **404 on refresh**: workflow copies `index.html` → `404.html` for SPA routing; redeploy Pages if that step failed.
