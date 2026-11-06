-- Nivel da conta quando a review foi criada: reviews antigas ficam REGULAR, ou seja, peso 1,0.
-- moderation_removed_at preenchido = removida pela moderacao, o que conta para o nivel RESTRITA.
-- Separado de status porque REMOVIDA vale tanto para o autor quanto para o ADMIN.
ALTER TABLE reviews ADD COLUMN author_trust_level VARCHAR(255) NOT NULL DEFAULT 'REGULAR';
ALTER TABLE reviews ADD COLUMN moderation_removed_at TIMESTAMP(6);
