-- Nome do local como o Google mostrava quando alguem avaliou. Serve para listas que nao tem o nome
-- por perto, como "Minhas avaliacoes". Locais antigos ficam com NULL ate a proxima avaliacao.
ALTER TABLE places ADD COLUMN name VARCHAR(255);
