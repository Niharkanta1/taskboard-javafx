# TaskBoard — Desktop Trello-like Task Manager (JavaFX)

A desktop kanban/task management application built with **JavaFX** and **SQLite**, developed incrementally in verified phases.

**Current status:** all planned application phases are complete, including boards, cards, due dates, Markdown, image attachments, drag-and-drop, and production hardening.

## Features

- **User login** with BCrypt password hashing (no plaintext passwords, no password logging)
- **Workspace management**: create, view, edit, delete — with validation and a confirmation dialog before deletion
- **Persistent storage**: SQLite database on disk with automatic creation and Flyway migrations (data survives restarts)
- **Structured logging**: SLF4J + Logback, written to `logs/taskboard.log`
- **Clean UI architecture**: FXML for layout (Scene Builder compatible), Java controllers for behavior, CSS for all visual styling
- **Layered architecture**: UI → controllers → services → repositories → database (no SQL in controllers)
- **Automated tests**: JUnit 5 unit and integration tests (57/57 passing)
- **Graceful error handling**: friendly user-facing messages; real exceptions logged internally

The dashboard's **Storage Settings** button selects the directory containing `taskboard.db` and `attachments/`. The setting is stored in `%USERPROFILE%\\.taskboard\\settings.properties` and takes effect after restarting the app.

I have created a ready to ship version:
# Boardly — Desktop Trello-like Task Manager (JavaFX)

## Screenshots:
<img width="756" height="591" alt="{4EBA475E-3AAB-4BB3-8F97-119E0C4938FD}" src="https://github.com/user-attachments/assets/8fe97060-f0f6-488a-87e1-8b25c1decf3f" />
<img width="754" height="589" alt="{9AF2F317-5F14-4D1B-8815-4C66ECCC2561}" src="https://github.com/user-attachments/assets/73926ed0-a883-488c-bb2a-3f0b8063131a" />
<img width="757" height="587" alt="{656B0251-6561-4619-975B-7CBDEBD8FBC6}" src="https://github.com/user-attachments/assets/73ee5c4e-a9fa-438b-94fa-1bb61f930087" />
<img width="761" height="589" alt="{8ABAA6D9-FBE9-49F8-8A23-62A22C8B4AB0}" src="https://github.com/user-attachments/assets/696b1add-b7f6-4b40-8617-b05feb5dce17" />
<img width="1097" height="680" alt="{948AC61B-8D94-4131-920C-D757967E2E29}" src="https://github.com/user-attachments/assets/7b2ddbd2-7753-4462-8dcc-63ab4235ee94" />
<img width="714" height="584" alt="{907AD7E3-5508-469E-92A7-0103A33DE602}" src="https://github.com/user-attachments/assets/2813e92c-b5d3-4979-9fd7-44775a245e51" />
<img width="713" height="578" alt="{BEBDC0B5-F764-4402-A190-D63D58FDC1F8}" src="https://github.com/user-attachments/assets/3f5382a4-f938-40dd-bf9b-043185e67c14" />

## Technology

| Area               | Technology                                        |
| ------------------ | ------------------------------------------------- |
| Language / runtime | Java 21                                           |
| Build              | Maven (`javafx-maven-plugin` for zero-setup runs) |
| UI                 | JavaFX 21.0.2, FXML, CSS                          |
| Database           | SQLite (sqlite-jdbc 3.46.0.0), JDBC               |
| Migrations         | Flyway 9.22.3                                     |
| Logging            | SLF4J 2.0.13 + Logback 1.5.6                      |
| Password hashing   | BCrypt (jbcrypt 0.4)                              |
| Testing            | JUnit 5 (5.10.2)                                  |

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

### 3. Build a portable Windows release

Prerequisite: a full JDK 21 installation with `jpackage` on `PATH`.

```bash
mvn clean verify -Prelease
```

The release profile creates `target/release/TaskBoard-1.0.0-windows.zip`. Extract it anywhere and run `TaskBoard/TaskBoard.exe`; no Java installation is required on the target machine.

### 4. Log in

A development user is created automatically on first start:

- **Username:** `dev`
- **Password:** `Dev-1234`

(For a different dev password, set the environment variable `TASKBOARD_DEV_PASSWORD` before running.)

If you do not know the development credentials, click **Create User** on the starting login screen. Choose a username and password, then return to login with the new account. The same starting screen also contains **Storage Settings**, where you can select the database and attachments directory before logging in.

The first screen lists all locally registered usernames. Select one and click **Continue** to open the login screen with that username prefilled. Enter the password and click **Login** to reach the dashboard and workspace list.

### 5. Work with workspaces

1. Click **Create Workspace**, enter a name (required, max 100 chars) and an optional description (max 500 chars), then click **Save**.
2. Select a workspace in the list to **Edit** it.
3. Select a workspace and click **Delete** — a confirmation dialog appears before removal (deletion also removes its boards and cards).

Workspaces persist across restarts.

### 6. Where the data lives

| Item          | Location                                               |
| ------------- | ------------------------------------------------------ |
| Database file | Configured data directory, default `data/taskboard.db` |
| Attachments   | Configured data directory, `attachments/`              |
| Log file      | `logs/taskboard.log`                                   |
| Migrations    | `src/main/resources/db/migration/`                     |

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
