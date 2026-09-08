# JavaFX Trello-Like Task Board — Incremental Development Plan

## 1. Project Objective

Build a production-quality, desktop Trello-like task management application using JavaFX.

The application must be developed **incrementally in phases**. Every phase must result in a **compilable, runnable application** and must be manually verified before development proceeds to the next phase.

The application should support:

- User login
- Workspace CRUD
- Board/Kanban UI
- Card CRUD
- Card statuses:
  - Planned
  - In Progress
  - Completed
  - Closed
- Proper due-date handling
- Markdown card descriptions
- Markdown checkboxes
- Image support inside card descriptions
- Persistent disk-based database
- SQLite preferred
- Better structured logging
- Error handling
- Automated tests for important business logic
- UI designed using FXML and editable in Scene Builder
- Styling separated into CSS so the visual design can be changed without rewriting business logic

---

# 2. Development Rules for the AI Agent

These rules are mandatory.

## 2.1 Incremental Development

Do NOT implement the entire application at once.

Implement exactly one phase at a time.

After completing a phase:

1. Compile the project.
2. Run the application.
3. Execute the phase's manual verification checklist.
4. Execute automated tests relevant to the phase.
5. Fix all errors.
6. Confirm that existing functionality still works.
7. STOP.

Do not begin the next phase until the user explicitly confirms that the current phase has been verified.

---

## 2.2 Never Break Existing Features

Before modifying existing functionality:

- Understand the current implementation.
- Preserve existing behavior.
- Avoid unnecessary refactoring.
- Do not replace working code merely for stylistic reasons.

Every new phase must build on the previous phase.

---

## 2.3 UI Architecture

Use:

- JavaFX
- FXML
- Scene Builder-compatible FXML
- JavaFX CSS

FXML should contain UI structure.

Java code should contain behavior.

CSS should contain visual styling.

Do not hard-code visual styling throughout Java controllers.

Avoid creating UI entirely through Java code unless there is a strong technical reason.

---

## 2.4 Database Architecture

Use SQLite as the default database.

Use:

- JDBC
- SQLite JDBC driver
- Flyway for database migrations

The database must be stored on disk.

Example:

```text
data/
    taskboard.db
```

Never use an in-memory database for the real application.

The application must automatically create the required database directory and database file if they do not exist.

---

## 2.5 Architecture

Use a layered architecture.

Preferred structure:

```text
JavaFX UI
    ↓
Controllers / ViewModels
    ↓
Services
    ↓
Repositories
    ↓
Database
```

Do not place SQL directly inside JavaFX controllers.

Do not place business rules directly inside FXML controllers when they belong in services.

---

# 3. Technology Stack

Use versions compatible with **Java 21 or newer**.

Prefer Java 21 LTS when the environment supports it, but do not use Java features that require a version newer than the project's configured Java version unless explicitly requested.

## Required

- Java 21+
- JavaFX
- Maven
- FXML
- Scene Builder
- SQLite
- JDBC
- Flyway
- SLF4J
- Logback
- JUnit 5

## Recommended

- ControlsFX
- BCrypt or Argon2 password hashing
- Flexmark-Java for Markdown
- Mockito
- TestFX for JavaFX UI tests where practical

Do not add dependencies unnecessarily.

Before adding a dependency, verify that it solves a real requirement.

---

# 4. Target Project Structure

Use a package structure similar to:

