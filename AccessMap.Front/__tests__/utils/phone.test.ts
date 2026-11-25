import { formatPhone, isValidPhone, onlyDigits } from '../../src/utils/phone';

describe('onlyDigits', () => {
  it('tira a máscara e qualquer caractere que não seja dígito', () => {
    expect(onlyDigits('(11) 91234-5678')).toBe('11912345678');
  });

  it('corta em 11 dígitos, o máximo que o backend aceita', () => {
    expect(onlyDigits('119123456789999')).toBe('11912345678');
  });
});

describe('formatPhone', () => {
  it.each([
    ['', ''],
    ['11', '11'],
    ['1191', '(11) 91'],
    ['1112345678', '(11) 1234-5678'],
    ['11912345678', '(11) 91234-5678'],
  ])('%s vira "%s"', (input, expected) => {
    expect(formatPhone(input)).toBe(expected);
  });

  it('aceita null e undefined sem quebrar', () => {
    expect(formatPhone(null)).toBe('');
    expect(formatPhone(undefined)).toBe('');
  });
});

describe('isValidPhone', () => {
  it.each(['1112345678', '11912345678'])('%s é válido (10 ou 11 dígitos)', (phone) => {
    expect(isValidPhone(phone)).toBe(true);
  });

  it.each(['', '119123', '119123456789', '(11) 91234-5678'])('"%s" é inválido', (phone) => {
    expect(isValidPhone(phone)).toBe(false);
  });
});
