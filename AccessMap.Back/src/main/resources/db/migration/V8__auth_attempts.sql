-- Tentativas falhas de login e cadastro, para limitar forca bruta. A chave e o hash SHA-256 do
-- telefone ou do IP: nao guarda em texto puro um numero digitado por engano nem o IP de ninguem.
CREATE TABLE auth_attempts (
    id         VARCHAR(255) NOT NULL,
    kind       VARCHAR(255) NOT NULL,
    key_hash   VARCHAR(64)  NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_auth_attempts PRIMARY KEY (id)
);

CREATE INDEX idx_auth_attempts_lookup ON auth_attempts (kind, key_hash, created_at);
CREATE INDEX idx_auth_attempts_created_at ON auth_attempts (created_at);
