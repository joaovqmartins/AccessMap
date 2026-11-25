package br.com.accessmap.backend.identity.event;

/**
 * Publicado antes de apagar um usuário, na mesma transação. Quem guarda dados ligados a ele
 * (ex.: avaliações) escuta e os trata, sem que o módulo identity precise conhecer os outros módulos.
 */
public record UserDeletedEvent(String userId) {
}
