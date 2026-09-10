-- Assign pre-existing workspaces to the first local user during the ownership transition.
ALTER TABLE workspaces ADD COLUMN owner_user_id INTEGER;

UPDATE workspaces
SET owner_user_id = (SELECT MIN(id) FROM users)
WHERE owner_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_workspaces_owner_user_id ON workspaces (owner_user_id);