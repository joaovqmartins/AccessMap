import { StyleSheet, Text } from 'react-native';

import { colors, spacing } from '../constants/theme';
import type { PlacesMapProps } from './PlacesMap';

/**
 * O react-native-maps não roda no navegador. Este arquivo substitui o mapa na web para ele nem entrar
 * no bundle; a Home já esconde a opção de mapa lá, então isto só aparece se alguém usar o componente direto.
 */
export default function PlacesMap(_props: PlacesMapProps) {
  return <Text style={styles.notice}>O mapa está disponível no aplicativo. Use a lista.</Text>;
}

const styles = StyleSheet.create({
  notice: { fontSize: 15, color: colors.textMuted, textAlign: 'center', padding: spacing.lg },
});
