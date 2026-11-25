import { Stack, router, type Href } from 'expo-router';
import { useEffect } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { AuthProvider, useAuth } from '../auth/AuthContext';
import ErrorScreen from '../components/ErrorScreen';
import { colors } from '../constants/theme';
import { takePendingRedirect } from '../utils/navigation';

// Qualquer tela que quebre ao renderizar cai aqui, em vez de tela branca (#133)
export { ErrorScreen as ErrorBoundary };

// No iOS 26 o cabeçalho nativo é "Liquid Glass" e fica transparente quando não tem cor de fundo:
// o conteúdo começava por baixo dele (nome e telefone escondidos no perfil). Com fundo opaco ele
// volta a ocupar o próprio espaço, em todas as telas e plataformas.
const HEADER_OPTIONS = {
  headerTransparent: false,
  headerStyle: { backgroundColor: colors.surface },
  headerShadowVisible: true,
  headerTintColor: colors.primary,
  headerTitleStyle: { color: colors.text, fontWeight: '600' as const },
  contentStyle: { backgroundColor: colors.background },
};

export default function RootLayout() {
  return (
    <AuthProvider>
      <RootNavigator />
    </AuthProvider>
  );
}

function RootNavigator() {
  const { status, user } = useAuth();

  // Quem entrou por "Entrar para avaliar" volta para onde estava (#102). Sem destino guardado, o
  // Stack.Protected leva para a Home sozinho. O replace espera o Stack já com as telas de quem está logado.
  useEffect(() => {
    if (status !== 'signedIn') return;
    const redirect = takePendingRedirect();
    if (redirect) {
      const timer = setTimeout(() => router.replace(redirect as Href), 0);
      return () => clearTimeout(timer);
    }
  }, [status]);

  if (status === 'loading') {
    return (
      <View style={styles.loading} accessibilityLabel="Carregando">
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  const signedIn = status === 'signedIn';
  return (
    <Stack screenOptions={HEADER_OPTIONS}>
      <Stack.Protected guard={signedIn}>
        {/* O título não aparece (sem cabeçalho), mas é o que o "voltar" das outras telas anuncia */}
        <Stack.Screen name="index" options={{ headerShown: false, title: 'Início' }} />
        <Stack.Screen name="perfil" options={{ title: 'Meu perfil' }} />
        <Stack.Screen name="minhas-avaliacoes" options={{ title: 'Minhas avaliações' }} />
        <Stack.Screen name="locais/[placeId]/avaliar" options={{ title: 'Avaliar local' }} />
        <Stack.Screen name="avaliacoes/[id]/editar" options={{ title: 'Editar avaliação' }} />
      </Stack.Protected>
      {/* Quem não é ADMIN e abre o endereço volta para a Home; o backend também responde 403 */}
      <Stack.Protected guard={user?.role === 'ADMIN'}>
        <Stack.Screen name="moderacao" options={{ title: 'Moderação' }} />
      </Stack.Protected>
      <Stack.Protected guard={!signedIn}>
        <Stack.Screen name="(auth)/login" options={{ headerShown: false, title: 'Entrar' }} />
        <Stack.Screen name="(auth)/cadastro" options={{ title: 'Cadastro' }} />
        <Stack.Screen name="(auth)/esqueci-senha" options={{ title: 'Recuperar senha' }} />
      </Stack.Protected>
      {/* Leitura pública, como no backend */}
      <Stack.Screen name="locais/[placeId]/index" options={{ title: 'Local' }} />
      <Stack.Screen name="locais/[placeId]/avaliacoes" options={{ title: 'Avaliações' }} />
      <Stack.Screen name="avaliacoes/[id]/index" options={{ title: 'Avaliação' }} />
      {/* Só em desenvolvimento: força um erro para o e2e conferir a tela de erro */}
      <Stack.Screen name="dev/erro" options={{ title: 'Teste de erro' }} />
    </Stack>
  );
}

const styles = StyleSheet.create({
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
});
