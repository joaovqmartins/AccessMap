import { PASSWORD_MAX_BYTES, PASSWORD_TOO_LONG, utf8Length } from '../constants/limits';

/** Espelha o backend: mínimo de 8 caracteres e até 72 bytes (limite do BCrypt). `undefined` quando está ok. */
export function validateNewPassword(password: string): string | undefined {
  if (password.length < 8) return 'A senha deve ter no mínimo 8 caracteres';
  if (utf8Length(password) > PASSWORD_MAX_BYTES) return PASSWORD_TOO_LONG;
  return undefined;
}
