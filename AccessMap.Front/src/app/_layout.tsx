import { Stack, router, type Href } from 'expo-router';
import { useEffect } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { AuthProvider, useAuth } from '../auth/AuthContext';
import { colors } from '../constants/theme';
import { takePendingRedirect } from '../utils/navigation';

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
    <Stack>
      <Stack.Protected guard={signedIn}>
        <Stack.Screen name="index" options={{ headerShown: false }} />
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
        <Stack.Screen name="(auth)/login" options={{ headerShown: false }} />
        <Stack.Screen name="(auth)/cadastro" options={{ title: 'Cadastro' }} />
      </Stack.Protected>
      {/* Leitura pública, como no backend */}
      <Stack.Screen name="locais/[placeId]/index" options={{ title: 'Local' }} />
      <Stack.Screen name="locais/[placeId]/avaliacoes" options={{ title: 'Avaliações' }} />
      <Stack.Screen name="avaliacoes/[id]/index" options={{ title: 'Avaliação' }} />
    </Stack>
  );
}

const styles = StyleSheet.create({
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
});
