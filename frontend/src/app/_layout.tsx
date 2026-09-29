import { Stack } from 'expo-router';
import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { AuthProvider, useAuth } from '../auth/AuthContext';
import { colors } from '../constants/theme';

export default function RootLayout() {
  return (
    <AuthProvider>
      <RootNavigator />
    </AuthProvider>
  );
}

function RootNavigator() {
  const { status } = useAuth();

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
        <Stack.Screen name="locais/[placeId]/avaliar" options={{ title: 'Avaliar local' }} />
        <Stack.Screen name="avaliacoes/[id]/editar" options={{ title: 'Editar avaliação' }} />
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
