/** Mantém só os dígitos (o backend espera 10 ou 11 dígitos, sem máscara). */
export function onlyDigits(value: string) {
  return value.replace(/\D/g, '').slice(0, 11);
}

/** Formata para exibição: (11) 91234-5678 ou (11) 1234-5678. */
export function formatPhone(value: string | null | undefined) {
  const digits = onlyDigits(value ?? '');
  if (digits.length <= 2) return digits;
  const ddd = digits.slice(0, 2);
  const rest = digits.slice(2);
  const split = rest.length > 8 ? 5 : 4;
  if (rest.length <= split) return `(${ddd}) ${rest}`;
  return `(${ddd}) ${rest.slice(0, split)}-${rest.slice(split)}`;
}

export function isValidPhone(value: string) {
  return /^\d{10,11}$/.test(value);
}
