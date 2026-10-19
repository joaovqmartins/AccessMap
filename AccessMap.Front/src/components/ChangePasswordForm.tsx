import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { ApiError } from '../api/client';
import { usersApi } from '../api/users';
import { spacing } from '../constants/theme';
import { notify } from '../utils/dialog';
import Button from './Button';
import ErrorMessage from './ErrorMessage';
import TextField from './TextField';

type Field = 'currentPassword' | 'password' | 'confirmation';

type Props = {
  onCancel: () => void;
  /** O backend encerra todas as sessões ao trocar a senha: quem usa precisa mandar para o login. */
  onChanged: () => void;
};

export default function ChangePasswordForm({ onCancel, onChanged }: Props) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Espelha o UserRequestDto (senha nova com no mínimo 8) e o UserService (senha atual obrigatória)
  function validate() {
    const next: Partial<Record<Field, string>> = {};
    if (!currentPassword) next.currentPassword = 'Informe sua senha atual';
    if (password.length < 8) next.password = 'A nova senha deve ter no mínimo 8 caracteres';
    else if (password === currentPassword) next.password = 'A nova senha deve ser diferente da atual';
    if (confirmation !== password) next.confirmation = 'As senhas não conferem';
    return next;
  }

  async function handleSave() {
    const next = validate();
    setErrors(next);
    setFormError(null);
    if (Object.keys(next).length) return;

    setSaving(true);
    try {
      await usersApi.updateMe({ currentPassword, password });
      await notify('Senha alterada', 'Por segurança, todas as sessões foram encerradas. Entre novamente com a nova senha.');
      onChanged();
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fields as Partial<Record<Field, string>>);
        setFormError(e.message);
      } else {
        setFormError('Não foi possível alterar a senha');
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <View>
      <ErrorMessage message={formError} />
      <TextField
        label="Senha atual"
        value={currentPassword}
        onChangeText={setCurrentPassword}
        secureTextEntry
        autoComplete="current-password"
        textContentType="password"
        error={errors.currentPassword}
      />
      <TextField
        label="Nova senha"
        value={password}
        onChangeText={setPassword}
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
        secureTextEntry
        autoComplete="new-password"
        textContentType="newPassword"
        error={errors.confirmation}
        onSubmitEditing={handleSave}
      />
      <View style={styles.actions}>
        <Button
          title="Alterar senha"
          onPress={handleSave}
          loading={saving}
          accessibilityHint="Você precisará entrar novamente depois de alterar"
        />
        <Button title="Cancelar" variant="secondary" onPress={onCancel} disabled={saving} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  actions: { gap: spacing.sm },
});
