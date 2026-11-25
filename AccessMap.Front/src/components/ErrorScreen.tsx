import { router, type ErrorBoundaryProps } from 'expo-router';
import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, spacing } from '../constants/theme';
import Button from './Button';

/**
 * Tela mostrada quando uma rota quebra ao renderizar (#133), em vez da tela branca no celular.
 * Fica fora do AuthProvider (o ErrorBoundary do _layout raiz substitui o layout inteiro), então não usa useAuth.
 */
export default function ErrorScreen({ error, retry }: ErrorBoundaryProps) {
  useEffect(() => {
    // Só a mensagem: o objeto do erro pode carregar a resposta da API, com token ou telefone
    console.error('Erro ao renderizar a tela:', error.message);
  }, [error]);

  function goHome() {
    // Primeiro troca a rota, depois re-renderiza: senão o retry desenha de novo a tela que quebrou
    router.replace('/');
    retry();
  }

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.content}>
        <Text style={styles.title} accessibilityRole="alert">
          Algo deu errado
        </Text>
        <Text style={styles.message}>
          Esta tela não pôde ser exibida. Tente de novo ou volte ao início.
        </Text>
        <View style={styles.actions}>
          <Button title="Tentar de novo" onPress={() => retry()} />
          <Button title="Voltar ao início" variant="secondary" onPress={goHome} />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  content: { flex: 1, justifyContent: 'center', padding: spacing.lg },
  title: { fontSize: 24, fontWeight: '700', color: colors.text, textAlign: 'center' },
  message: { fontSize: 16, color: colors.textMuted, textAlign: 'center', marginTop: spacing.sm },
  actions: { gap: spacing.sm, marginTop: spacing.lg },
});
