import { assessmentColors, colors } from '../../src/constants/theme';

// Contraste WCAG 2.1 de cada par de cores que o app usa (#56).
// Texto pede 4,5:1 (AA); borda de campo e ícone que identifica algo pede 3:1 (1.4.11).
// Ao criar uma combinação nova de texto e fundo, acrescente o par aqui.

function luminance(hex: string) {
  const value = hex.replace('#', '');
  const channels = [0, 2, 4].map((i) => parseInt(value.slice(i, i + 2), 16) / 255);
  const [r, g, b] = channels.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string) {
  const [light, dark] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (light + 0.05) / (dark + 0.05);
}

const TEXT = 4.5;
const UI = 3;

// [descrição, cor da frente, cor do fundo, mínimo]
const pairs: [string, string, string, number][] = [
  ['texto no fundo da tela', colors.text, colors.background, TEXT],
  ['texto nos cards', colors.text, colors.surface, TEXT],
  ['texto nos chips neutros', colors.text, colors.border, TEXT],
  ['texto no aviso da busca', colors.text, colors.warningBackground, TEXT],
  ['texto secundário nos cards', colors.textMuted, colors.surface, TEXT],
  ['texto secundário no fundo da tela', colors.textMuted, colors.background, TEXT],
  ['texto secundário nos chips neutros', colors.textMuted, colors.border, TEXT],
  ['texto secundário no campo de busca', colors.textMuted, colors.surfaceMuted, TEXT],
  ['texto secundário nos chips de necessidade', colors.textMuted, colors.infoBackground, TEXT],
  ['links e abas ativas nos cards', colors.primary, colors.surface, TEXT],
  ['links no fundo da tela', colors.primary, colors.background, TEXT],
  ['texto dos chips de necessidade', colors.primary, colors.infoBackground, TEXT],
  ['texto do botão primário', colors.onPrimary, colors.primary, TEXT],
  ['erro nos cards', colors.danger, colors.surface, TEXT],
  ['erro na caixa de erro', colors.danger, colors.dangerBackground, TEXT],
  ['texto do botão de perigo', colors.onPrimary, colors.danger, TEXT],
  ['sucesso nos cards', colors.success, colors.surface, TEXT],
  ['sucesso no chip de sucesso', colors.success, colors.successBackground, TEXT],
  ['aviso nos cards', colors.warning, colors.surface, TEXT],
  ['aviso na caixa de aviso', colors.warning, colors.warningBackground, TEXT],
  ['"Adequado" nos cards', assessmentColors.ADEQUADO, colors.surface, TEXT],
  ['"Inadequado" nos cards', assessmentColors.INADEQUADO, colors.surface, TEXT],
  ['"Inexistente" nos cards', assessmentColors.INEXISTENTE, colors.surface, TEXT],
  ['texto da opção marcada no seletor de características', colors.onPrimary, assessmentColors.INEXISTENTE, TEXT],
  ['borda dos campos nos cards', colors.inputBorder, colors.surface, UI],
  ['borda do campo de busca no cabeçalho', colors.inputBorder, colors.surface, UI],
  ['borda dos campos no fundo da tela', colors.inputBorder, colors.background, UI],
];

describe('contraste das cores do tema (WCAG AA)', () => {
  it.each(pairs)('%s', (_label, foreground, background, minimum) => {
    expect(contrast(foreground, background)).toBeGreaterThanOrEqual(minimum);
  });

  it('a conta de contraste bate com os valores de referência do WCAG', () => {
    expect(contrast('#000000', '#FFFFFF')).toBeCloseTo(21, 1);
    expect(contrast('#FFFFFF', '#FFFFFF')).toBeCloseTo(1, 5);
    // O azul antigo, que motivou a troca
    expect(contrast('#007BFF', '#FFFFFF')).toBeLessThan(TEXT);
  });
});