```text
src/
├── main/
│   ├── java/
│   │   └── com/example/taskboard/
│   │       ├── Main.java
│   │       │
│   │       ├── config/
│   │       │   ├── AppConfig.java
│   │       │   └── AppPaths.java
│   │       │
│   │       ├── database/
│   │       │   ├── DatabaseManager.java
│   │       │   └── MigrationManager.java
│   │       │
│   │       ├── model/
│   │       │   ├── User.java
│   │       │   ├── Workspace.java
│   │       │   ├── Board.java
│   │       │   ├── Card.java
│   │       │   ├── CardStatus.java
│   │       │   ├── CardAttachment.java
│   │       │   └── DueDateStatus.java
│   │       │
│   │       ├── repository/
│   │       │   ├── UserRepository.java
│   │       │   ├── WorkspaceRepository.java
│   │       │   ├── BoardRepository.java
│   │       │   ├── CardRepository.java
│   │       │   └── CardAttachmentRepository.java
│   │       │
│   │       ├── service/
│   │       │   ├── AuthService.java
│   │       │   ├── WorkspaceService.java
│   │       │   ├── BoardService.java
│   │       │   ├── CardService.java
│   │       │   ├── DueDateService.java
│   │       │   ├── MarkdownService.java
│   │       │   └── AttachmentService.java
│   │       │
│   │       ├── session/
│   │       │   └── SessionManager.java
│   │       │
│   │       ├── controller/
│   │       │   ├── LoginController.java
│   │       │   ├── DashboardController.java
│   │       │   ├── WorkspaceController.java
│   │       │   ├── BoardController.java
│   │       │   └── CardController.java
│   │       │
│   │       ├── exception/
│   │       │   ├── AppException.java
│   │       │   ├── DatabaseException.java
│   │       │   ├── AuthenticationException.java
│   │       │   └── ValidationException.java
│   │       │
│   │       └── util/
│   │           ├── DateUtils.java
│   │           └── ValidationUtils.java
│   │
│   └── resources/
│       ├── fxml/
│       │   ├── Login.fxml
│       │   ├── Dashboard.fxml
│       │   ├── Board.fxml
│       │   ├── WorkspaceDialog.fxml
│       │   └── CardDialog.fxml
│       │
│       ├── css/
│       │   ├── app.css
│       │   ├── login.css
│       │   ├── dashboard.css
│       │   ├── board.css
│       │   └── card.css
│       │
│       └── db/
│           └── migration/
│               ├── V1__initial_schema.sql
│               └── ...
│
└── test/
    └── java/
        └── com/example/taskboard/
```

The exact package name can be changed if the user specifies one.

---

# 5. Domain Model

The initial domain hierarchy should be:

```text
User
 └── Workspace
      └── Board
           └── Card
                └── Attachments
```

A workspace can contain one or more boards.

A board contains cards.

A card belongs to exactly one board.

---

# 6. Database Schema

Use SQLite.

Enable foreign key enforcement on every connection:

```sql
PRAGMA foreign_keys = ON;
```

## 6.1 Users

```sql
CREATE TABLE users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL
);
```

Never store plaintext passwords.

---

## 6.2 Workspaces

```sql
CREATE TABLE workspaces (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT,
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL
);
```

---

## 6.3 Boards

```sql
CREATE TABLE boards (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    workspace_id INTEGER NOT NULL,
    name         TEXT NOT NULL,
    description  TEXT,
    created_at   TEXT NOT NULL,
    updated_at   TEXT NOT NULL,

    FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE
);
```

---

## 6.4 Cards

```sql
CREATE TABLE cards (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    board_id     INTEGER NOT NULL,
    title        TEXT NOT NULL,
    description  TEXT,
    status       TEXT NOT NULL,
    position     REAL NOT NULL,
    due_date     TEXT,
    created_at   TEXT NOT NULL,
    updated_at   TEXT NOT NULL,
    completed_at TEXT,

    FOREIGN KEY (board_id)
        REFERENCES boards(id)
        ON DELETE CASCADE
);
```

Recommended status enum:

```java
public enum CardStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CLOSED
}
```

---

## 6.5 Card Attachments

```sql
CREATE TABLE card_attachments (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id     INTEGER NOT NULL,
    file_name   TEXT NOT NULL,
    file_path   TEXT NOT NULL,
    mime_type   TEXT,
    created_at  TEXT NOT NULL,

    FOREIGN KEY (card_id)
        REFERENCES cards(id)
        ON DELETE CASCADE
);
```

