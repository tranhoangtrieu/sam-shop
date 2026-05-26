-- Image column stores public URL path (not file bytes)
ALTER TABLE products ALTER COLUMN image TYPE VARCHAR(512);
