# JavaFX Trello-Like Task Board — Incremental Plan

## 1. Objective
Build a production-quality desktop Trello-like task manager with JavaFX. Develop **one phase at a time**; every phase must compile, run, pass relevant tests, and be manually verified before the next phase.

Features:
- Login; workspace CRUD; boards/Kanban; card CRUD
- Card statuses: `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CLOSED`
- Due dates; Markdown descriptions/checklists; images in Markdown
- Persistent disk SQLite DB; migrations; structured logging; graceful errors; automated business-logic tests
- FXML + Scene Builder-compatible UI; CSS-separated styling

## 2. Mandatory Agent Rules
For each phase:
1. Inspect current implementation.
2. Briefly plan files, DB changes, tests, behavior.
3. Implement **only the current phase**.
4. Run `mvn clean test`; fix failures.
5. Run `mvn javafx:run` (or configured equivalent).
6. Perform phase checklist and relevant tests.
7. Confirm existing features still work.
8. Report phase/status, implementation, tests, manual verification, issues, changed files.
9. **STOP and wait for explicit user verification.**

Never blindly replace working code or refactor unnecessarily. Never start a later phase without user confirmation.

## 3. Architecture / UI / DB
UI stack: JavaFX + FXML + Scene Builder + JavaFX CSS.
- FXML = structure/layout; Java = behavior; CSS = styling.
- Avoid UI construction in Java unless technically justified.
- Keep major screens Scene Builder-editable.
- Use semantic CSS classes; don't hard-code visual styling in controllers.

Layering:
`JavaFX UI → Controllers/ViewModels → Services → Repositories → Database`
- No SQL in controllers; business rules belong in services.

DB:
- SQLite + JDBC + Flyway; persistent `data/taskboard.db`
- Auto-create `data/` and DB.
- Enable `PRAGMA foreign_keys = ON` on every connection.
- Use transactions for multi-step atomic changes.
- Never use an in-memory DB for the real app.

## 4. Technology
- Java 21+; prefer Java 21 LTS, but don't use features newer than configured Java.
- Maven, JavaFX, FXML, Scene Builder, SQLite/JDBC, Flyway, SLF4J, Logback, JUnit 5.
- Recommended only when useful: ControlsFX, BCrypt/Argon2, Flexmark-Java, Mockito, TestFX.
- Avoid unnecessary dependencies; each must solve a real requirement.

## 5. Target Structure
```text
src/main/java/com/example/taskboard/
  Main.java
  config/{AppConfig.java,AppPaths.java}
  database/{DatabaseManager.java,MigrationManager.java}
  model/{User.java,Workspace.java,Board.java,Card.java,CardStatus.java,CardAttachment.java,DueDateStatus.java}
  repository/{UserRepository.java,WorkspaceRepository.java,BoardRepository.java,CardRepository.java,CardAttachmentRepository.java}
  service/{AuthService.java,WorkspaceService.java,BoardService.java,CardService.java,DueDateService.java,MarkdownService.java,AttachmentService.java}
  session/SessionManager.java
  controller/{LoginController.java,DashboardController.java,WorkspaceController.java,BoardController.java,CardController.java}
  exception/{AppException.java,DatabaseException.java,AuthenticationException.java,ValidationException.java}
  util/{DateUtils.java,ValidationUtils.java}

src/main/resources/
  fxml/{Login.fxml,Dashboard.fxml,Board.fxml,WorkspaceDialog.fxml,CardDialog.fxml}
  css/{app.css,login.css,dashboard.css,board.css,card.css}
  db/migration/{V1__initial_schema.sql,...}

src/test/java/com/example/taskboard/
```
Package may change if explicitly specified.

## 6. Domain
`User → Workspace → Board → Card → Attachments`
- Workspace has one or more boards.
- Card belongs to exactly one board.

