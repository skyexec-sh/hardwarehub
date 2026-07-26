# Database notes (Milestone 1)

Flyway migrations live in `backend/src/main/resources/db/migration`.

## Local PostgreSQL (Docker)

```bash
docker compose up -d postgres
```

Connection defaults:

- Host: `localhost`
- Port: `5432`
- Database: `hardwarehub`
- User / password: `hardwarehub` / `hardwarehub`

## Tables created in V1

- `roles`, `users`, `user_roles`
- `refresh_tokens`, `password_reset_tokens`
- `audit_logs`

Default owner user is seeded by the backend `DataSeeder` on first startup.
