UPDATE users
SET nickname = LEFT(COALESCE(NULLIF(TRIM(nickname), ''), username), 50)
WHERE nickname IS NULL
   OR TRIM(nickname) = ''
   OR CHAR_LENGTH(nickname) > 50;

ALTER TABLE users
    MODIFY COLUMN nickname VARCHAR(50) NOT NULL;
