-- Uma avaliacao publicada por usuario por local. Indice parcial: avaliacoes removidas (soft delete)
-- nao contam, para que o usuario possa avaliar de novo apos remover a anterior.
-- Fecha a janela de corrida que a checagem no service, sozinha, deixava em POSTs simultaneos.
CREATE UNIQUE INDEX uk_reviews_user_place_active
    ON reviews (user_id, place_id)
    WHERE status <> 'REMOVIDA';
