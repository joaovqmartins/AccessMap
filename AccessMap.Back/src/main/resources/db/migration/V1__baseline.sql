-- Baseline do schema existente, transcrito do DDL que o Hibernate gerava com ddl-auto=update.
-- A partir daqui o schema e versionado pelo Flyway e o Hibernate apenas valida.

CREATE TABLE users (
    id         VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(255),
    age        INTEGER,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_accessibility_needs (
    user_id VARCHAR(255) NOT NULL,
    need    VARCHAR(255) CHECK (need IN ('MOBILIDADE_REDUZIDA', 'DEFICIENCIA_VISUAL', 'DEFICIENCIA_AUDITIVA', 'OUTROS')),
    CONSTRAINT uk_user_accessibility_needs UNIQUE (user_id, need),
    CONSTRAINT fk_user_accessibility_needs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE places (
    id            VARCHAR(255) NOT NULL,
    place_id      VARCHAR(255) NOT NULL,
    average_score DOUBLE PRECISION,
    review_count  INTEGER,
    created_at    TIMESTAMP(6),
    updated_at    TIMESTAMP(6),
    CONSTRAINT pk_places PRIMARY KEY (id),
    CONSTRAINT uk_places_place_id UNIQUE (place_id)
);

CREATE TABLE place_tag_stats (
    place_id          VARCHAR(255) NOT NULL,
    tag               VARCHAR(255) NOT NULL CHECK (tag IN ('RAMPAS_E_ENTRADAS', 'ELEVADORES', 'BANHEIROS_ADAPTADOS', 'VAGAS_ESTACIONAMENTO', 'SINALIZACAO', 'ESPACO_CIRCULACAO', 'ATENDIMENTO', 'OUTROS')),
    adequado_count    BIGINT,
    inadequado_count  BIGINT,
    inexistente_count BIGINT,
    CONSTRAINT pk_place_tag_stats PRIMARY KEY (place_id, tag),
    CONSTRAINT fk_place_tag_stats_place FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE TABLE reviews (
    id         VARCHAR(255) NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    place_id   VARCHAR(255) NOT NULL,
    rating     INTEGER      NOT NULL,
    comment    VARCHAR(255),
    status     VARCHAR(255) NOT NULL CHECK (status IN ('PUBLICADA', 'OCULTA', 'REMOVIDA')),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT pk_reviews PRIMARY KEY (id)
);

CREATE TABLE review_tags (
    review_id  VARCHAR(255) NOT NULL,
    tag        VARCHAR(255) NOT NULL CHECK (tag IN ('RAMPAS_E_ENTRADAS', 'ELEVADORES', 'BANHEIROS_ADAPTADOS', 'VAGAS_ESTACIONAMENTO', 'SINALIZACAO', 'ESPACO_CIRCULACAO', 'ATENDIMENTO', 'OUTROS')),
    assessment VARCHAR(255) CHECK (assessment IN ('ADEQUADO', 'INADEQUADO', 'INEXISTENTE')),
    CONSTRAINT pk_review_tags PRIMARY KEY (review_id, tag),
    CONSTRAINT fk_review_tags_review FOREIGN KEY (review_id) REFERENCES reviews (id)
);

CREATE TABLE review_reviewer_needs (
    review_id VARCHAR(255) NOT NULL,
    need      VARCHAR(255) CHECK (need IN ('MOBILIDADE_REDUZIDA', 'DEFICIENCIA_VISUAL', 'DEFICIENCIA_AUDITIVA', 'OUTROS')),
    CONSTRAINT uk_review_reviewer_needs UNIQUE (review_id, need),
    CONSTRAINT fk_review_reviewer_needs_review FOREIGN KEY (review_id) REFERENCES reviews (id)
);
