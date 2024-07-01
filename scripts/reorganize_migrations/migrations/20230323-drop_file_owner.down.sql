DELETE FROM files WHERE owner_id IS NULL;
-- deleting all files without an owner is a drastic measure
-- but I couldn't think of anything better so...
ALTER TABLE files ALTER COLUMN owner_id SET NOT NULL;
