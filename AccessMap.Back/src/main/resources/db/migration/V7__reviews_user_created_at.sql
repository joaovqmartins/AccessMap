-- O limite de frequencia conta as avaliacoes do usuario por janela de tempo a cada POST /api/reviews.
CREATE INDEX idx_reviews_user_created_at ON reviews (user_id, created_at);
