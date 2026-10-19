// Paleta e espaçamentos base, extraídos da HomeScreen.
export const colors = {
  primary: '#007BFF',
  background: '#F5F5F5',
  surface: '#FFFFFF',
  border: '#E0E0E0',
  text: '#333333',
  textMuted: '#6B7280',
  danger: '#C62828',
  success: '#2E7D32',
  warning: '#B26A00',
  onPrimary: '#FFFFFF',
  star: '#F5A623',
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