---

# 7. Date and Time Rules

Use Java's `java.time` API.

For a card deadline containing only a date:

```java
LocalDate
```

For timestamps:

```java
Instant
```

Do not use legacy:

```java
java.util.Date
java.util.Calendar
```

unless required by a third-party API.

Store database timestamps in a consistent ISO-8601 representation.

For example:

```text
2026-09-12
2026-09-08T10:15:30Z
```

---

# 8. Phase 0 — Project Foundation

## Goal

Create the initial JavaFX application and development infrastructure.

## Tasks

Create:

- Maven project
- Java 21+ configuration
- JavaFX dependencies
- JavaFX application entry point
- FXML loading
- CSS loading
- basic logging
- basic application configuration
- resource structure

Create:

```text
Main.java
Main.fxml
app.css
```

The application should display a simple window.

Example:

```text
TaskBoard

Application is running.
```

## Scene Builder Requirement

Open the FXML in Scene Builder successfully.

The FXML must not depend on generated code or unsupported constructs.

## Logging

Configure Logback.

Create:

```text
logs/
```

Log application startup.

Do not log passwords or sensitive credentials.

## Verification

Run:

```bash
mvn clean test
```

Then:

```bash
mvn javafx:run
```

Verify:

- Application starts
- Window opens
- FXML loads
- CSS loads
- Log file is created
- Scene Builder can open the FXML
- No stack traces occur

## STOP CHECKPOINT

STOP after Phase 0.

Ask the user to verify the application manually.

Do not continue until the user confirms.

---

# 9. Phase 1 — SQLite and Database Infrastructure

## Goal

Introduce persistent storage without changing the user-facing functionality significantly.

## Tasks

Implement:

```text
DatabaseManager
MigrationManager
```

Configure SQLite.

Database location:

```text
data/taskboard.db
```

Create migration:

```text
V1__initial_schema.sql
```

Create the following tables:

- users
- workspaces
- boards
- cards
- card_attachments

Add appropriate:

- primary keys
- foreign keys
- unique constraints
- indexes

Use transactions where multiple database changes must succeed or fail together.

## Repository Foundation

Create repository interfaces/classes.

Do not implement every CRUD operation yet unless required for database verification.

## Verification

Run the application.

Verify:

1. `data/` is automatically created.
2. `taskboard.db` is created.
3. Flyway migration runs.
4. Tables exist.
5. Restarting the application does not destroy data.
6. Migration is not repeatedly applied.
7. Database errors are logged correctly.

## Automated Tests

Test:

- database initialization
- migration
- connection creation
- foreign-key enforcement

## STOP CHECKPOINT

STOP and request user verification.

---

# 10. Phase 2 — Authentication and Login

## Goal

Implement a real login workflow.

## UI

Create:

```text
Login.fxml
LoginController.java
```

UI:

```text
Username
[________________]

Password
[________________]

[ Login ]
```

## Backend

Implement:

```text
UserRepository
AuthService
SessionManager
```

Use a secure password hashing algorithm such as BCrypt or Argon2.

Never compare plaintext passwords directly against database values.

## Initial User

Provide a controlled development mechanism to create an initial user.

Do not hard-code a production password.

A development-only bootstrap mechanism may be used if clearly separated from production behavior.

## Authentication Flow

```text
Application Start
      ↓
Login Screen
      ↓
AuthService
      ↓
UserRepository
      ↓
SQLite
      ↓
Successful Login
      ↓
Dashboard
```

## Validation

Handle:

- empty username
- empty password
- invalid username
- incorrect password
- database failure

Show user-friendly messages.

Do not expose SQL exceptions or stack traces directly to users.

## Verification

Test:

- successful login
- invalid password
- unknown username
- empty fields
- application restart
- logout
- login again

## Automated Tests

Test:

