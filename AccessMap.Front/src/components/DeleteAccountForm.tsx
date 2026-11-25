import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { ApiError } from '../api/client';
import { usersApi } from '../api/users';
import { colors, spacing } from '../constants/theme';
import { confirm, notify } from '../utils/dialog';
import Button from './Button';
import ErrorMessage from './ErrorMessage';
import TextField from './TextField';

type Props = {
  onCancel: () => void;
  /** A conta e as sessões deixam de existir: quem usa precisa mandar para o login. */
  onDeleted: () => void;
};

export default function DeleteAccountForm({ onCancel, onDeleted }: Props) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  async function handleDelete() {
    setFormError(null);
    if (!currentPassword) {
      setPasswordError('Informe sua senha atual');
      return;
    }
    setPasswordError(null);

    const confirmed = await confirm(
      'Excluir minha conta',
      'Esta ação não pode ser desfeita. Suas avaliações continuam publicadas, mas sem nenhuma ligação com você.',
      'Excluir',
    );
    if (!confirmed) return;

    setDeleting(true);
    try {
      await usersApi.deleteMe(currentPassword);
      await notify('Conta excluída', 'Sua conta foi excluída.');
      onDeleted();
    } catch (e) {
      setFormError(e instanceof Error ? e.message : 'Não foi possível excluir a conta');
      if (e instanceof ApiError && e.fields.currentPassword) setPasswordError(e.fields.currentPassword);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <View>
      <Text style={styles.warning}>
        Seus dados pessoais serão apagados. As avaliações que você fez continuam publicadas, sem nenhuma ligação
        com você e sem as suas necessidades de acessibilidade.
      </Text>
      <ErrorMessage message={formError} />
      <TextField
        label="Senha atual"
        value={currentPassword}
        onChangeText={setCurrentPassword}
        secureTextEntry
        autoComplete="current-password"
        textContentType="password"
        error={passwordError ?? undefined}
        onSubmitEditing={handleDelete}
      />
      <View style={styles.actions}>
        <Button
          title="Excluir minha conta"
          variant="danger"
          onPress={handleDelete}
          loading={deleting}
          accessibilityHint="Pede confirmação antes de excluir"
        />
        <Button title="Cancelar" variant="secondary" onPress={onCancel} disabled={deleting} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  warning: { fontSize: 15, color: colors.text, marginBottom: spacing.md },
  actions: { gap: spacing.sm },
});
