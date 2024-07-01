CREATE OR REPLACE FUNCTION update_last_updated_at_column() 
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_at = now();