- password hashing
- password verification
- successful authentication
- failed authentication
- session creation
- logout

## STOP CHECKPOINT

STOP for user verification.

---

# 11. Phase 3 — Workspace CRUD

## Goal

Implement complete workspace management.

## UI

Dashboard should display workspaces.

Example:

```text
WORKSPACES

Development
Personal
Projects

[ + Create Workspace ]
```

Create workspace dialog:

```text
Name
Description

[ Cancel ] [ Save ]
```

## Operations

Implement:

- Create
- Read
- Update
- Delete

Create:

```text
WorkspaceRepository
WorkspaceService
WorkspaceController
```

## Validation

Workspace name:

- required
- trimmed
- reasonable maximum length
- cannot be blank

## Delete Behavior

Deleting a workspace should delete its boards and cards using database foreign-key cascade behavior or an explicit transaction.

The user should receive a confirmation dialog before destructive operations.

## Verification

Test:

1. Create workspace.
2. Display workspace.
3. Edit workspace.
4. Delete workspace.
5. Restart application.
6. Verify persistence.
7. Test deleting a workspace containing boards/cards after those features exist.

## Automated Tests

Test service validation and repository CRUD.

## STOP CHECKPOINT

STOP for user verification.

---

# 12. Phase 4 — Board and Kanban UI

## Goal

Create the Trello-like board interface.

## UI

Example:

```text
┌──────────────────────────────────────────────────────┐
│ Workspace: Development                              │
│ Board: Software Project                             │
├─────────────┬─────────────┬────────────┬────────────┤
│ PLANNED     │ IN PROGRESS │ COMPLETED  │ CLOSED     │
│             │             │            │            │
│ Card        │ Card        │ Card       │ Card       │
│ Card        │             │            │            │
└─────────────┴─────────────┴────────────┴────────────┘
```

Use JavaFX containers such as:

- HBox
- VBox
- ScrollPane
- BorderPane
- StackPane

Cards should be reusable JavaFX components.

## Important

Do not implement drag-and-drop yet.

At this stage, status changes can be performed through the card editor.

## CSS

Create dedicated styles:

```text
board.css
card.css
```

Do not put large amounts of styling in Java.

## Verification

Verify:

- Board loads
- Columns display
- Cards display in the correct column
- Empty columns display correctly
- Long board content scrolls
- Window resizing behaves reasonably
- CSS can be modified without Java code changes

## STOP CHECKPOINT

STOP for user verification.

---

# 13. Phase 5 — Card CRUD

## Goal

Implement complete card lifecycle.

## Card Fields

At minimum:

```text
Title
Description
Status
Due Date
Position
Created At
Updated At
Completed At
```

## Card Dialog

Create:

```text
CardDialog.fxml
CardController.java
```

Example:

```text
Title
[____________________________]

Status
[ IN PROGRESS ▼ ]

Due Date
[ 12/09/2026 ]

Description
[                            ]
[                            ]

[ Cancel ] [ Save ]
```

## Operations

Implement:

- Create
- Read
- Update
- Delete

## Status

Use:

```java
public enum CardStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CLOSED
}
```

Persist the enum safely.

Avoid relying on enum ordinal numbers.

Use stable string values.

## Completed Timestamp

When a card transitions into:

```text
COMPLETED
```

set:

```text
completed_at
```

When it leaves COMPLETED, decide explicitly whether the timestamp is cleared.

Document the chosen behavior.

Recommended:

- entering COMPLETED → set `completed_at`
- leaving COMPLETED → clear `completed_at`

CLOSED should be treated as a separate final status.

## Verification

Test:

- Create card
- Edit card
- Change status
- Delete card
- Open card
- Restart application
- Verify all data persists

## STOP CHECKPOINT

STOP for user verification.

---

# 14. Phase 6 — Proper Due-Date Implementation

## Goal

Implement robust due-date behavior.

Create:

```text
DueDateService
DueDateStatus
```

Recommended enum:

