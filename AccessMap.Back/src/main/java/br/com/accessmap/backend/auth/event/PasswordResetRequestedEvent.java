package br.com.accessmap.backend.auth.event;

/** Código gerado e gravado. O e-mail só sai depois do commit, para não mandar um código que não valeu. */
public record PasswordResetRequestedEvent(String email, String name, String code) {
}
