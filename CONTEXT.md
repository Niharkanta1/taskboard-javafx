# Context Checkpoint — JavaFX Trello-Like Task Board

Last updated: after Phase 3 completion (awaiting user verification).

## 1. Project

- Path: `D:/Coding/AI/AI-Test/javafx-trellolike`
- Goal: production-quality desktop Trello-like app in JavaFX, built **incrementally in phases**.
- Rules (from `plan.md`): one phase at a time; each phase must compile, run, pass manual verification, pass automated tests; stop after each phase for user verification; do not break existing functionality; JavaFX + FXML (Scene Builder-compatible) + CSS; Java 21+; Maven; structured logging (SLF4J/logback, no `System.out` app logging); SQLite.
- Environment: Java `21.0.4` Temurin, Maven `3.9.9`, Windows, IntelliJ Community 2024.x.

## 2. Phase Status

```text
[x] Phase 0  — Project Foundation        (verified by user)
[x] Phase 1  — SQLite + Database Infra  (verified by user)
[x] Phase 2  — Authentication + Login   (verified by user)
[x] Phase 3  — Workspace CRUD           (DONE — awaiting user verification)
[ ] Phase 4  — Board + Kanban UI        (NEXT)
[ ] Phase 5  — Card CRUD
[ ] Phase 6  — Due Dates
[ ] Phase 7  — Markdown
[ ] Phase 8  — Image Attachments
[ ] Phase 9  — Drag & Drop
[ ] Phase 10 — Production Hardening
```

Tracker lives in `plan.md` section 36. Update it when a phase is verified.

## 3. Current State (verified)

- `mvn clean test` → **BUILD SUCCESS, 57/57 tests pass** (previous 35 + 13 `WorkspaceServiceTest` + 8 `WorkspaceIntegrationTest` + 1 new `ResourceSmokeTest` case).
- `mvn javafx:run` → app starts; login screen is the first view; dashboard now shows the workspace list.
- Runtime verified:
  - `data/taskboard.db` exists; Flyway idempotent (0 migrations on restart).
  - Dev bootstrap user `dev` (BCrypt hash, never plaintext).
  - Workspace CRUD verified against temp SQLite in integration tests: create, read, update, delete, blank-description→null, cascade delete of boards+cards.
- Session flow: login → dashboard ("Logged in as: dev" + Workspaces list) → logout → login screen. Session is in-memory per run.

## 4. File Map

```text
pom.xml
javafx-sdk/lib/            # Windows JavaFX 21.0.2 -win jars (for IntelliJ runs)
data/taskboard.db          # runtime DB (created by app; git-ignorable artifact)
logs/taskboard.log         # logback output
plan.md                  # requirements + phase tracker (section 36)

src/main/java/com/example/taskboard/
  Main.java                          # entry; wires DB + services, bootstraps dev user, shows Login
  config/AppConfig.java             # app name/version, window size, FXML/CSS resource paths
  config/AppPaths.java              # resolves data dir + DB path
  exception/AppException.java
  exception/DatabaseException.java
  exception/AuthenticationException.java
  exception/ValidationException.java
  database/DatabaseManager.java     # shared Connection; PRAGMA foreign_keys=ON; inTransaction()
  database/MigrationManager.java    # Flyway wrapper
  model/User.java, Workspace.java, Board.java, Card.java, CardStatus.java, CardAttachment.java
  repository/UserRepository.java, WorkspaceRepository.java, BoardRepository.java,
            CardRepository.java, CardAttachmentRepository.java
  repository/impl/*Impl.java        # User insert + reads; Workspace insert/update/find/findAll/count/delete
  service/AuthService.java          # BCrypt hash/verify, login validation
  service/DevUserBootstrap.java     # dev-only initial user (env-overridable password)
  service/WorkspaceService.java     # workspace validation + CRUD (name <=100, description <=500)
  service/NavigationService.java    # central scene switching (login <-> dashboard); getStage()
  session/SessionManager.java       # in-memory current user
  controller/LoginController.java   # login form + friendly errors
  controller/DashboardController.java # welcome label, workspace list, create/edit/delete, logout
  controller/WorkspaceDialogController.java # modal create/edit workspace dialog

src/main/resources/
  fxml/Login.fxml
  fxml/Dashboard.fxml
  fxml/WorkspaceDialog.fxml
  css/app.css, css/login.css, css/dashboard.css
  logback.xml
  db/migration/V1__initial_schema.sql

src/test/java/com/example/taskboard/
  ResourceSmokeTest.java
  database/DatabaseIntegrationTest.java
  service/AuthServiceTest.java          # in-memory fake UserRepository
  service/AuthenticationIntegrationTest.java  # temp-dir SQLite
  service/WorkspaceServiceTest.java     # in-memory fake WorkspaceRepository
  repository/WorkspaceIntegrationTest.java  # temp-dir SQLite CRUD + cascade
  session/SessionManagerTest.java
```

## 5. Key Decisions