```java
public enum DueDateStatus {
    NONE,
    UPCOMING,
    DUE_TODAY,
    OVERDUE,
    COMPLETED
}
```

## Rules

No due date:

```text
NONE
```

Future date:

```text
UPCOMING
```

Today's date:

```text
DUE_TODAY
```

Past date:

```text
OVERDUE
```

Completed card:

```text
COMPLETED
```

Completed cards should not be visually treated as overdue.

## UI

Cards should display something similar to:

```text
Due Sep 12
```

or:

```text
Due today
```

or:

```text
Overdue
```

Do not hard-code colors in Java.

Use CSS pseudo-classes or style classes.

For example:

```text
due-normal
due-today
due-overdue
due-completed
```

## Verification

Test cards with:

- no due date
- yesterday
- today
- tomorrow
- future date
- completed status
- closed status

## Automated Tests

Create unit tests covering date boundaries.

Especially verify:

- today at midnight
- today
- tomorrow
- yesterday
- status transitions

## STOP CHECKPOINT

STOP for user verification.

---

# 15. Phase 7 — Markdown Support

## Goal

Allow rich card descriptions using Markdown.

Use Flexmark-Java or another mature Markdown implementation.

Do not write a custom Markdown parser.

## Storage

Store the original Markdown in:

```text
cards.description
```

Do not store only rendered HTML.

## Supported Features

At minimum:

- headings
- bold
- italic
- unordered lists
- ordered lists
- links
- inline code
- code blocks
- checkboxes
- images

Example:

```markdown
# Implement Login

**Important**

- [x] Create login screen
- [x] Create database
- [ ] Add authentication
- [ ] Add tests

## Notes

This is a test card.
```

## UI

Prefer a split editor:

```text
┌────────────────────┬────────────────────┐
│ Markdown Editor    │ Preview            │
│                    │                    │
│ # My Task          │ My Task             │
│                    │                    │
│ - [x] Done         │ ☑ Done              │
│ - [ ] Todo         │ ☐ Todo              │
│                    │                    │
└────────────────────┴────────────────────┘
```

The preview may use JavaFX WebView.

## Security

Markdown rendering must not blindly allow unsafe HTML or scripts.

Sanitize or configure the Markdown parser appropriately.

Do not allow arbitrary local/remote content to execute JavaScript.

## Verification

Create a card containing:

```markdown
# Test Card

**Bold**

*Italic*

- Item 1
- Item 2

- [x] Complete
- [ ] Pending

`code`

```java
System.out.println("Hello");
```
```

Verify the preview.

Save.

Restart.

Open the card.

Verify the original Markdown is still present and renders correctly.

## STOP CHECKPOINT

STOP for user verification.

---

# 16. Phase 8 — Image Attachments

## Goal

Support images in Markdown cards.

## Storage

Use an application-controlled directory:

```text
data/
├── taskboard.db
└── attachments/
    ├── card-1/
    │   ├── image1.png
    │   └── screenshot.jpg
    └── card-2/
        └── image.png
```

Do not store large image files directly inside SQLite unless there is a compelling reason.

## Attachment Service

Implement:

```text
AttachmentService
CardAttachmentRepository
```

## UI

Add:

```text
[ Attach Image ]
```

When selected:

1. Validate the file.
2. Create card attachment directory.
3. Copy the image.
4. Create database record.
5. Insert/reference the image in Markdown.
6. Refresh preview.

## Markdown

Use an application-specific URI such as:

```markdown
![Screenshot](attachment://42)
```

Resolve the attachment URI safely.

Never allow arbitrary filesystem traversal such as:

```text
../../../../some-file
```

## Verification

Test:

- PNG
- JPG/JPEG
- invalid file type
- large file handling
- missing attachment
- deleted attachment
- application restart

Verify images continue to render after restart.

## STOP CHECKPOINT

STOP for user verification.

---

# 17. Phase 9 — Drag and Drop

