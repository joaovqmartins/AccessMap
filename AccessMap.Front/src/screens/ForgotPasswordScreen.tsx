import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { passwordResetApi } from '../api/auth';
import { ApiError } from '../api/client';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import TextField from '../components/TextField';
import KeyboardAvoidingScreen from '../components/KeyboardAvoidingScreen';
import { PASSWORD_MAX_BYTES, TEXT_MAX } from '../constants/limits';
import { colors, spacing } from '../constants/theme';
import { notify } from '../utils/dialog';
import { safeRedirect } from '../utils/navigation';
import { isValidEmail } from '../utils/email';
import { validateNewPassword } from '../utils/password';
import { authStyles } from './LoginScreen';

type Field = 'email' | 'code' | 'password' | 'confirmation';

/** Recuperação de senha por código enviado ao e-mail (#109), em dois passos. */
export default function ForgotPasswordScreen() {
  // Veio do login com destino (#102): volta para o login levando o mesmo destino
  const redirect = safeRedirect(useLocalSearchParams<{ redirect?: string }>().redirect);
  const [step, setStep] = useState<'email' | 'code'>('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function showError(e: unknown, fallback: string) {
    if (e instanceof ApiError) {
      setErrors(e.fields as Partial<Record<Field, string>>);
      setFormError(e.message);
    } else {
      setFormError(fallback);
    }
  }

  async function sendCode() {
    if (!isValidEmail(email)) {
      setErrors({ email: 'Informe o e-mail da sua conta' });
      return;
    }
    setErrors({});
    setFormError(null);
    setSubmitting(true);
    try {
      const { message } = await passwordResetApi.request(email.trim());
      setNotice(`${message} Confira também a caixa de spam. O código vale por 15 minutos.`);
      setCode('');
      setStep('code');
    } catch (e) {
      showError(e, 'Não foi possível enviar o código');
    } finally {
      setSubmitting(false);
    }
  }

  async function resetPassword() {
    const next: Partial<Record<Field, string>> = {};
    if (!/^\d{6}$/.test(code)) next.code = 'O código tem 6 números';
    const passwordError = validateNewPassword(password);
    if (passwordError) next.password = passwordError;
    if (confirmation !== password) next.confirmation = 'As senhas não conferem';
    setErrors(next);
    setFormError(null);
    if (Object.keys(next).length) return;

    setSubmitting(true);
    try {
      await passwordResetApi.confirm({ email: email.trim(), code, password });
      await notify('Senha alterada', 'Entre com seu telefone e a nova senha.');
      router.replace(redirect ? { pathname: '/login', params: { redirect } } : '/login');
    } catch (e) {
      showError(e, 'Não foi possível alterar a senha');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SafeAreaView style={authStyles.safe} edges={['bottom', 'left', 'right']}>
      <KeyboardAvoidingScreen style={authStyles.flex}>
        <ScrollView contentContainerStyle={authStyles.content} keyboardShouldPersistTaps="handled">
          <Text style={authStyles.title} accessibilityRole="header">Recuperar senha</Text>

          {step === 'email' ? (
            <>
              <Text style={authStyles.subtitle}>
                Informe o e-mail da sua conta. Vamos enviar um código para você criar uma nova senha.
              </Text>
              <ErrorMessage message={formError} />
              <TextField
                label="E-mail"
                value={email}
                onChangeText={setEmail}
                maxLength={TEXT_MAX}
                keyboardType="email-address"
                autoCapitalize="none"
                autoComplete="email"
                textContentType="emailAddress"
                error={errors.email}
                onSubmitEditing={sendCode}
              />
              <Button title="Enviar código" onPress={sendCode} loading={submitting} />
            </>
          ) : (
            <>
              <Text style={styles.notice} accessibilityLiveRegion="polite">{notice}</Text>
              <ErrorMessage message={formError} />
              <TextField
                label="Código"
                value={code}
                onChangeText={(v) => setCode(v.replace(/\D/g, '').slice(0, 6))}
                keyboardType="number-pad"
                autoComplete="one-time-code"
                textContentType="oneTimeCode"
                placeholder="6 números"
                error={errors.code}
              />
              <TextField
                label="Nova senha"
                value={password}
                onChangeText={setPassword}
                maxLength={PASSWORD_MAX_BYTES}
                secureTextEntry
                autoComplete="new-password"
                textContentType="newPassword"
                placeholder="Mínimo de 8 caracteres"
                error={errors.password}
              />
              <TextField
                label="Confirme a nova senha"
                value={confirmation}
                onChangeText={setConfirmation}
                maxLength={PASSWORD_MAX_BYTES}
                secureTextEntry
                autoComplete="new-password"
                textContentType="newPassword"
                error={errors.confirmation}
                onSubmitEditing={resetPassword}
              />
              <View style={styles.actions}>
                <Button title="Redefinir senha" onPress={resetPassword} loading={submitting} />
                <Button title="Reenviar código" variant="secondary" onPress={sendCode} disabled={submitting} />
              </View>
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingScreen>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  notice: {
    fontSize: 15,
    color: colors.text,
    backgroundColor: colors.surface,
    borderRadius: 8,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  actions: { gap: spacing.sm },
});
