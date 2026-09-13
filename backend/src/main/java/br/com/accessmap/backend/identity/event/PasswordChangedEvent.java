package br.com.accessmap.backend.identity.event;

/**
 * Publicado quando a senha de um usuario muda. Quem mantem sessoes (ex.: refresh tokens)
 * escuta e as invalida, sem que o modulo identity precise conhecer o modulo de autenticacao.
 */
public record PasswordChangedEvent(String userId) {
}
