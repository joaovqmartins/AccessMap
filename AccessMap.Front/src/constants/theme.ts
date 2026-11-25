// Paleta e espaçamentos do app. Toda cor de tela vem daqui: o teste de contraste
// (__tests__/a11y/contraste.test.ts) confere cada par texto/fundo contra o WCAG AA (#56).
export const colors = {
  // #007BFF dava 3,98:1 com o branco (botões, links, abas); #0062CC dá 5,8:1
  primary: '#0062CC',
  background: '#F5F5F5',
  surface: '#FFFFFF',
  // Fundo de campos de busca e de chips neutros
  surfaceMuted: '#F0F0F0',
  // Divisórias e bordas decorativas (não precisam de contraste: não identificam nada sozinhas)
  border: '#E0E0E0',
  // Borda de campo de formulário: identifica onde digitar, então pede 3:1 (WCAG 1.4.11). #E0E0E0 dava 1,3:1
  inputBorder: '#8C8C8C',
  text: '#333333',
  // #6B7280 dava 4,43:1 no fundo cinza; #5B6270 passa de 4,5:1 em todos os fundos do app
  textMuted: '#5B6270',
  danger: '#C62828',
  dangerBackground: '#FDECEA',
  success: '#2E7D32',
  successBackground: '#E8F5E9',
  // #B26A00 dava 4,24:1 com o branco; #9A5B00 dá 5,4:1
  warning: '#9A5B00',
  warningBackground: '#FFF4E5',
  infoBackground: '#E6F0FF',
  onPrimary: '#FFFFFF',
  star: '#F5A623',
  // Fundo escurecido atrás de menus e painéis
  overlay: 'rgba(0, 0, 0, 0.4)',
};

// Cor de cada avaliação de característica (sempre acompanhada de texto, nunca só a cor)
export const assessmentColors = {
  ADEQUADO: colors.success,
  INADEQUADO: colors.danger,
  // #9E9E9E tinha contraste 2,68:1 com o branco; #616161 dá 6,19:1 (WCAG AA pede 4,5:1 para texto)
  INEXISTENTE: '#616161',
} as const;

export const spacing = { xs: 4, sm: 8, md: 16, lg: 24 };

// Alvo mínimo de toque recomendado para acessibilidade
export const MIN_TOUCH = 44;

// Menor tamanho de texto do app. Abaixo disso fica difícil de ler para quem tem baixa visão.
export const MIN_FONT_SIZE = 13;
