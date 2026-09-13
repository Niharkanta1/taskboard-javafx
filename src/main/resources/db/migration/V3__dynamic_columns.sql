-- V3: Dynamic kanban columns.
--
-- Columns are persisted per board, and cards reference their column
-- instead of a fixed status. Existing cards are mapped to their
-- board's default columns, and the legacy status column is removed.

CREATE TABLE board_columns (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    board_id   INTEGER NOT NULL,
    name       TEXT NOT NULL,
    position   REAL NOT NULL,
    is_final   INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,

    FOREIGN KEY (board_id)
        REFERENCES boards(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_board_columns_board_id ON board_columns (board_id);
CREATE INDEX idx_board_columns_board_position ON board_columns (board_id, position);

ALTER TABLE cards ADD COLUMN board_column_id INTEGER NOT NULL DEFAULT 0;

-- Seed the four default columns for every existing board.
INSERT INTO board_columns (board_id, name, position, is_final, created_at, updated_at)
SELECT b.id, 'Planned', 1.0, 0, b.created_at, b.created_at
FROM boards b;

INSERT INTO board_columns (board_id, name, position, is_final, created_at, updated_at)
SELECT b.id, 'In Progress', 2.0, 0, b.created_at, b.created_at
FROM boards b;

INSERT INTO board_columns (board_id, name, position, is_final, created_at, updated_at)
SELECT b.id, 'Completed', 3.0, 1, b.created_at, b.created_at
FROM boards b;

INSERT INTO board_columns (board_id, name, position, is_final, created_at, updated_at)
SELECT b.id, 'Closed', 4.0, 1, b.created_at, b.created_at
FROM boards b;

-- Map each existing card to its board's column of the same name.
UPDATE cards
SET board_column_id = (
    SELECT c.id
    FROM board_columns c
    WHERE c.board_id = cards.board_id
      AND ((cards.status = 'PLANNED' AND c.name = 'Planned')
        OR (cards.status = 'IN_PROGRESS' AND c.name = 'In Progress')
        OR (cards.status = 'COMPLETED' AND c.name = 'Completed')
        OR (cards.status = 'CLOSED' AND c.name = 'Closed'))
);

DROP INDEX IF EXISTS idx_cards_board_status_position;

ALTER TABLE cards DROP COLUMN status;

CREATE INDEX idx_cards_board_column_position ON cards (board_id, board_column_id, position);
