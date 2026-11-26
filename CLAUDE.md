# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Usty** is a private web app for couples. Each couple shares date ideas, which can be planned, completed and given photos, plus wishlists and dreams. It has a Telegram bot (`@UstyBM_bot`), and an admin who creates couples. The UI, code comments, README and user communication are all in **Ukrainian**. Keep that convention.

## Commands

The dev machine is Windows; Git Bash is also available.

```powershell
docker compose up -d                       # local PostgreSQL 17 (container planning-db, db/user/pass: planning)
cd backend;  .\gradlew.bat bootRun         # API on :8080 (reads backend/secrets.properties)
cd frontend; npm start                     # Angular dev server on :4200, proxies /api and /uploads -> :8080
```

- **Backend tests** (JUnit 5 + Mockito, pure unit tests, no DB):
  - all: `cd backend; .\gradlew.bat test`
  - one class: `.\gradlew.bat test --tests "com.planning.backend.service.AdminServiceTest"`
  - one method: `--tests "*.InviteServiceTest.eachLinkGivesOnlyItsOwnersPassword"`
- **Frontend:** `npx ng build` is the type-check and build. There are no frontend unit tests and no lint script; Prettier config is in `frontend/.prettierrc`.
- **Production stack** (Caddy + backend + db, secrets from `.env`): `docker compose -f docker-compose.prod.yml up -d --build`. See `DEPLOY.md`, `scripts/backup.sh`, `scripts/restore.sh` and `scripts/export-local.ps1`.

### Testing against a running instance without touching the user's data

- The user often has their own backend on :8080 and frontend on :4200. Don't stop them; use other ports, e.g. 18080 and 4201.
- Work on a copy of the database:
  - create it: `docker exec planning-db sh -c "createdb -U planning planning_test && pg_dump -U planning planning | psql -q -U planning planning_test"`
  - start the backend against it with `DB_URL=jdbc:postgresql://localhost:5432/planning_test`, `UPLOADS_DIR=<temp dir>`, `TELEGRAM_POLLING_ENABLED=false` and `TELEGRAM_CHAT_ID=" "`;
  - drop the copy afterwards.
- Never send real Telegram messages from tests.
- Only one process may poll the bot at a time; a second one gets 409 Conflict.

## Configuration and secrets

- **Local:** secrets live only in `backend/secrets.properties` (gitignored), imported via `spring.config.import=optional:file:./secrets.properties`. `application.properties` contains empty defaults.
- **Server:** the same keys come from environment variables in `.env`. Spring maps env vars to properties, e.g. `TELEGRAM_BOT_TOKEN` → `telegram.bot-token`.
- **Keys:**
  - `jwt.secret`: base64, ≥ 32 bytes;
  - `users.bodya.password` / `users.marichka.password`: used only to seed an empty DB;
  - `telegram.bot-token`, `telegram.bot-username`, `telegram.chat-id`, `telegram.polling-enabled`;
  - `pixabay.api-key`.
- **Spring Boot 4 property name:** it's `spring.web.error.include-message`, not `server.error.*`. The admin UI shows backend error messages from it.

## Architecture

### Stack

- **Backend:** Spring Boot 4.2, Java 17, Spring Security 7 as an OAuth2 resource server with HS256 JWTs. It uses Jackson 3 (`tools.jackson.*`, not `com.fasterxml`).
- **Database:** Hibernate `ddl-auto=update` creates and alters the schema. There is no Flyway.
- **Frontend:** Angular 22 with standalone components, signals, the new control flow syntax, `linkedSignal`/`model()`/`@let`, and no NgModules.

### Backend layering

`controller → service → repository → entity`, with request/response records in `dto/<area>/` and outbound HTTP in `client/` (Pixabay, translator, Telegram).

### Multi-couple isolation (the core invariant)

- Every content entity (`DateIdea`, `WishlistItem`, `Dream`) has a `couple` (`couple_id`); photos belong to dates.
- Services get the caller from `security/CurrentUser` (`get()`, `coupleId()`, `requireAdmin()`).
- Always query through couple-scoped repository methods such as `findByIdAndCouple_Id` or `findByCouple_Id…`. Another couple's record must come back as 404.
- New content must have its couple set on create.

### Authentication