## 7. SQLite Schema
```sql
PRAGMA foreign_keys = ON;

users(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 username TEXT NOT NULL UNIQUE,
 password_hash TEXT NOT NULL,
 created_at TEXT NOT NULL,
 updated_at TEXT NOT NULL
);

workspaces(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 name TEXT NOT NULL,
 description TEXT,
 created_at TEXT NOT NULL,
 updated_at TEXT NOT NULL
);

boards(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 workspace_id INTEGER NOT NULL,
 name TEXT NOT NULL,
 description TEXT,
 created_at TEXT NOT NULL,
 updated_at TEXT NOT NULL,
 FOREIGN KEY(workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

cards(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 board_id INTEGER NOT NULL,
 title TEXT NOT NULL,
 description TEXT,
 status TEXT NOT NULL,
 position REAL NOT NULL,
 due_date TEXT,
 created_at TEXT NOT NULL,
 updated_at TEXT NOT NULL,
 completed_at TEXT,
 FOREIGN KEY(board_id) REFERENCES boards(id) ON DELETE CASCADE
);

card_attachments(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 card_id INTEGER NOT NULL,
 file_name TEXT NOT NULL,
 file_path TEXT NOT NULL,
 mime_type TEXT,
 created_at TEXT NOT NULL,
 FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE CASCADE
);
```
Use PK/FK/unique constraints/indexes as appropriate. Never store plaintext passwords. Persist `CardStatus` as stable strings, never enum ordinals.

## 8. Date/Time
Use `java.time`: `LocalDate` for date-only deadlines, `Instant` for timestamps. Avoid `java.util.Date/Calendar` except required APIs. Store timestamps consistently as ISO-8601 (e.g. `2026-09-08T10:15:30Z`).

---

# 9. Development Phases

## Phase 0 — Project Foundation
Goal: initial runnable JavaFX infrastructure.

Implement:
- Maven + Java 21+
- JavaFX entry point
- FXML loading
- CSS loading
- Logback + `logs/`
- app config/resource structure
- `Main.java`, `Main.fxml`, `app.css`
- Simple window proving app runs
- FXML must open in Scene Builder and use no unsupported/generated constructs
- Log startup; never log passwords/credentials

Verify:
- `mvn clean test`
- `mvn javafx:run`
- app/window/FXML/CSS/log file work; Scene Builder opens FXML; no stack traces

**STOP; request user verification.**

## Phase 1 — SQLite + DB Infrastructure
Goal: persistent storage without major UI change.

Implement `DatabaseManager`, `MigrationManager`, SQLite at `data/taskboard.db`, Flyway `V1__initial_schema.sql`, tables above, repository foundations. Don't implement unnecessary full CRUD yet.

Verify:
- auto-created `data/taskboard.db`
- migration runs once and persists across restart
- tables/constraints work
- DB errors logged
- tests: initialization, migration, connections, FK enforcement

**STOP.**

## Phase 2 — Authentication/Login
UI: `Login.fxml`, `LoginController`.
Backend: `UserRepository`, `AuthService`, `SessionManager`.
- Secure BCrypt/Argon2 hashing; never plaintext comparison/storage.
- Controlled dev-only initial-user bootstrap; never hard-code production password.
  Flow: `Start → Login → AuthService → UserRepository → SQLite → Dashboard`.
  Handle empty fields, unknown user, wrong password, DB failure with friendly messages; don't expose SQL/stack traces.

Verify login success/failure, empty fields, restart, logout, re-login.
Tests: hashing, verification, auth success/failure, session creation/logout.

**STOP.**

## Phase 3 — Workspace CRUD
Dashboard lists workspaces; workspace dialog has name/description and Cancel/Save.
Implement `WorkspaceRepository`, `WorkspaceService`, `WorkspaceController`; Create/Read/Update/Delete.
Validate name: required, trimmed, nonblank, reasonable max length.
Delete requires confirmation; DB cascade/transaction removes boards/cards.

Verify create/display/edit/delete, persistence, destructive delete behavior.
Tests: service validation + repository CRUD.

**STOP.**

## Phase 4 — Board + Kanban UI
Create Trello-like columns:
`PLANNED | IN PROGRESS | COMPLETED | CLOSED`.
Use HBox/VBox/ScrollPane/BorderPane/StackPane as appropriate; cards are reusable JavaFX components.
**No drag/drop yet**; status can change through editor.
Use `board.css`/`card.css`.

Verify board/columns/cards/empty columns/scrolling/resizing and CSS-only visual changes.

**STOP.**

## Phase 5 — Card CRUD
Card fields:
`title, description, status, due_date, position, created_at, updated_at, completed_at`.
Create `CardDialog.fxml` + `CardController`; CRUD.
Stable string status values.
Recommended completed timestamp:
- enter `COMPLETED` → set `completed_at`
- leave `COMPLETED` → clear it
- `CLOSED` remains separate final status
  Document behavior.

Verify create/edit/open/status/delete/restart/persistence.

**STOP.**

## Phase 6 — Due Dates
Implement `DueDateService`, `DueDateStatus`:
`NONE, UPCOMING, DUE_TODAY, OVERDUE, COMPLETED`.

