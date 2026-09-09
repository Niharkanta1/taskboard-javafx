# Agent State

Current Phase: 7
Status: COMPLETE (awaiting user verification)
Last Verified Phase: 6 (Due Dates)
Phase Name: Markdown
Build: pass
Tests: 170/170 pass
Run: pass
Blockers: none
Next Action: user_verification_phase_7
Current Objective:
- Phase 7 (Markdown) is implemented and agent-verified: raw Markdown stored in card description; bold/italic/headings h1-h6/nested lists/tables/task lists/links/code/nested blockquotes rendered in card view and card dialog preview; clickable task checkboxes in board view persist toggles
- Card dialog description area now uses Edit/Preview mode buttons (raw Markdown editor vs rendered preview, default = Edit) instead of a split view
- Stop for user verification before Phase 8 (Image Attachments)

Completed Phases:
- 0: Project Foundation (verified)
- 1: SQLite + Database Infrastructure (verified)
- 2: Authentication + Login (verified)
- 3: Workspace CRUD (verified)
- 4: Board + Kanban UI (verified by user; ScrollPane bug fixed)
- 5: Card CRUD (verified by user)
- 6: Due Dates (verified by user)

Pending Phases:
7, 8, 9, 10

Files Changed This Phase:
- pom.xml (added com.vladsch.flexmark:flexmark-all:0.42.14; added flexmark-ext-gfm-tables:0.42.14 for GFM table parsing)
- src/main/java/com/example/taskboard/service/MarkdownService.java (new: Flexmark-backed block/inline parsing + toggleTask; ListItemBlock/TaskItemBlock carry nesting indent; BlockQuoteBlock carries nested content blocks; TableBlock parsed via GFM TablesExtension, separator row skipped)
- src/main/java/com/example/taskboard/view/MarkdownRenderer.java (new: renders blocks as plain JavaFX nodes; no HTML/WebView; headings h1-h6 keep their level; nested list/task rows indented 16px per level; tables render as GridPane with header styling; nested blockquotes render as nested quote boxes)
- src/main/java/com/example/taskboard/view/CardView.java (renders Markdown description; task-toggle callback)
- src/main/java/com/example/taskboard/controller/BoardController.java (toggleCardTask: toggleTask + updateCard + refresh)
- src/main/java/com/example/taskboard/controller/CardDialogController.java (Edit/Preview mode buttons switch between raw Markdown editor and rendered preview; default mode = Edit; preview non-interactive)
- src/main/resources/fxml/CardDialog.fxml (description area: Edit/Preview button bar + ScrollPane with StackPane holding TextArea editor and preview VBox; replaced old SplitPane; fixed fitToWidth/fitToHeight)
- src/main/resources/css/card.css (Markdown style classes: card-markdown, md-h1..h6, md-bold, md-italic, md-code, md-link, md-bullet, card-markdown-task-item, card-markdown-codeblock, md-blockquote, md-table, md-table-header, mode-button, mode-button-active)
- src/main/java/com/example/taskboard/Main.java + NavigationService.java (MarkdownService wiring)
- src/test/java/com/example/taskboard/service/MarkdownServiceTest.java (updated for new record signatures; added nested list/task indentation, nested blockquote, table, fenced-code-with-info tests)
- src/test/java/com/example/taskboard/CardDialogFxmlTest.java (updated: asserts Edit/Preview buttons, ScrollPane+StackPane description area, default edit-mode visibility; regression for FXML property-binding bugs)
- src/test/java/com/example/taskboard/MarkdownRenderTest.java (new: renders MARKDOWN_TEST.md end-to-end and asserts headings h1/h2/h6, emphasis, nested list indentation, table GridPane, fenced + mermaid code, links, nested blockquotes, image alt text, inline code)

Verification:
- mvn clean test: PASS (agent-run 2026-09-09, 170/170) | user check: PENDING
- mvn javafx:run: PASS (agent-run, app starts, "Application started successfully") | user check: PENDING
- End-to-end verifier (real temp DB + real JavaFX toolkit): PHASE7 VERIFY: ALL CHECKS PASSED — 8-block parse order (heading, paragraph, 2 tasks, code, list, blockquote, link paragraph), rendered node types (heading Label, code TextArea, task CheckBoxes with correct states, link Text node with md-link), toggleTask + updateCard persistence (re-render shows checked checkbox), CardView renders without exception; throwaway verifier removed
- Markdown safety: no HTML/JS rendered; only http/https links are link-styled and open via HostServices; everything else is plain text
- Dialog preview is non-interactive; task toggling happens from the board view after save
- FXML bug fixed after user report: CardDialog.fxml used read-only Pane properties fitWidth/fitHeight on ScrollPane (must be fitToWidth/fitToHeight) and an invalid dividerPosition attribute on SplitPane (dividers auto-sync with items; default position is 0.5); both removed/fixed; CardDialogFxmlTest now loads the dialog through FXMLLoader as a permanent regression
- CardView/card-dialog fixes per user request: nested list items no longer dropped (indent tracked in parse + 16px left padding per level in render), tables parse (GFM TablesExtension; separator row skipped) and render as GridPane, headings h4-h6 keep their own styles, nested blockquotes keep nested structure, card dialog uses Edit/Preview buttons instead of a split view (default = Edit); MARKDOWN_TEST.md renders fully (covered by MarkdownRenderTest)
- user manual verification of Phase 7 (incl. the CardView/card-dialog fixes): PENDING