- Login is **password-only**: `AuthService` checks the password against every user's BCrypt hash. Because of that, two users can never share a password; the admin flow rejects duplicates.
- `LoginAttemptLimiter` throttles attempts per client IP. Behind Caddy, `SERVER_FORWARD_HEADERS_STRATEGY=native` makes that IP real.
- The JWT subject is the user id.
- Admin rights are the DB flag `users.is_admin`, re-checked on every `/api/admin/**` call via `CurrentUser.requireAdmin()` (403 otherwise). The frontend also has an `adminGuard`.

### Activity tracking

- `security/ActivityTracker` is a `HandlerInterceptor` on `/api/**` (excluding `/api/auth/**`). After each successful non-GET request it runs a single `UserRepository.touchActivity` UPDATE.
- Login sets `last_login_at` and `last_activity_at` via `User.recordLogin`.
- Don't add per-service activity updates.

### Startup migrations

Startup migrations are idempotent `ApplicationRunner`s in `config/`, ordered with `@Order` and safe to run on every start:

1. `CoupleSetup`:
   - seeds the first couple (Бодя = admin, Марічка) on an empty DB, failing fast if the seed passwords are missing or equal;
   - migrates a legacy single-couple DB;
   - backfills `couple_id` and `last_activity_at`.
2. `LegacyDataMigration`: the old `items` table → `items_legacy`.
3. `TelegramRelink`: if the configured bot changed, clears stale `telegram_chat_id` links and issues invite tokens so everyone re-links.

For schema or data changes Hibernate can't do (dropping a column, backfilling), add JdbcTemplate steps here.

### Telegram

- `TelegramPoller` long-polls `getUpdates` on a daemon thread (`SmartLifecycle`; no webhook).
- `InviteService` handles `/start <token>`:
  1. the single-use `users.invite_token` resolves to exactly one user;
  2. it links `telegram_chat_id` and `telegram_bot`;
  3. it sends that user's pending password.
- The pending password is stored AES-GCM-encrypted (`security/PasswordVault`) until it is delivered or the user first logs in, and is then cleared.
- `NotificationService` sends "date planned" messages to the couple's linked chats (`@Async`).

### Stats

- `service/stats/`:
  - `StatsPoints` holds the points formula;
  - `MotivationGenerator` keeps phrase pools by gap level and category.
- The API returns only the caller's own motivation (`myMotivation`). This privacy is enforced on the backend, not just hidden in the UI.

### Photo quality

`service/quality/` scores uploaded date photos with OpenCV (YuNet face model in `resources/opencv/`) plus pixel metrics.

### Uploads

- `ImageStorageService` writes files to `uploads.dir` (`UPLOADS_DIR`) in subfolders `avatars/`, `covers/` and `date-photos/`. `WebConfig` serves them at `/uploads/**`.
- The DB stores relative `/uploads/...` URLs.
- Deleting a couple removes its files only after the transaction commits (`TransactionSynchronization`).

### Frontend structure (`frontend/src/app/`)

- `core/`: auth, models and one HTTP service per API area.
  - Session is stored in localStorage key `usty.session`.
  - An interceptor adds the Bearer token.
  - All API calls use relative `/api/...` URLs; same origin everywhere, so there is no CORS.
- `shared/`: reusable UI.
  - `modal/`: stacked modals; a static open-modal list drives z-index, Esc closes only the top modal, body scroll is restored only when no modals remain.
  - `page-hero/`: the slim blurred header used by every section; action buttons are projected, and icons wrapped in `<span class="ico">` hide on narrow screens.
  - Also `calendar/`, `user-avatar/`, `photo-picker/`, `entry-form/`, `entry-details/`.
- `features/`: `login`, `home` (couple bar + profile menu), `dates` (ideas + planned panel, plan/complete dialogs, history, stats), `wishlist`, `dreams`, `admin`.
- Date lifecycle: `DateStatus` `IDEA → PLANNED → COMPLETED`.

### Production

- `docker-compose.prod.yml` runs three services: `db` (named volume), `backend` (uploads bind-mounted at `./data/uploads`) and `web`.
- `web` is Caddy with the built Angular app in `/srv`. `deploy/Caddyfile` proxies `/api` and `/uploads` to the backend, does the SPA fallback, and gets automatic HTTPS when `SITE_ADDRESS` is a domain.
- Shell scripts must keep LF endings (`.gitattributes`).
- PowerShell 5.1 scripts that contain Cyrillic need a UTF-8 BOM.
