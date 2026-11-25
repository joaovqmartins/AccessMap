import { Link, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import TextField from '../components/TextField';
import { PASSWORD_MAX_BYTES, TEXT_MAX } from '../constants/limits';
import { isValidEmail } from '../utils/email';
import { safeRedirect, setPendingRedirect } from '../utils/navigation';
import { formatPhone, isValidPhone, onlyDigits } from '../utils/phone';
import { validateNewPassword } from '../utils/password';
import { authStyles as styles } from './LoginScreen';

type Field = 'name' | 'phone' | 'email' | 'password';

export default function RegisterScreen() {
  const { signUp } = useAuth();
  // Para onde voltar depois do cadastro, quando veio de "Entrar para avaliar" (#102)
  const redirect = safeRedirect(useLocalSearchParams<{ redirect?: string }>().redirect);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Espelha as validações do RegisterRequestDto
  function validate() {
    const next: Partial<Record<Field, string>> = {};
    if (!name.trim()) next.name = 'Informe seu nome';
    if (!isValidPhone(phone)) next.phone = 'Informe um telefone com DDD (10 ou 11 dígitos)';
    if (!isValidEmail(email)) next.email = 'Informe um e-mail válido';
    const passwordError = validateNewPassword(password);
    if (passwordError) next.password = passwordError;
    return next;
  }

  async function handleSubmit() {
    const next = validate();
    setErrors(next);
    setFormError(null);
    if (Object.keys(next).length) return;

    setSubmitting(true);
    try {
      setPendingRedirect(redirect);
      await signUp({ name: name.trim(), phone, email: email.trim(), password });
    } catch (e) {
      setPendingRedirect(null);
      if (e instanceof ApiError) {
        setErrors(e.fields as Partial<Record<Field, string>>);
        setFormError(e.message);
      } else {
        setFormError('Não foi possível concluir o cadastro');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Text style={styles.title} accessibilityRole="header">Criar conta</Text>
          <Text style={styles.subtitle}>
            Idade e necessidades de acessibilidade podem ser informadas depois, no perfil.
          </Text>

          <ErrorMessage message={formError} />

          <TextField
            label="Nome"
            value={name}
            onChangeText={setName}
            maxLength={TEXT_MAX}
            autoComplete="name"
            textContentType="name"
            error={errors.name}
          />
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
            label="E-mail"
            value={email}
            onChangeText={setEmail}
            maxLength={TEXT_MAX}
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            textContentType="emailAddress"
            placeholder="Usado para recuperar a senha"
            error={errors.email}
          />
          <TextField
            label="Senha"
            value={password}
            onChangeText={setPassword}
            maxLength={PASSWORD_MAX_BYTES}
            secureTextEntry
            autoComplete="new-password"
            textContentType="newPassword"
            placeholder="Mínimo de 8 caracteres"
            error={errors.password}
            onSubmitEditing={handleSubmit}
          />

          <Button title="Cadastrar" onPress={handleSubmit} loading={submitting} />

          <Link
            href={redirect ? { pathname: '/login', params: { redirect } } : '/login'}
            style={styles.link}
            accessibilityRole="link"
          >
            Já tem conta? Entrar
          </Link>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
