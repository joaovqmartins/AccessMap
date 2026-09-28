-- Login e cadastro passam a ser por telefone (#77): e-mail vira opcional e telefone passa a ser único.
-- Postgres permite vários NULL numa constraint UNIQUE, então e-mails não informados não colidem.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

ALTER TABLE users ADD CONSTRAINT uk_users_phone UNIQUE (phone);