- Package `com.example.taskboard` (default; plan allows changes).
- JavaFX 21.0.2; `javafx-fxml` is a separate module dependency (required).
- **IntelliJ runs need `-win` classifier jars** — plain `javafx-*-21.0.2.jar` Maven artifacts are manifest-only stubs.
- IntelliJ run config VM options:
  ```
  --module-path "D:\Coding\AI\AI-Test\javafx-trellolike\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml
  ```
- Zero-setup alternative: `mvn javafx:run` (plugin resolves platform jars automatically).
- Flyway **9.22.3** (OSS, SQLite support, Java 21). API notes:
  - `Flyway.configure().dataSource(jdbcUrl, user, password)` (3-arg overload).
  - `flyway.migrate()` returns `MigrateResult` → use `.migrationsExecuted`.
  - Exception class: `org.flywaydb.core.api.FlywayException`.
- sqlite-jdbc **3.46.0.0** (depends on slf4j-api transitively).
- **BCrypt via `org.mindrot:jbcrypt:0.4`** — pure Java, on Maven Central.
  - API: `BCrypt.gensalt(int)`, `BCrypt.hashpw(pw, salt)`, `BCrypt.checkpw(pw, hash)`.
  - `checkpw` handles malformed hashes internally (returns false).
  - `at.favre:bcrypt` is NOT on Maven Central (empty artifact dir) — do not use it.
- **Single shared connection** in `DatabaseManager`; repositories call `db.getConnection()` and must NOT close it.
- Timestamps stored as ISO-8601 strings (`Instant.toString()` / `Instant.parse`).
- `CardStatus` uses stable string codes: `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CLOSED`.
- **Dev bootstrap**: `DevUserBootstrap.ensureInitialUser()` runs at startup only when `users` is empty;
  username `dev`; password from env `TASKBOARD_DEV_PASSWORD`, default `Dev-1234` (dev-only, documented,
  not a production secret). No password is logged.
- **NavigationService** owns scene creation + stylesheet loading; controllers are injected via
  `FXMLLoader.setController(...)` so FXML has no `fx:controller` dependency (Scene Builder compatible).
- Controllers get services via constructor injection from `Main`; no static holders.
- Login failure messages never reveal which credential was wrong; SQL exceptions are logged internally
  and shown to the user as "Unable to sign in. Please try again."
- `Main.fxml` (Phase 0 placeholder) was removed; Login.fxml is the initial view.

## 6. Verification Commands

```bash
mvn clean test          # automated tests
mvn javafx:run          # run app (zero setup)
```

Manual checks after running:
- Login screen appears on start.
- Login with `dev` / `Dev-1234` succeeds → dashboard shows "Logged in as: dev".
- Wrong password / empty fields show friendly error messages (no stack traces).
- Logout returns to login screen; login again works.
- `data/taskboard.db` persists; restart does not re-apply migrations.
- `logs/taskboard.log` shows structured startup, bootstrap, session open/close lines.

## 7. Phase 4 Requirements (next — from plan.md section 12)

- Trello-like board UI: columns for PLANNED / IN_PROGRESS / COMPLETED / CLOSED.
- `Board.fxml`, `BoardController`; cards as reusable components.
- No drag-and-drop yet (status changes via card editor — Phase 5).
- CSS: `board.css`, `card.css`; styling stays in CSS.
- Board loads, columns display, empty columns correct, scrolling, resizing.
- Must not break Phases 0–3; stop after Phase 4 for user verification.

## 8. Known Pitfalls (learned)

1. **Shared connection must never be closed by callers** — `db.getConnection()` returns the app-wide connection; only `DatabaseManager.close()` may close it.
2. **Flyway 9.x API differences** (above) — `dataSource(String,String,String)`, `MigrateResult`, `api.FlywayException`.
3. **JavaFX modular runtime** — IntelliJ needs `--module-path` + `--add-modules` with the `-win` jars.
4. Git Bash on Windows: `taskkill /PID` needs `//PID` (path mangling).
5. sqlite-jdbc needs `slf4j-api` on classpath for standalone runs (Maven resolves it transitively).
6. Lambda bodies passed to `Function<Connection, T>` cannot throw checked `SQLException` — catch inside and rethrow as `DatabaseException`.
7. **Windows classpath separator is `;`** — when running `java` with Windows paths from Git Bash, use `;` (not `:`) and backslash paths; `C:` in paths is fine only with `;` separators.
8. **`at.favre:bcrypt` is not published on Maven Central** — use `org.mindrot:jbcrypt:0.4`.
9. Flyway needs `gson` on the classpath for standalone runs (Maven resolves it transitively).

## 9. Immediate Next Steps

1. Wait for user to verify Phase 3 (run `mvn clean test` + `mvn javafx:run`, log in with `dev`/`Dev-1234`, create/edit/delete a workspace, restart, check persistence).
2. On confirmation: implement Phase 4 (Board + Kanban UI: Board.fxml, BoardController, board.css/card.css, columns per status, no drag-and-drop yet), keep all tests green, run the app, verify, then STOP and ask for verification.