Invariants:
- one phase at a time; stop after each for user verification
- all tests must stay green
- db.getConnection() returns shared connection; callers must NOT close it
- Flyway 9.x API: dataSource(url,user,password), MigrateResult.migrationsExecuted, api.FlywayException
- structured logging via SLF4J/logback only
- BCrypt via org.mindrot:jbcrypt (at.favre:bcrypt not on Maven Central)
- dev bootstrap: user "dev", env-overridable password (TASKBOARD_DEV_PASSWORD, default Dev-1234), dev-only
- JavaFX 21: Stage.initOwner(Window) (not setOwner); Class.getResource handles leading-slash names, raw ClassLoader.getResource does not

Paths:
project: D:/Coding/AI/AI-Test/javafx-trellolike
db: data/taskboard.db
log: logs/taskboard.log
plan: plan.md
context: CONTEXT.md

History:
2026-09-08: phase 2 verified by user; phase 3 complete; tests 57/57; app verified (starts, login, Flyway idempotent); awaiting user verification of workspace CRUD
2026-09-08: phase 4 complete (board create/load, workspace board list, read-only kanban view); tests 78/78; app verified (starts cleanly); awaiting user verification of Board + Kanban UI
2026-09-09: user reported board would not open; root cause = Board.fxml imported javafx.scene.layout.ScrollPane (wrong package; ScrollPane is in javafx.scene.control); fixed import, added FXML-import regression test; tests 79/79; board FXML verified to load; awaiting user re-verification of Phase 4
2026-09-09: Phase 5 (Card CRUD) complete: CardService (create/update/delete, validation, position = max+1, completed_at set on entering COMPLETED / cleared on leaving), CardDialog.fxml + CardDialogController (modal create/edit, delete with confirmation), BoardController New Card buttons per column + card click opens editor + refresh after changes, CardView clickable, double-click navigation for workspace/board rows, 17 CardServiceTest + 8 CardIntegrationTest cases; tests 105/105; app verified to start; both FXML views verified to load; awaiting user verification
2026-09-09: user reported board LoadException (stale build with old ScrollPane import) + cards not appearing after save; root cause = BoardController.refreshBoard() discarded the freshly loaded board so board.getCards() stayed stale/null; fixed by reassigning board = boardService.loadBoard(...) and making groupCardsByStatus null-safe; end-to-end flow check (real DB + real FXML) now passes: save -> card appears, edit to COMPLETED -> moves column + completed_at set; tests 105/105; app verified to start
2026-09-09: user verified Phase 5 (Card CRUD) and re-verified the Phase 4 board-opening fix
2026-09-09: Phase 6 (Due Dates) complete: DueDateStatus enum + DueDateService (status rules, display text, CSS class mapping; COMPLETED/CLOSED treated as final, never overdue), CardView due-date label, DueDateService wiring through Main -> NavigationService -> BoardController -> CardView, card.css due-date classes, 17 DueDateServiceTest cases + null due-date integration test; tests 123/123; end-to-end verifier (real temp DB + real Board.fxml) passed; app verified to start
2026-09-09: user verified Phase 6 (Due Dates); starting Phase 7 (Markdown)
2026-09-09: Phase 7 (Markdown) complete: MarkdownService (Flexmark 0.42.14, TaskListExtension) + MarkdownRenderer (plain JavaFX nodes only; TextFlow inline segments; http/https links only), CardView Markdown rendering + task-toggle callback, BoardController.toggleCardTask (toggleTask -> updateCard -> refreshBoard), CardDialog live non-interactive preview, card.css Markdown classes; 37 MarkdownServiceTest cases; tests 162/162; end-to-end verifier (temp DB + real JavaFX toolkit) passed; app verified to start; awaiting user verification
2026-09-09: user reported CardDialog.fxml LoadException (Property "fitWidth" does not exist or is read-only); root cause = FXML used read-only Pane properties fitWidth/fitHeight on ScrollPane (correct: fitToWidth/fitToHeight) plus an invalid dividerPosition attribute on SplitPane; fixed both, added CardDialogFxmlTest (FXMLLoader load with real controller) as permanent regression; tests 163/163; app verified to start
2026-09-09: user-requested CardView/card-dialog fixes complete: nested lists (indent in parse + 16px/level padding in render), tables (flexmark-ext-gfm-tables dependency; GFM TablesExtension; TableBlock -> GridPane; separator row skipped), headings h1-h6 keep their level (md-h1..md-h6), nested blockquotes (BlockQuoteBlock carries nested blocks; nested md-blockquote VBox), card dialog split view replaced by Edit/Preview mode buttons (default = Edit; StackPane holds editor + preview); MARKDOWN_TEST.md renders fully (new MarkdownRenderTest); tests 170/170; app verified to start; awaiting user verification before Phase 8
