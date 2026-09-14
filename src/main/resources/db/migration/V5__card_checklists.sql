-- V5: Card checklists (Trello-style checklists on cards)

CREATE TABLE checklists (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id    INTEGER NOT NULL,
    title      TEXT NOT NULL,
    position   INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_checklists_card_id ON checklists(card_id);

CREATE TABLE checklist_items (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    checklist_id INTEGER NOT NULL,
    text         TEXT NOT NULL,
    completed    INTEGER NOT NULL DEFAULT 0,
    position     INTEGER NOT NULL DEFAULT 0,
    created_at   TEXT NOT NULL,
    updated_at   TEXT NOT NULL,
    FOREIGN KEY (checklist_id) REFERENCES checklists(id) ON DELETE CASCADE
);

CREATE INDEX idx_checklist_items_checklist_id ON checklist_items(checklist_id);
