/** Checagem simples, só para avisar antes de enviar. Quem decide é o @Email do backend. */
export function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}
