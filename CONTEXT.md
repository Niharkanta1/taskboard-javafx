# Context Checkpoint — JavaFX Trello-Like Task Board

Last updated: Phase 7 (Markdown) complete and agent-verified (FXML card-dialog bug fixed); awaiting user verification.

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
[x] Phase 3  — Workspace CRUD           (verified with Phase 5 re-verification)
[x] Phase 4  — Board + Kanban UI        (verified by user; ScrollPane bug fixed)
[x] Phase 5  — Card CRUD                (verified by user)
[x] Phase 6  — Due Dates                (verified by user)
[x] Phase 7  — Markdown                (agent-verified; awaiting user verification)
[ ] Phase 8  — Image Attachments
[ ] Phase 9  — Drag & Drop
[ ] Phase 10 — Production Hardening
```

Tracker lives in `plan.md` section 36. Update it when a phase is verified.

## 3. Current State (verified)

- `mvn clean test` → **BUILD SUCCESS, 163/163 tests pass** (123 previous + 37 `MarkdownServiceTest` + 1 `CardDialogFxmlTest`).
- `mvn javafx:run` → app starts; login screen is the first view; dashboard shows the workspace list with an "Open Workspace" button; due-date labels render on board cards; card descriptions render as Markdown (headings, bold/italic, lists, task lists with checkboxes, fenced code, block quotes, http/https links).
- Phase 7 flow: card editor (CardDialog) shows a live, non-interactive Markdown preview next to the raw Markdown field; board cards render Markdown; clicking a task checkbox on a board card toggles it (`[ ]` <-> `[x]`) and persists via `CardService.updateCard` + board refresh.
- **Bug fixed (2026-09-09, user report):** `CardDialog.fxml` failed to load with `Property "fitWidth" does not exist or is read-only` — `fitWidth`/`fitHeight` are read-only `Pane` properties; `ScrollPane` uses `fitToWidth`/`fitToHeight`. Also removed the invalid `dividerPosition` attribute on `SplitPane` (dividers auto-sync with items; the default divider position is 0.5). `CardDialogFxmlTest` now loads the dialog through `FXMLLoader` with a real controller as a permanent regression.
- Phase 5 flow: dashboard → workspace view (board list; double-click or Open opens a board) → board view (kanban columns, "New Card" button per column, click a card to edit; card editor supports title, status, due date, description, save, cancel, delete with confirmation).
- Phase 6 flow: cards with a due date show a due-date label (`Overdue` / `Due today` / `Due <date>` / `Completed`); no due date → no label; COMPLETED/CLOSED cards always show `Completed`.
- Double-click navigation: workspace rows on the dashboard and board rows on the workspace view open the target view; existing Open buttons unchanged.
- `Board.fxml` and `CardDialog.fxml` both verified to load via direct FXMLLoader check (throwaway verifier, removed afterwards).
- **Bug fixed (2026-09-09):** `BoardController.refreshBoard()` now reassigns `board = boardService.loadBoard(...)` (was discarding the result, leaving `board.getCards()` stale/null). `groupCardsByStatus` is null-safe. End-to-end flow check (real DB + real FXML) confirms: save → card appears; edit to COMPLETED → moves column + `completed_at` set.
- **Note:** the user's 11:39 `LoadException` (`ClassNotFoundException: javafx.scene.layout.ScrollPane`) came from a stale build; the current `target/classes` has the correct `javafx.scene.control.ScrollPane` import, so a fresh build opens the board.
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
  model/User.java, Workspace.java, Board.java, Card.java, CardStatus.java, CardAttachment.java, DueDateStatus.java
  repository/UserRepository.java, WorkspaceRepository.java, BoardRepository.java,
            CardRepository.java, CardAttachmentRepository.java
  repository/impl/*Impl.java        # User insert + reads; Workspace insert/update/find/findAll/count/delete
  service/AuthService.java          # BCrypt hash/verify, login validation
  service/DevUserBootstrap.java     # dev-only initial user (env-overridable password)
  service/WorkspaceService.java     # workspace validation + CRUD (name <=100, description <=500)
  service/BoardService.java         # board validation, createBoard, loadBoard (with cards), findBoardsByWorkspace
  service/CardService.java          # card validation + createCard/updateCard/deleteCard; position = max+1; completed_at rules
  service/DueDateService.java       # due-date status, display text, CSS class mapping
  service/MarkdownService.java      # Flexmark-backed Markdown parsing (blocks, inline segments) + toggleTask
  service/NavigationService.java    # central scene switching (login <-> dashboard <-> workspace <-> board); getStage()
  session/SessionManager.java       # in-memory current user
  controller/LoginController.java   # login form + friendly errors
  controller/DashboardController.java # welcome label, workspace list, open/create/edit/delete, logout; double-click opens workspace
  controller/WorkspaceDialogController.java # modal create/edit workspace dialog
  controller/WorkspaceController.java  # workspace view: board list, new board dialog, open board; double-click opens board
  controller/BoardDialogController.java  # modal create board dialog
  controller/BoardController.java       # kanban view: New Card per column, card click opens editor, refresh after changes
  controller/CardDialogController.java  # modal create/edit card dialog (title, status, due date, description, live Markdown preview, delete)
  view/CardView.java                  # clickable card component (title, Markdown description, due-date label; task-toggle callback; opens editor on click)
  view/MarkdownRenderer.java          # renders Markdown blocks as plain JavaFX nodes (no HTML/WebView)

src/main/resources/
  fxml/Login.fxml
  fxml/Dashboard.fxml
  fxml/WorkspaceDialog.fxml
  fxml/Workspace.fxml
  fxml/Board.fxml
  fxml/BoardDialog.fxml
  fxml/CardDialog.fxml
  css/app.css, css/login.css, css/dashboard.css, css/board.css, css/card.css  # due-date classes + Markdown classes: card-markdown, md-h1..md-h3, md-bold, md-italic, md-code, md-link, md-bullet, card-markdown-task-item, card-markdown-codeblock, md-blockquote
  logback.xml
  db/migration/V1__initial_schema.sql

src/test/java/com/example/taskboard/
  ResourceSmokeTest.java
  database/DatabaseIntegrationTest.java
  service/AuthServiceTest.java          # in-memory fake UserRepository
  service/AuthenticationIntegrationTest.java  # temp-dir SQLite
  service/WorkspaceServiceTest.java     # in-memory fake WorkspaceRepository
  service/BoardServiceTest.java         # in-memory fake Board/Card/Workspace repositories
  service/CardServiceTest.java          # in-memory fake Card/Board repositories; validation, position, completed_at
  service/DueDateServiceTest.java       # due-date boundaries (fixed reference date) + display text + CSS classes
  service/MarkdownServiceTest.java      # Flexmark parsing: blocks, inline segments, links, task toggling (37 tests)
  CardDialogFxmlTest.java               # FXMLLoader load of CardDialog.fxml with real controller (FXML property-binding regression)
  repository/WorkspaceIntegrationTest.java  # temp-dir SQLite CRUD + cascade
  repository/BoardIntegrationTest.java    # temp-dir SQLite board/card persistence + ordering + cascade
  repository/CardIntegrationTest.java     # temp-dir SQLite card CRUD + completed_at + FK + cascade + null due-date round trip
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
- Due-date logic lives in `DueDateService` (pure, testable, date-only `LocalDate` semantics).
- Both `COMPLETED` and `CLOSED` are final statuses: their due dates are reported as `COMPLETED`, never `OVERDUE`.
- A due date equal to today is `DUE_TODAY` at any time of day, including midnight (date-only).
- No due date → `DueDateStatus.NONE` → no label rendered.
- Due-date styling lives in `card.css` (classes: `card-due-date`, `due-normal`, `due-today`, `due-overdue`, `due-completed`); no hardcoded colors in Java.
- **Markdown (Phase 7)**: Flexmark `0.42.14` (`com.vladsch:flexmark-all`) with `TaskListExtension`. Descriptions are stored as raw Markdown; `MarkdownService` parses blocks (headings, paragraphs, list items, task items, fenced/indented code blocks, block quotes) and inline segments (bold, italic, code, links). `MarkdownRenderer` renders everything as plain JavaFX nodes (`Label`, `Text` in `TextFlow`, `CheckBox`, `TextArea`) — no HTML, no `WebView`, no scripting. Only `http`/`https` links are link-styled and open via `HostServices`; everything else (incl. HTML-like text and unsafe URLs) is plain text. Task toggling: `MarkdownService.toggleTask(markdown, index)` rewrites `[ ]`/`[x]` in place; `BoardController.toggleCardTask` persists via `CardService.updateCard` + `refreshBoard`. CardDialog preview is non-interactive (toggling happens on the board view after save).
- Flexmark AST notes: fenced and indented code blocks are sibling classes of `CodeBlock` (`FencedCodeBlock`, `IndentedCodeBlock`, all extend `Block`) — all three must be matched; code lines come from `ContentNode.getContentLines()` (each line includes its trailing line break). Paragraph `getChars()` may include the trailing line break separating it from the next block — strip it.
- **Dev bootstrap**: `DevUserBootstrap.ensureInitialUser()` runs at startup only when `users` is empty;
  username `dev`; password from env `TASKBOARD_DEV_PASSWORD`, default `Dev-1234` (dev-only, documented,
  not a production secret). No password is logged.
- **NavigationService** owns scene creation + stylesheet loading; controllers are injected via
  `FXMLLoader.setController(...)` so FXML has no `fx:controller` dependency (Scene Builder compatible).
- Controllers get services via constructor injection from `Main`; no static holders.
- Login failure messages never reveal which credential was wrong; SQL exceptions are logged internally
  and shown to the user as "Unable to sign in. Please try again."
- `Main.fxml` (Phase 0 placeholder) was removed; Login.fxml is the initial view.
- **`Board.cards` is transient view data** — populated by `BoardService.loadBoard`, never persisted;
  repositories neither read nor write it.
- JavaFX 21: `javafx.scene.layout.Priority` is an enum with `ALWAYS`, `SOMETIMES`, `NEVER`
  (the old `LOW`/`MEDIUM`/`HIGH` constants were removed); use `Priority.ALWAYS` for grow.

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

## 7. Phase 5 Requirements (done — from plan.md section 13)

- Card CRUD inside a board: create, edit, delete; board refreshes after every change.
- `CardService` owns validation and persistence: title required (max 200), description optional (max 5000), board must exist.
- New card position = `max(position) + 1` for the board.
- `completed_at` rules: entering `COMPLETED` sets it to now; leaving `COMPLETED` (including to `CLOSED`) clears it to NULL; staying `COMPLETED` keeps the original value.
- `CardDialog.fxml` + `CardDialogController`: modal create/edit dialog (title, status, due date, description); delete button only in edit mode, with confirmation.
- `CardView` is clickable (opens the card editor); cursor: hand via CSS.
- Double-click navigation: workspace rows (dashboard) and board rows (workspace view) open the target view; existing Open buttons kept.
- No database schema changes (cards table exists from V1).
- Must not break Phases 0–4; stop after Phase 5 for user verification.

## 8. Phase 6 Requirements (done — from plan.md section 14)

- `DueDateService` + `DueDateStatus` (NONE, UPCOMING, DUE_TODAY, OVERDUE, COMPLETED).
- No due date → NONE (no label); future → UPCOMING ("Due Sep 12"); today → DUE_TODAY ("Due today"); past → OVERDUE ("Overdue").
- COMPLETED and CLOSED cards → COMPLETED status; never displayed as overdue.
- CSS classes: `card-due-date`, `due-normal`, `due-today`, `due-overdue`, `due-completed` (no hardcoded colors in Java).
- Unit tests: date boundaries with a fixed reference date (midnight/today/tomorrow/yesterday, status transitions).
- Integration test: null due-date round trip.
- No database schema changes (due_date exists from V1).
- Must not break Phases 0–5; stop after Phase 6 for user verification.

## 9. Known Pitfalls (learned)

1. **Shared connection must never be closed by callers** — `db.getConnection()` returns the app-wide connection; only `DatabaseManager.close()` may close it.
2. **Flyway 9.x API differences** (above) — `dataSource(String,String,String)`, `MigrateResult`, `api.FlywayException`.
3. **JavaFX modular runtime** — IntelliJ needs `--module-path` + `--add-modules` with the `-win` jars.
4. Git Bash on Windows: `taskkill /PID` needs `//PID` (path mangling).
5. sqlite-jdbc needs `slf4j-api` on classpath for standalone runs (Maven resolves it transitively).
6. Lambda bodies passed to `Function<Connection, T>` cannot throw checked `SQLException` — catch inside and rethrow as `DatabaseException`.
7. **Windows classpath separator is `;`** — when running `java` with Windows paths from Git Bash, use `;` (not `:`) and backslash paths; `C:` in paths is fine only with `;` separators.
8. **`at.favre:bcrypt` is not published on Maven Central** — use `org.mindrot:jbcrypt:0.4`.
12. **`ScrollPane` is in `javafx.scene.control`, not `javafx.scene.layout`** — a wrong FXML import fails at runtime with `ClassNotFoundException` (Phase 4 bug, fixed 2026-09-09). Regression test now checks every FXML `<?import>` class exists (`Class.forName(name, false, loader)` — `initialize=false` avoids JavaFX toolkit static init in tests).
9. Flyway needs `gson` on the classpath for standalone runs (Maven resolves it transitively).
10. **JavaFX 21 `Priority` is an enum** (`ALWAYS`/`SOMETIMES`/`NEVER`) — `Priority.HIGH` etc. do not compile; use `Priority.ALWAYS` for `setHgrow`/`setVgrow`.
11. Integration tests share one temp DB per test class — assertions must be scoped to the test's own rows (global `count()` assertions break when test order changes).
12. **FXML can only set *settable* properties** — read-only properties (e.g. `Pane.fitWidth`/`fitHeight`) fail at load time with `Property "..." does not exist or is read-only`. `ScrollPane` uses `fitToWidth`/`fitToHeight`; `SplitPane` has no settable `dividerPosition` (dividers auto-sync with items; default position 0.5). `ResourceSmokeTest` only checks resource existence + imports, so `CardDialogFxmlTest` (FXMLLoader load with a real controller) is the regression guard for FXML property-binding bugs.

## 10. Immediate Next Steps

1. Wait for user to verify Phase 7 (Markdown):
   - `mvn clean test` → 163/163 expected.
   - `mvn javafx:run` → log in with `dev`/`Dev-1234`; open a workspace and a board.
   - Create/edit a card with a Markdown description: `# Heading`, `**bold**`, `*italic*`, `` `code` ``, bullet/ordered lists, `- [ ]` / `- [x]` task lists, fenced code blocks, `> blockquote`, `[link](https://...)`.
   - Card editor: live preview renders next to the raw Markdown field (preview checkboxes are NOT clickable in the dialog).
   - Board card: rendered Markdown appears; clicking a task checkbox toggles `[ ]` <-> `[x]`, persists after refresh, and survives restart.
   - Only http/https links are clickable/styled; other protocols and HTML-like text render as plain text.
   - Phases 0–6 still work (login, workspace CRUD, board creation, card CRUD, due dates).
2. On confirmation: implement Phase 8 (Image Attachments), keep all tests green, run the app, verify, then STOP and ask for verification.

> **Important for re-verification:** run a fresh build with `mvn clean javafx:run` (not a stale IntelliJ/`target/classes` build).
