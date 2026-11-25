-- Indices para as consultas mais frequentes de avaliacoes (#131). Os indices que ja existiam
-- (uk_reviews_user_place_active, idx_reviews_user_created_at) comecam por user_id e nao serviam a estas.
--
-- CREATE INDEX comum, nao CONCURRENTLY: o Flyway roda a migration numa transacao e as tabelas ainda sao
-- pequenas, entao o bloqueio dura milissegundos. Com volume grande, o caminho seria CONCURRENTLY numa
-- migration sem transacao.

-- Avaliacoes do local (tela de detalhes): place_id = ? AND status = 'PUBLICADA' ORDER BY created_at DESC.
-- O prefixo (place_id, status) tambem serve aos agregados recalculados a cada avaliacao criada, editada,
-- removida, denunciada ou moderada.
CREATE INDEX idx_reviews_place_status_created ON reviews (place_id, status, created_at DESC);

-- Fila de moderacao do ADMIN: suspicion_reason IS NOT NULL ORDER BY created_at. Parcial: so as reviews
-- com suspeita entram no indice, ja na ordem da fila; o filtro por status sobra para poucas linhas.
CREATE INDEX idx_reviews_moderation_queue ON reviews (created_at) WHERE suspicion_reason IS NOT NULL;

-- Exclusao de conta: o ON DELETE CASCADE de fk_review_reports_reporter procura as denuncias por
-- reporter_id. A unique (review_id, reporter_id) comeca por review_id e nao ajuda.
CREATE INDEX idx_review_reports_reporter ON review_reports (reporter_id);
