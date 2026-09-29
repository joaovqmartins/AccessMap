import { Pressable, StyleSheet, Text, View } from 'react-native';

import { MIN_TOUCH, colors } from '../constants/theme';

type Props = {
  value: number;
  onChange: (value: number) => void;
  error?: string;
};

/** Seleção de nota de 1 a 5; cada estrela é um alvo de toque próprio, anunciado ao leitor de tela. */
export default function StarRatingInput({ value, onChange, error }: Props) {
  return (
    <View>
      <View style={styles.row} accessibilityRole="radiogroup" accessibilityLabel="Nota">
        {[1, 2, 3, 4, 5].map((n) => (
          <Pressable
            key={n}
            onPress={() => onChange(n)}
            accessibilityRole="radio"
            accessibilityLabel={`${n} ${n === 1 ? 'estrela' : 'estrelas'}`}
            accessibilityState={{ checked: value === n }}
            hitSlop={4}
            style={styles.target}
          >
            <Text style={[styles.star, n > value && styles.empty]}>★</Text>
          </Pressable>
        ))}
      </View>
      {!!error && <Text style={styles.error}>{error}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  target: { minWidth: MIN_TOUCH, minHeight: MIN_TOUCH, alignItems: 'center', justifyContent: 'center' },
  star: { fontSize: 36, color: colors.star },
  empty: { color: colors.border },
  error: { color: colors.danger, fontSize: 13, marginTop: 4 },
});
