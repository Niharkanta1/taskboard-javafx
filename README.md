# TaskBoard — Desktop Trello-like Task Manager (JavaFX)

A desktop kanban/task management application built with **JavaFX** and **SQLite**, developed incrementally in verified phases.

**Current status:** login, session management, and full workspace CRUD are implemented (Phases 0–3). Boards, cards, due dates, Markdown, and image attachments are planned in later phases.

## Features

- **User login** with BCrypt password hashing (no plaintext passwords, no password logging)
- **Workspace management**: create, view, edit, delete — with validation and a confirmation dialog before deletion
- **Persistent storage**: SQLite database on disk with automatic creation and Flyway migrations (data survives restarts)
- **Structured logging**: SLF4J + Logback, written to `logs/taskboard.log`
- **Clean UI architecture**: FXML for layout (Scene Builder compatible), Java controllers for behavior, CSS for all visual styling
- **Layered architecture**: UI → controllers → services → repositories → database (no SQL in controllers)
- **Automated tests**: JUnit 5 unit and integration tests (57/57 passing)
- **Graceful error handling**: friendly user-facing messages; real exceptions logged internally

Planned next: Kanban boards with status columns (Planned / In Progress / Completed / Closed), card CRUD, due dates, Markdown descriptions, and image attachments.

## Technology

| Area | Technology |
|---|---|
| Language / runtime | Java 21 |
| Build | Maven (`javafx-maven-plugin` for zero-setup runs) |
| UI | JavaFX 21.0.2, FXML, CSS |
| Database | SQLite (sqlite-jdbc 3.46.0.0), JDBC |
| Migrations | Flyway 9.22.3 |
| Logging | SLF4J 2.0.13 + Logback 1.5.6 |
| Password hashing | BCrypt (jbcrypt 0.4) |
| Testing | JUnit 5 (5.10.2) |

## Getting Started

**Prerequisites:** Java 21+ and Maven. No other setup is needed — the Maven plugin resolves JavaFX automatically.

### 1. Build and run tests

```bash
mvn clean test
```

### 2. Run the application

```bash
mvn javafx:run
```

The app opens a login window.

### 3. Log in

A development user is created automatically on first start:

- **Username:** `dev`
- **Password:** `Dev-1234`

(For a different dev password, set the environment variable `TASKBOARD_DEV_PASSWORD` before running.)

Enter the username and password, then click **Log In**. You land on the dashboard, which shows the current user and the list of workspaces.

### 4. Work with workspaces

1. Click **Create Workspace**, enter a name (required, max 100 chars) and an optional description (max 500 chars), then click **Save**.
2. Select a workspace in the list to **Edit** it.
3. Select a workspace and click **Delete** — a confirmation dialog appears before removal (deletion also removes its boards and cards).

Workspaces persist across restarts.

### 5. Where the data lives

| Item | Location |
|---|---|
| Database file | `data/taskboard.db` (created automatically on first run) |
| Log file | `logs/taskboard.log` |
| Migrations | `src/main/resources/db/migration/` |

## Project Layout

```
src/main/java/com/example/taskboard/
  Main.java                  # entry point
  config/                   # AppConfig, AppPaths
  controller/               # Login, Dashboard, WorkspaceDialog controllers
  service/                  # Auth, Workspace, Navigation, DevUserBootstrap
  repository/               # interfaces + JDBC implementations
  model/                    # User, Workspace, Board, Card, ...
  database/                 # DatabaseManager, MigrationManager
src/main/resources/
  fxml/                     # Login, Dashboard, WorkspaceDialog
  css/                     # app.css, login.css, dashboard.css
  db/migration/             # Flyway SQL migrations
src/test/java/              # unit + integration tests
```