Rules:
- no date → NONE
- future → UPCOMING
- today → DUE_TODAY
- past → OVERDUE
- completed card → COMPLETED (not overdue)

UI examples: `Due Sep 12`, `Due today`, `Overdue`.
Use CSS classes (`due-normal`, `due-today`, `due-overdue`, `due-completed`), not Java colors.

Test no date, yesterday, today, tomorrow, future, completed, closed; unit-test date boundaries and status transitions.

**STOP.**

## Phase 7 — Markdown
Use Flexmark-Java/mature parser; **never write custom Markdown parser**.
Store original Markdown in `cards.description`, not only rendered HTML.
Support headings, bold, italic, ordered/unordered lists, links, inline/code blocks, checkboxes, images.

UI: EDIT/PREVIEW toggle. EDIT edits Markdown; PREVIEW renders it (JavaFX WebView acceptable).
Security: sanitize/configure parser; don't allow unsafe HTML/scripts or arbitrary local/remote JavaScript.

Verify rich Markdown, save, restart, reopen, original Markdown preserved and renders correctly.

**STOP.**

## Phase 8 — Image Attachments
Storage:
```text
data/
  taskboard.db
  attachments/
    card-1/{image1.png,screenshot.jpg}
    card-2/image.png
```
Don't store large images in SQLite unless compelling.
Implement `AttachmentService`, `CardAttachmentRepository`, `[Attach Image]`.
On attach: validate → create card dir → copy → DB record → insert Markdown reference → refresh preview.
Use safe app URI, e.g. `![Screenshot](attachment://42)`. Resolve safely; reject traversal (`../../...`).

Verify PNG/JPG/JPEG, invalid type, large file, missing/deleted attachment, restart/render persistence.

**STOP.**

## Phase 9 — Drag & Drop
Support:
- reorder within same column
- move between status columns

Persist `status`, `position`, `updated_at` transactionally. Use existing `position REAL`, not IDs; sequential positions initially, optimize later if needed.

Move transaction:
`BEGIN → update status/position → COMMIT`; failure → `ROLLBACK`.

Verify same-column/other-column, first/last, multi-card reorder, restart/order persistence.

**STOP.**

## Phase 10 — Production Hardening
Logging: SLF4J + Logback.
Log startup/shutdown, auth success/failure (no passwords), DB init, CRUD failures, unexpected exceptions, attachment failures.
Never log passwords, hashes, sensitive user data, or unnecessary full Markdown.
Levels: TRACE/DEBUG/INFO/WARN/ERROR; INFO for important events, DEBUG diagnostics, ERROR failures.

### Global errors
Normal user-operation failures must not crash app. Show friendly messages (e.g. “Unable to save the card. Please try again.”); log real exception internally.
Exceptions: `AppException`, `DatabaseException`, `AuthenticationException`, `ValidationException`; use exception chaining.

### Validation
Centralize where practical; validate in services as well as UI.
- Workspace: required name + max length
- Card: required/max title; description max if needed
- Attachment: allowed image types + max size

### Tests
JUnit 5:
- Unit: AuthService, WorkspaceService, CardService, DueDateService, MarkdownService, validation
- Repository: temporary DB; insert/update/delete/find, relationships, cascades, transactions
- UI/TestFX where practical: Login, Dashboard, Workspace dialog, Card dialog
  Don't make fragile tests for every CSS detail.

### Threading
JavaFX operations on FX Application Thread. DB/file/large Markdown work must not block it; use `Task`, `Service`, or `ExecutorService` when actually needed. Don't add async complexity without reason.

### CSS
Major styling externalized:
`app.css`, `login.css`, `dashboard.css`, `board.css`, `card.css`.
Use semantic classes such as `.board-column`, `.card`, `.card-title`, `.card-description`, `.card-due-date`, `.card-due-today`, `.card-overdue`, `.card-completed`, `.workspace-item`. Java applies/removes classes; CSS controls appearance.

### Navigation
Use a central `NavigationService` to switch views, preserve session, avoid duplicated navigation, and handle logout.
Flow: `Application → Login → Dashboard → Workspace → Board → Card Dialog`.
Logout: `Dashboard → SessionManager.logout() → Login`.

---

# 10. Persistence / Backup / Security / Performance

Persist users, workspaces, boards, cards, statuses, positions, due dates, Markdown, attachment metadata/files. Restart must not lose authoritative data.

Backup-friendly:
`data/taskboard.db` + `data/attachments/`.
Future export/import only if requested.

