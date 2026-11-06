-- suspicion_reason preenchido = a review esta na fila do ADMIN.
-- moderation_restored_at preenchido = o ADMIN restaurou, e denuncias nao ocultam de novo.
ALTER TABLE reviews ADD COLUMN suspicion_reason VARCHAR(255);
ALTER TABLE reviews ADD COLUMN moderation_restored_at TIMESTAMP(6);

-- Idade da conta na hora da denuncia: uma conta criada so para denunciar nao passa a valer depois.
ALTER TABLE review_reports ADD COLUMN reporter_established BOOLEAN NOT NULL DEFAULT FALSE;
