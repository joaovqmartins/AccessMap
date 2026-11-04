-- Denuncias de avaliacoes pela comunidade. O indice unico impede o mesmo usuario de denunciar
-- a mesma avaliacao duas vezes, inclusive em POSTs simultaneos.
-- reason sem CHECK de proposito: a lista de motivos ainda pode mudar com as regras da #27.
CREATE TABLE review_reports (
    id          VARCHAR(255) NOT NULL,
    review_id   VARCHAR(255) NOT NULL,
    reporter_id VARCHAR(255) NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_review_reports PRIMARY KEY (id),
    CONSTRAINT uk_review_reports_review_reporter UNIQUE (review_id, reporter_id),
    CONSTRAINT fk_review_reports_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    CONSTRAINT fk_review_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE
);
