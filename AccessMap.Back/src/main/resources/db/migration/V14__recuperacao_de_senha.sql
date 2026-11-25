-- #109: recuperacao de senha por codigo enviado ao e-mail.

-- O e-mail passa a ser procurado sem diferenciar maiusculas: grava tudo em minusculas e
-- impede dois cadastros que so mudam a caixa. Contas antigas sem e-mail continuam com null.
UPDATE users SET email = lower(trim(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email));

-- Codigo de 6 digitos com validade curta. So o hash BCrypt fica no banco: com 6 digitos,
-- um SHA-256 se quebraria na hora se o banco vazasse.
CREATE TABLE password_reset_codes (
    id         VARCHAR(255) NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    attempts   INTEGER      NOT NULL DEFAULT 0,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at    TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_password_reset_codes PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_codes_user_id ON password_reset_codes (user_id);