## Goal

Implement Trello-like card movement.

Support:

```text
Card → another position in same column
Card → another status column
```

Example:

```text
PLANNED
   ↓ drag
IN_PROGRESS
```

## Persistence

Update:

```text
status
position
updated_at
```

in a transaction.

## Position Strategy

Do not use card IDs for ordering.

Use the existing:

```text
position REAL
```

field.

Initially, simple sequential positions are acceptable.

Later, optimize insertion/reordering if necessary.

## Transaction

A move should behave like:

```text
BEGIN
    update card status
    update card position
COMMIT
```

If anything fails:

```text
ROLLBACK
```

## Verification

Test:

- move within same column
- move to another column
- move to first position
- move to last position
- reorder several cards
- restart application
- verify ordering persists

## STOP CHECKPOINT

STOP for user verification.

---

# 18. Phase 10 — Production Hardening

## Goal

Improve reliability, maintainability, and diagnostics.

## Logging

Use:

```text
SLF4J
Logback
```

Log:

- application startup
- shutdown
- authentication success/failure without passwords
- database initialization
- CRUD failures
- unexpected exceptions
- attachment failures

Do not log:

- passwords
- password hashes
- sensitive user data
- unnecessary full Markdown content

## Log Levels

Use:

```text
TRACE
DEBUG
INFO
WARN
ERROR
```

Use INFO for important application events.

Use DEBUG for development diagnostics.

Use ERROR for failures.

---

# 19. Global Error Handling

The application should not crash because of a normal user-facing operation failure.

Example:

Instead of showing:

```text
java.sql.SQLException:
...
```

show:

```text
Unable to save the card.

Please try again.
```

Log the real exception internally.

Create application exceptions:

```text
AppException
DatabaseException
AuthenticationException
ValidationException
```

Use exception chaining.

Example concept:

```java
throw new DatabaseException("Unable to save card", e);
```

---

# 20. Validation

Centralize validation where practical.

Examples:

Workspace:

```text
Name required
Maximum length
```

Card:

```text
Title required
Maximum title length
Description maximum size if necessary
```

Attachment:

```text
Allowed image types
Maximum file size
```

Never trust UI validation alone.

Services should validate important business rules too.

---

# 21. Testing Strategy

Use JUnit 5.

## Unit Tests

Test:

- AuthService
- WorkspaceService
- CardService
- DueDateService
- MarkdownService
- validation utilities

## Repository Tests

Use a temporary test database.

Test:

- insert
- update
- delete
- find
- relationships
- cascade behavior
- transactions

## UI Tests

Use TestFX where practical.

Prioritize:

- Login
- Dashboard loading
- Workspace dialog
- Card dialog

Do not create fragile UI tests for every CSS detail.

---

# 22. Threading Rules

JavaFX UI operations must happen on the JavaFX Application Thread.

Database operations should not block the JavaFX Application Thread.

For potentially slow operations use:

```text
Task
Service
ExecutorService
```

Examples:

- database loading
- attachment copying
- large Markdown processing
- future search operations

Do not blindly introduce asynchronous code everywhere.

Keep the implementation simple until there is a real reason to move work off the UI thread.

---

# 23. CSS and Theme Architecture

All major styling should be CSS-based.

Example:

```text
css/
├── app.css
├── login.css
├── dashboard.css
├── board.css
└── card.css
```

Use semantic style classes.

Examples:

```text
.board-column
.card
.card-title
.card-description
.card-due-date
.card-due-today
.card-overdue
.card-completed
.workspace-item
```

The Java code should apply/remove semantic classes rather than setting colors and dimensions directly.

This allows the user to modify the UI using CSS without changing business logic.

---

# 24. Scene Builder Requirements

Every major UI screen should remain editable using Scene Builder.

Do not create an FXML structure that requires proprietary or generated tooling unavailable to Scene Builder.

Keep:

- layout
- controls
- labels
- buttons
- containers

in FXML where reasonable.

Use custom JavaFX components only where they provide significant value.

---

# 25. UI Navigation

Create a central navigation mechanism rather than controllers directly constructing every screen.

Recommended concept:

```text
NavigationService
```

Responsibilities:

- switch scenes/views
- preserve application session
- avoid duplicated navigation code
- handle logout

Expected flow:

```text
Application
    ↓
Login
    ↓
Dashboard
    ↓
Workspace
    ↓
Board
    ↓
Card Dialog
```

Logout:

```text
Dashboard
    ↓
SessionManager.logout()
    ↓
Login
```

---

# 26. Data Persistence Requirements

The application must persist:

- users
- workspaces
- boards
- cards
- statuses
- positions
- due dates
- Markdown
- attachment metadata
- attachment files

Restarting the application must not lose data.

The application must not depend on temporary application memory for authoritative data.

---

# 27. Backup-Friendly Design

Because SQLite is used, the user should be able to back up:

```text
data/taskboard.db
data/attachments/
```

Future versions may introduce an export/import feature.

Do not implement export/import in the initial phases unless requested.

---

# 28. Security Baseline

Implement reasonable desktop-application security.

At minimum:

- hashed passwords
- no plaintext passwords
- no password logging
- prepared SQL statements
- safe attachment paths
- input validation
- safe Markdown rendering

Use `PreparedStatement`.

Never concatenate user input into SQL.

Bad:

```java
"SELECT * FROM users WHERE username = '" + username + "'"
```

Good:

```java
"SELECT * FROM users WHERE username = ?"
```

---

# 29. Performance Guidelines

The application is initially intended for:

- personal use
- small teams
- relatively small datasets

Do not prematurely optimize.

However:

- use database indexes where useful
- avoid loading unnecessary data
- do not block JavaFX UI unnecessarily
- use pagination later if the dataset becomes large
- avoid rendering huge Markdown documents repeatedly
- avoid loading full-size image data unnecessarily

---

# 30. Definition of Done for Every Phase

A phase is NOT complete merely because the code compiles.

A phase is complete only when:

- [ ] Code compiles
- [ ] Application starts
- [ ] Feature works manually
- [ ] Existing features still work
- [ ] Relevant tests pass
- [ ] No obvious errors appear in logs
- [ ] FXML remains Scene Builder compatible where applicable
- [ ] CSS remains externally editable
- [ ] Database changes are migrated properly
- [ ] No plaintext secrets are introduced
- [ ] User-facing errors are understandable

---

# 31. Required Agent Workflow

For every phase, follow this exact workflow.

## Step 1 — Inspect

Before coding:

- inspect existing project structure
- inspect current classes
- inspect current FXML
- inspect current CSS
- inspect database migrations
- understand what previous phases implemented

Do not overwrite existing functionality blindly.

## Step 2 — Plan

Briefly state:

- files to create
- files to modify
- database changes
- tests to add
- expected behavior

## Step 3 — Implement

Implement only the current phase.

Do not implement future phases.

## Step 4 — Compile

Run:

```bash
mvn clean test
```

Fix all compilation/test failures.

## Step 5 — Run

Start:

```bash
mvn javafx:run
```

or the project's configured equivalent.

## Step 6 — Verify

Perform the phase-specific verification checklist.

## Step 7 — Report

Report:

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

## Step 8 — STOP

After the report, STOP.

Do not automatically continue to the next phase.

Wait for the user to say something such as:

```text
Phase verified. Continue.
```

---

# 32. Agent Must Not Do These Things

Do NOT:

- implement all phases at once
- skip verification
- silently change architecture
- replace SQLite with another database without approval
- add unnecessary frameworks
- store plaintext passwords
- put SQL in controllers
- put business logic in FXML
- hard-code colors throughout Java
- store attachments as arbitrary Base64 strings in Markdown
- use absolute filesystem paths in persisted Markdown
- swallow exceptions
- use `System.out.println` for application logging
- ignore database transactions
- break Scene Builder compatibility
- remove tests to make builds pass
- disable tests because they fail
- continue to the next phase without user confirmation

