import { ActivityIndicator, Pressable, StyleSheet, Text } from 'react-native';

import { MIN_TOUCH, colors, spacing } from '../constants/theme';

type Props = {
  title: string;
  onPress: () => void;
  /** `accent`: coral do pin da logo, para a ação principal da tela ("Avaliar este local"). */
  variant?: 'primary' | 'secondary' | 'danger' | 'accent';
  loading?: boolean;
  disabled?: boolean;
  accessibilityHint?: string;
};

export default function Button({ title, onPress, variant = 'primary', loading, disabled, accessibilityHint }: Props) {
  const inactive = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityHint={accessibilityHint}
      accessibilityState={{ disabled: !!inactive, busy: !!loading }}
      onPress={onPress}
      disabled={inactive}
      style={({ pressed }) => [
        styles.base,
        styles[variant],
        (pressed || inactive) && styles.dimmed,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'secondary' ? colors.primary : colors.onPrimary} />
      ) : (
        <Text style={[styles.text, variant === 'secondary' && styles.textSecondary]}>{title}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: MIN_TOUCH,
    borderRadius: 12,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 4,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primary: { backgroundColor: colors.primary },
  secondary: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.primary },
  danger: { backgroundColor: colors.danger },
  accent: { backgroundColor: colors.accent },
  dimmed: { opacity: 0.6 },
  text: { color: colors.onPrimary, fontSize: 16, fontWeight: '600' },
  textSecondary: { color: colors.primary },
});
