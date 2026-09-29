import { StyleSheet, Text, View } from 'react-native';

import { colors } from '../constants/theme';

type Props = {
  value: number;
  size?: number;
};

/** Exibição somente leitura de uma nota de 0 a 5 (arredondada para a estrela mais próxima). */
export default function StarRating({ value, size = 18 }: Props) {
  const filled = Math.round(value);
  return (
    <View
      style={styles.row}
      accessible
      accessibilityRole="image"
      accessibilityLabel={`Nota ${value.toFixed(1).replace('.', ',')} de 5`}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <Text key={n} style={[styles.star, { fontSize: size }, n > filled && styles.empty]}>
          ★
        </Text>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  star: { color: colors.star, marginRight: 2 },
  empty: { color: colors.border },
});
