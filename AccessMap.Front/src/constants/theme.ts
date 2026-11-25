// Paleta e espaçamentos do app, tirada da logo (assets/images/logo.png): azul-marinho do contorno,
// azul da cadeira de rodas, coral do pin, verde do mapa, amarelo e laranja dos detalhes.
// Toda cor de tela vem daqui: o teste de contraste (__tests__/a11y/contraste.test.ts) confere cada
// par texto/fundo contra o WCAG AA (#56). As cores vivas da logo viram tons mais fechados quando
// precisam carregar texto; as originais ficam só para enfeite (bright).
export const colors = {
  // Azul-marinho do contorno da logo: cabeçalhos e títulos. Branco sobre ele dá 12,3:1
  brand: '#1C2B7F',
  onBrand: '#FFFFFF',
  // Azul da cadeira de rodas: botões, links, abas. Branco sobre ele dá 6,6:1
  primary: '#1A56C4',
  // Coral do pin: avatar, "Avaliar", aba ativa, "sua avaliação". O coral da logo (#F2455C) dá 3,6:1
  // com o branco; este, mais fechado, dá 5,8:1
  accent: '#C2263F',
  accentSoft: '#FDE8EB',
  // Fundo azul-claro da logo, bem suave
  background: '#F4F7FD',
  surface: '#FFFFFF',
  // Fundo de campos de busca e de chips neutros
  surfaceMuted: '#EEF2FA',
  // Divisórias e bordas decorativas (não precisam de contraste: não identificam nada sozinhas)
  border: '#DDE3F0',
  // Borda de campo de formulário: identifica onde digitar, então pede 3:1 (WCAG 1.4.11)
  inputBorder: '#7A8199',
  text: '#1F2540',
  textMuted: '#565E78',
  // Erro usa o coral da marca: sempre vem acompanhado de texto, nunca só a cor
  danger: '#C2263F',
  dangerBackground: '#FDE8EB',
  success: '#17733F',
  successBackground: '#E2F5EA',
  warning: '#9A4C00',
  warningBackground: '#FFF4D6',
  infoBackground: '#E4EEFD',
  onPrimary: '#FFFFFF',
  // Amarelo da logo: decorativo no branco (a nota sempre aparece em texto); 7,1:1 sobre o marinho
  star: '#FDB927',
  // Coral vivo da logo, só para enfeite (bolinhas do menu, detalhes): 3,6:1 no branco, nunca como texto
  accentBright: '#F2455C',
  // Fundo escurecido atrás de menus e painéis
  overlay: 'rgba(16, 24, 72, 0.45)',
};

/**
 * Cor de cada categoria da Home, em rodízio: fundo suave quando solta, cor cheia com texto branco
 * quando escolhida. Toda cor aqui passa de 4,5:1 com o branco e com o texto sobre o fundo suave.
 */
export const categoryColors = [
  { solid: colors.primary, soft: colors.infoBackground },
  { solid: colors.accent, soft: colors.accentSoft },
  { solid: colors.success, soft: colors.successBackground },
  { solid: colors.warning, soft: colors.warningBackground },
] as const;

export function categoryColor(index: number) {
  return categoryColors[index % categoryColors.length];
}

// Cor de cada avaliação de característica (sempre acompanhada de texto, nunca só a cor)
export const assessmentColors = {
  ADEQUADO: colors.success,
  INADEQUADO: colors.danger,
  // #9E9E9E tinha contraste 2,68:1 com o branco; #5E6478 passa de 4,5:1
  INEXISTENTE: '#5E6478',
} as const;

export const spacing = { xs: 4, sm: 8, md: 16, lg: 24 };

// Alvo mínimo de toque recomendado para acessibilidade
export const MIN_TOUCH = 44;

// Menor tamanho de texto do app. Abaixo disso fica difícil de ler para quem tem baixa visão.
export const MIN_FONT_SIZE = 13;