Security baseline:
- hashed passwords; no plaintext/password logging
- `PreparedStatement` everywhere
- never concatenate user input into SQL
- safe attachment paths
- input validation
- safe Markdown rendering

Performance target: personal/small-team datasets. Don't prematurely optimize; use useful indexes, avoid unnecessary loads/UI blocking, paginate later if needed, avoid repeatedly rendering huge Markdown or loading full-size images unnecessarily.

## 11. Definition of Done — Every Phase
A phase is complete only if:
- [ ] compiles
- [ ] starts
- [ ] feature manually works
- [ ] existing features still work
- [ ] relevant tests pass
- [ ] no obvious log errors
- [ ] FXML remains Scene Builder-compatible where applicable
- [ ] CSS remains externally editable
- [ ] DB changes are migrated
- [ ] no plaintext secrets
- [ ] user-facing errors are understandable

## 12. Exact Agent Workflow
**Inspect:** project/classes/FXML/CSS/migrations/previous phase.
**Plan:** files, DB changes, tests, expected behavior.
**Implement:** current phase only.
**Compile:** `mvn clean test`.
**Run:** `mvn javafx:run` or configured equivalent.
**Verify:** phase checklist.
**Report:**
```text
Phase:
Status:
Implemented:
- ...
Tests:
- ...
Manual verification:
- ...
Known issues:
- ...
Files changed:
- ...
```
**STOP** and wait for explicit user confirmation, e.g. `Phase verified. Continue.`

## 13. Prohibited
Do not:
- implement multiple phases at once or skip verification
- silently change architecture or replace SQLite without approval
- add unnecessary frameworks
- store plaintext passwords
- put SQL/business logic in controllers/FXML
- hard-code Java styling
- store attachments as arbitrary Base64 in Markdown
- persist absolute filesystem paths in Markdown
- swallow exceptions
- use `System.out.println` for app logging
- ignore transactions
- break Scene Builder compatibility
- remove/disable tests to pass builds
- continue without user confirmation

## 14. Future — Do Not Initially Implement
Unless explicitly requested:
multiple users/workspace roles/invitations, real-time collaboration, cloud sync, REST API, notifications/email, mobile app, calendar integration, recurring tasks, labels, comments, activity history, separate checklist entities, non-image attachments, full-text search, Kanban analytics.

## 15. Final Target
```text
TASKBOARD
  Login
    ↓
  Dashboard
    ↓
  Workspace
    ↓
  Board
    ├─ Planned
    ├─ In Progress
    ├─ Completed
    └─ Closed
         ↓
       Card
      ├─ Markdown
      ├─ Due Date
      └─ Images
         ↓
      Renderer → JavaFX UI
```
Result: lightweight desktop Trello alternative, maintainable as a JavaFX project.

## 16. First Instruction to Coding Agent
> Read PLAN.md completely before changing anything.
> Start with **Phase 0 only**.
> Inspect the current repository first; if empty, create JavaFX Maven project using Java 21+.
> Implement only Phase 0.
> Compile/test and run the JavaFX app if possible.
> Report implementation, changed files, verification commands, and whether it started.
> Do NOT implement Phase 1+.
> Stop after Phase 0 and wait for my verification.
> Don't ask unnecessary questions; use reasonable defaults. Ask only for genuinely essential missing requirements before irreversible architecture decisions.

## 17. Progress Tracker
```text
[x] Phase 0  — Project Foundation
[x] Phase 1  — SQLite + Database Infrastructure
[x] Phase 2  — Authentication + Login
[x] Phase 3  — Workspace CRUD
[x] Phase 4  — Board + Kanban UI
[x] Phase 5  — Card CRUD
[x] Phase 6  — Due Dates (verified by user)
[x] Phase 7  — Markdown (agent-verified; awaiting user verification)
[ ] Phase 8  — Image Attachments
[ ] Phase 9  — Drag & Drop
[ ] Phase 10 — Production Hardening
```
Only mark a phase complete after user manual verification.

## 18. Success Criteria
1. Reliable launch/login.
2. Workspace create/edit/view/delete.
3. Kanban board with four statuses.
4. Card create/edit/view/delete; status and ordering persist.
5. Correct due dates/display.
6. Markdown + checkboxes preserved/rendered.
7. Images attach/render.
8. All data survives restart; migrations reliable.
9. Important business logic tested.
10. Structured logging + graceful user errors.
11. Scene Builder-editable UI + CSS-driven visual changes.
12. No obvious security issues (plaintext passwords/SQL injection).
13. Every phase independently verified before the next.

## END
