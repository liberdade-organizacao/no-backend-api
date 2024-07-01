CREATE OR REPLACE FUNCTION update_file_size()
RETURNS TRIGGER AS $$
BEGIN
    NEW.file_size = LENGTH(NEW.contents);