// Espelham o FieldLimits do backend: colunas VARCHAR(255) e o limite de 72 bytes do BCrypt

/** Nome, e-mail e comentário. */
export const TEXT_MAX = 255;

/** O BCrypt usa no máximo 72 bytes: letras acentuadas ocupam 2, então cabem menos de 72 caracteres. */
export const PASSWORD_MAX_BYTES = 72;

export const PASSWORD_TOO_LONG = 'Senha muito longa (máximo de 72 caracteres; letras acentuadas contam em dobro)';

/** Tamanho do texto em bytes UTF-8. */
export function utf8Length(text: string): number {
  let bytes = 0;
  for (const char of text) {
    const code = char.codePointAt(0) ?? 0;
    bytes += code < 0x80 ? 1 : code < 0x800 ? 2 : code < 0x10000 ? 3 : 4;
  }
  return bytes;
}
