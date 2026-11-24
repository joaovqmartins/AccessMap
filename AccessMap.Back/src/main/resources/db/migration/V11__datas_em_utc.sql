-- A partir da #115 a API grava datas em UTC. Ate aqui, gravava no fuso da maquina: horario de Brasilia
-- nas maquinas de desenvolvimento e nos testes (ainda nao houve deploy). Converte o que ja existe:
-- "x AT TIME ZONE 'America/Sao_Paulo'" le x como horario de Brasilia; "AT TIME ZONE 'UTC'" devolve em UTC.
UPDATE users SET
    created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    updated_at = (updated_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';

UPDATE places SET
    created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    updated_at = (updated_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';

UPDATE reviews SET
    created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    updated_at = (updated_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    moderation_removed_at = (moderation_removed_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    moderation_restored_at = (moderation_restored_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';

UPDATE refresh_tokens SET
    expires_at = (expires_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    revoked_at = (revoked_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC',
    created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';

UPDATE review_reports SET
    created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';

UPDATE auth_attempts SET
    created_at = (created_at AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'UTC';
