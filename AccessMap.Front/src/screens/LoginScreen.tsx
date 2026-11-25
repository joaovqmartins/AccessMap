import { Link, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Image, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';

import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import TextField from '../components/TextField';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { safeRedirect, setPendingRedirect } from '../utils/navigation';
import { formatPhone, isValidPhone, onlyDigits } from '../utils/phone';

const LOGO = require('../../assets/images/logo.png');
// 200 de largura na proporção do arquivo (720x549). Fica fora do StyleSheet porque, na web,
// o react-native-web aplica o tamanho do arquivo como estilo inline, que venceria a classe.
const LOGO_SIZE = { width: 200, height: Math.round((200 * 549) / 720) };

export default function LoginScreen() {
  const { signIn } = useAuth();
  // Para onde voltar depois de entrar, quando veio de "Entrar para avaliar"/"denunciar" (#102)
  const redirect = safeRedirect(useLocalSearchParams<{ redirect?: string }>().redirect);
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ phone?: string; password?: string }>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit() {
    const nextErrors: typeof errors = {};
    if (!isValidPhone(phone)) nextErrors.phone = 'Informe um telefone com DDD (10 ou 11 dígitos)';
    if (!password) nextErrors.password = 'Informe sua senha';
    setErrors(nextErrors);
    setFormError(null);
    if (Object.keys(nextErrors).length) return;

    setSubmitting(true);
    try {
      setPendingRedirect(redirect);
      await signIn({ phone, password });
      // O guard do _layout troca para a Home (ou para o redirect) quando o status vira 'signedIn'
    } catch (e) {
      setPendingRedirect(null);
      setFormError(
        e instanceof ApiError && e.status === 401
          ? 'Telefone ou senha incorretos'
          : e instanceof Error ? e.message : 'Não foi possível entrar',
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SafeAreaView style={[styles.safe, styles.safeLogin]}>
      {/* Sem cabeçalho marinho aqui: relógio e bateria escuros sobre o fundo claro */}
      <StatusBar style="dark" />
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {/* Decorativa: o título logo abaixo já diz o nome para o leitor de tela */}
          <Image
            source={LOGO}
            style={[LOGO_SIZE, styles.logo]}
            resizeMode="contain"
            accessible={false}
            importantForAccessibility="no"
          />
          {/* Nome em duas cores, como na logo; o leitor de tela ouve uma palavra só */}
          <Text style={styles.title} accessibilityRole="header" accessibilityLabel="AccessMap">
            Access<Text style={styles.titleAccent}>Map</Text>
          </Text>
          <Text style={styles.subtitle}>Entre com seu telefone e senha</Text>

          <ErrorMessage message={formError} />

          <TextField
            label="Telefone"
            value={formatPhone(phone)}
            onChangeText={(v) => setPhone(onlyDigits(v))}
            keyboardType="phone-pad"
            autoComplete="tel"
            textContentType="telephoneNumber"
            placeholder="(11) 91234-5678"
            error={errors.phone}
          />
          <TextField
            label="Senha"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="current-password"
            textContentType="password"
            error={errors.password}
            onSubmitEditing={handleSubmit}
          />

          <Button title="Entrar" onPress={handleSubmit} loading={submitting} />

          <Link
            href={redirect ? { pathname: '/esqueci-senha', params: { redirect } } : '/esqueci-senha'}
            style={styles.link}
            accessibilityRole="link"
          >
            Esqueci minha senha
          </Link>

          <Link
            href={redirect ? { pathname: '/cadastro', params: { redirect } } : '/cadastro'}
            style={styles.link}
            accessibilityRole="link"
          >
            Não tem conta? Cadastre-se
          </Link>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

export const authStyles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  // O login não tem cabeçalho: o azul-claro do fundo da logo dá a cara do app logo na entrada
  safeLogin: { backgroundColor: colors.infoBackground },
  flex: { flex: 1 },
  content: { flexGrow: 1, justifyContent: 'center', padding: spacing.lg },
  logo: { alignSelf: 'center', marginBottom: spacing.md },
  title: { fontSize: 32, fontWeight: 'bold', color: colors.brand, textAlign: 'center' },
  titleAccent: { color: colors.accent },
  subtitle: { fontSize: 16, color: colors.textMuted, textAlign: 'center', marginBottom: spacing.lg },
  link: {
    marginTop: spacing.md,
    minHeight: MIN_TOUCH,
    textAlign: 'center',
    textAlignVertical: 'center',
    color: colors.primary,
    fontSize: 16,
    fontWeight: '600',
  },
});

const styles = authStyles;
