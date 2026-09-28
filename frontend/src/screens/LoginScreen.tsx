import { Link } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import TextField from '../components/TextField';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { formatPhone, isValidPhone, onlyDigits } from '../utils/phone';

export default function LoginScreen() {
  const { signIn } = useAuth();
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
      await signIn({ phone, password });
      // O guard do _layout troca para a Home quando o status vira 'signedIn'
    } catch (e) {
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
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Text style={styles.title} accessibilityRole="header">AccessMap</Text>
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

          <Link href="/cadastro" style={styles.link} accessibilityRole="link">
            Não tem conta? Cadastre-se
          </Link>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

export const authStyles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  content: { flexGrow: 1, justifyContent: 'center', padding: spacing.lg },
  title: { fontSize: 32, fontWeight: 'bold', color: colors.primary, textAlign: 'center' },
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
