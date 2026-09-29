import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { ApiError } from '../api/client';
import type { AccessibilityNeed, User, UserUpdateRequest } from '../api/types';
import { usersApi } from '../api/users';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import NeedsSelector from '../components/NeedsSelector';
import TextField from '../components/TextField';
import { NEED_LABELS } from '../constants/labels';
import { colors, spacing } from '../constants/theme';
import { formatPhone, isValidPhone, onlyDigits } from '../utils/phone';

type Form = {
  name: string;
  phone: string;
  email: string;
  age: string;
  accessibilityNeeds: AccessibilityNeed[];
};

type Errors = Partial<Record<keyof Form, string>>;

function toForm(user: User): Form {
  return {
    name: user.name,
    phone: user.phone ?? '',
    email: user.email ?? '',
    age: user.age != null ? String(user.age) : '',
    accessibilityNeeds: user.accessibilityNeeds ?? [],
  };
}

export default function ProfileScreen() {
  const { user, refreshUser, signOut } = useAuth();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<Form | null>(null);
  const [errors, setErrors] = useState<Errors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  if (!user) return null;

  function startEditing() {
    setForm(toForm(user!));
    setErrors({});
    setFormError(null);
    setSuccess(null);
    setEditing(true);
  }

  function set<K extends keyof Form>(key: K, value: Form[K]) {
    setForm((prev) => (prev ? { ...prev, [key]: value } : prev));
  }

  // Espelha o UserRequestDto e a regra do backend de que um campo preenchido não pode ser esvaziado
  function validate(f: Form, original: User): Errors {
    const next: Errors = {};
    if (!f.name.trim()) next.name = 'Informe seu nome';
    if (!isValidPhone(f.phone)) next.phone = 'Informe um telefone com DDD (10 ou 11 dígitos)';
    const email = f.email.trim();
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) next.email = 'E-mail inválido';
    if (!email && original.email) next.email = 'O e-mail não pode ser removido, apenas alterado';
    if (f.age) {
      const age = Number(f.age);
      if (!Number.isInteger(age) || age < 0 || age > 120) next.age = 'Idade inválida';
    } else if (original.age != null) {
      next.age = 'A idade não pode ser removida, apenas alterada';
    }
    if (!f.accessibilityNeeds.length && original.accessibilityNeeds?.length) {
      next.accessibilityNeeds = 'Mantenha ao menos uma necessidade selecionada';
    }
    return next;
  }

  async function handleSave() {
    if (!form || !user) return;
    const next = validate(form, user);
    setErrors(next);
    setFormError(null);
    if (Object.keys(next).length) return;

    const changes = diff(user, form);
    if (!Object.keys(changes).length) {
      setEditing(false);
      return;
    }

    setSaving(true);
    try {
      const updated = await usersApi.updateMe(changes);
      await refreshUser(updated);
      setEditing(false);
      setSuccess('Perfil atualizado');
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fields as Errors);
        setFormError(e.message);
      } else {
        setFormError('Não foi possível salvar o perfil');
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView style={styles.container} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {editing && form ? (
          <View style={styles.section}>
            <ErrorMessage message={formError} />
            <TextField label="Nome" value={form.name} onChangeText={(v) => set('name', v)} autoComplete="name" error={errors.name} />
            <TextField
              label="Telefone"
              value={formatPhone(form.phone)}
              onChangeText={(v) => set('phone', onlyDigits(v))}
              keyboardType="phone-pad"
              autoComplete="tel"
              error={errors.phone}
            />
            <TextField
              label="E-mail (opcional)"
              value={form.email}
              onChangeText={(v) => set('email', v)}
              keyboardType="email-address"
              autoCapitalize="none"
              autoComplete="email"
              error={errors.email}
            />
            <TextField
              label="Idade (opcional)"
              value={form.age}
              onChangeText={(v) => set('age', v.replace(/\D/g, '').slice(0, 3))}
              keyboardType="number-pad"
              error={errors.age}
            />
            <NeedsSelector
              value={form.accessibilityNeeds}
              onChange={(v) => set('accessibilityNeeds', v)}
              error={errors.accessibilityNeeds}
            />
            <View style={styles.actions}>
              <Button title="Salvar" onPress={handleSave} loading={saving} />
              <Button title="Cancelar" variant="secondary" onPress={() => setEditing(false)} disabled={saving} />
            </View>
          </View>
        ) : (
          <>
            {!!success && (
              <Text style={styles.success} accessibilityLiveRegion="polite">
                {success}
              </Text>
            )}
            <View style={styles.section}>
              <Text style={styles.name} accessibilityRole="header">{user.name}</Text>
              <InfoRow label="Telefone" value={formatPhone(user.phone) || 'Não informado'} />
              <InfoRow label="E-mail" value={user.email || 'Não informado'} />
              <InfoRow label="Idade" value={user.age != null ? `${user.age} anos` : 'Não informada'} />

              <Text style={styles.infoLabel}>Necessidades de acessibilidade</Text>
              {user.accessibilityNeeds?.length ? (
                <View style={styles.chips}>
                  {user.accessibilityNeeds.map((need) => (
                    <View key={need} style={styles.chip}>
                      <Text style={styles.chipText}>{NEED_LABELS[need]}</Text>
                    </View>
                  ))}
                </View>
              ) : (
                <Text style={styles.infoValue}>Nenhuma informada</Text>
              )}
            </View>
            <View style={styles.actions}>
              <Button title="Editar perfil" onPress={startEditing} />
              <Button title="Sair da conta" variant="secondary" onPress={signOut} />
            </View>
          </>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.infoRow} accessible accessibilityLabel={`${label}: ${value}`}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  );
}

/** PATCH só com o que mudou (campos vazios que já eram vazios não são enviados). */
function diff(user: User, form: Form): UserUpdateRequest {
  const changes: UserUpdateRequest = {};
  const name = form.name.trim();
  const email = form.email.trim();
  if (name !== user.name) changes.name = name;
  if (form.phone !== (user.phone ?? '')) changes.phone = form.phone;
  if (email && email !== user.email) changes.email = email;
  if (form.age && Number(form.age) !== user.age) changes.age = Number(form.age);
  const before = [...(user.accessibilityNeeds ?? [])].sort().join();
  const after = [...form.accessibilityNeeds].sort().join();
  if (form.accessibilityNeeds.length && before !== after) changes.accessibilityNeeds = form.accessibilityNeeds;
  return changes;
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md },
  section: { backgroundColor: colors.surface, borderRadius: 8, padding: spacing.md, marginBottom: spacing.md },
  name: { fontSize: 22, fontWeight: 'bold', color: colors.text, marginBottom: spacing.md },
  infoRow: { marginBottom: spacing.md },
  infoLabel: { fontSize: 13, fontWeight: '600', color: colors.textMuted, marginBottom: 2 },
  infoValue: { fontSize: 16, color: colors.text },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm, marginTop: spacing.xs },
  chip: { backgroundColor: '#E6F0FF', borderRadius: 12, paddingHorizontal: spacing.sm + 4, paddingVertical: spacing.xs },
  chipText: { color: colors.primary, fontWeight: '600', fontSize: 13 },
  actions: { gap: spacing.sm },
  success: {
    color: colors.success,
    backgroundColor: '#E8F5E9',
    borderRadius: 8,
    padding: spacing.sm + 4,
    marginBottom: spacing.md,
  },
});