---

# 33. Future Features — Do Not Implement Initially

Keep the architecture extensible, but do not implement these unless explicitly requested:

- multiple users per workspace
- role-based permissions
- workspace invitations
- real-time collaboration
- cloud synchronization
- REST API
- notifications
- email
- mobile application
- calendar integration
- recurring tasks
- labels
- comments
- activity history
- checklists as separate database entities
- file attachments other than images
- full-text search
- Kanban analytics

These may be added later.

---

# 34. Final Target Application

When all phases are complete, the application should provide:

```text
                    TASKBOARD
                         │
                    Login Screen
                         │
                         ▼
                    Dashboard
                         │
            ┌────────────┴────────────┐
            │                         │
       Workspaces                 User Session
            │
            ▼
         Workspace
            │
            ▼
          Board
            │
     ┌──────┼──────────┬──────────┐
     ▼      ▼          ▼          ▼
  Planned  In Progress Completed Closed
     │      │          │          │
     └──────┴──────────┴──────────┘
                    │
                    ▼
                  Card
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
       Markdown   Due Date  Images
          │
          ▼
       Renderer
          │
          ▼
       JavaFX UI
```

The completed application should feel like a lightweight desktop Trello alternative while remaining maintainable as a JavaFX project.

---

# 35. First Instruction to the Coding Agent

When starting this project, give the agent the following instruction:

> Read this PLAN.md completely before making changes.
>
> Start with **Phase 0 only**.
>
> Inspect the current repository first. If the project is empty, create the JavaFX Maven project using Java 21+.
>
> Implement only Phase 0.
>
> After implementation, compile and test the project and run the JavaFX application if possible.
>
> Report exactly what was implemented, what files were changed, the commands used for verification, and whether the application successfully started.
>
> Do NOT implement Phase 1 or any later phase.
>
> Stop after Phase 0 and wait for my verification.
>
> Do not ask me unnecessary questions if reasonable defaults can be used. If an essential requirement is genuinely missing, ask before making an irreversible architectural decision.

---

# 36. Phase Progress Tracker

Update this section as development progresses.

```text
[x] Phase 0  — Project Foundation
[x] Phase 1  — SQLite + Database Infrastructure
[x] Phase 2  — Authentication + Login
[ ] Phase 3  — Workspace CRUD
[ ] Phase 4  — Board + Kanban UI
[ ] Phase 5  — Card CRUD
[ ] Phase 6  — Due Dates
[ ] Phase 7  — Markdown
[ ] Phase 8  — Image Attachments
[ ] Phase 9  — Drag & Drop
[ ] Phase 10 — Production Hardening
```

Only mark a phase complete after the user has manually verified it.

---

# 37. Success Criteria

The project is successful when:

1. The application launches reliably.
2. A user can log in.
3. Workspaces can be created, edited, viewed, and deleted.
4. Boards can display Kanban columns.
5. Cards can be created, edited, viewed, and deleted.
6. Cards can move between Planned, In Progress, Completed, and Closed.
7. Card ordering persists.
8. Due dates are correctly calculated and displayed.
9. Markdown descriptions are preserved.
10. Markdown checkboxes render correctly.
11. Images can be attached and rendered inside cards.
12. All data survives application restart.
13. Database migrations work reliably.
14. Important business logic has automated tests.
15. Errors are logged using structured logging.
16. User-facing errors are handled gracefully.
17. UI layouts remain editable using Scene Builder.
18. UI appearance can be substantially changed through CSS without rewriting business logic.
19. The application does not contain obvious security problems such as plaintext passwords or SQL injection vulnerabilities.
20. Each development phase was independently verified before the next phase was started.

---

## END OF PLAN
