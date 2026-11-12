package br.com.accessmap.backend.validation;

/** Limites dos campos de texto, coerentes com as colunas do banco e com o BCrypt. */
public final class FieldLimits {

    /** Largura das colunas {@code VARCHAR(255)} (nome, e-mail, comentário). */
    public static final int TEXT_MAX = 255;

    /** O BCrypt usa no máximo 72 bytes da senha. */
    public static final int PASSWORD_MAX_BYTES = 72;

    public static final String PASSWORD_TOO_LONG =
            "Senha muito longa (máximo de 72 caracteres; letras acentuadas contam em dobro)";

    private FieldLimits() {
    }
}
