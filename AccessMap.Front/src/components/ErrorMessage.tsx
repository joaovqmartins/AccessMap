import { StyleSheet, Text } from 'react-native';

import { colors, spacing } from '../constants/theme';

export default function ErrorMessage({ message }: { message?: string | null }) {
  if (!message) return null;
  return (
    <Text style={styles.text} accessibilityRole="alert" accessibilityLiveRegion="assertive">
      {message}
    </Text>
  );
}

const styles = StyleSheet.create({
  text: {
    color: colors.danger,
    backgroundColor: '#FDECEA',
    borderRadius: 8,
    padding: spacing.sm + 4,
    marginBottom: spacing.md,
    fontSize: 14,
  },
});
