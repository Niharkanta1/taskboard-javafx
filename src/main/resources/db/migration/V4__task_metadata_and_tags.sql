-- V4: Task metadata (priority, severity) and custom tags

ALTER TABLE cards ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE cards ADD COLUMN severity TEXT NOT NULL DEFAULT 'MINOR';

CREATE TABLE tags (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    color      TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX idx_tags_name ON tags (name);

CREATE TABLE card_tags (
    card_id INTEGER NOT NULL,
    tag_id  INTEGER NOT NULL,
    PRIMARY KEY (card_id, tag_id),
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_card_tags_card_id ON card_tags (card_id);
CREATE INDEX idx_card_tags_tag_id ON card_tags (tag_id);

-- Seed default initial tags
INSERT INTO tags (name, color, created_at, updated_at) VALUES ('Bug', '#ef4444', datetime('now'), datetime('now'));
INSERT INTO tags (name, color, created_at, updated_at) VALUES ('Feature', '#0ea5e9', datetime('now'), datetime('now'));
INSERT INTO tags (name, color, created_at, updated_at) VALUES ('Enhancement', '#8b5cf6', datetime('now'), datetime('now'));
INSERT INTO tags (name, color, created_at, updated_at) VALUES ('Documentation', '#10b981', datetime('now'), datetime('now'));
