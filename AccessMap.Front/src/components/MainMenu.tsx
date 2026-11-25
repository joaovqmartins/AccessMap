import { router, type Href } from 'expo-router';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '../auth/AuthContext';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { confirm } from '../utils/dialog';

type Props = {
  onClose: () => void;
};

type Item = { label: string; href: Href };

/** Menu do ☰ da Home: painel à esquerda sobre um fundo escurecido que fecha ao tocar. */
export default function MainMenu({ onClose }: Props) {
  const { user, signOut } = useAuth();
  // No iOS o SafeAreaView dentro de um Modal recebe margem zero e o topo do menu ficava em cima
  // do relógio e da bateria. As margens do app (contexto do SafeAreaProvider) valem aqui também.
  const insets = useSafeAreaInsets();

  const items: Item[] = [
    { label: 'Meu perfil', href: '/perfil' },
    { label: 'Minhas avaliações', href: '/minhas-avaliacoes' },
    ...(user?.role === 'ADMIN' ? [{ label: 'Moderação', href: '/moderacao' as Href }] : []),
  ];

  function go(href: Href) {
    onClose();
    router.push(href);
  }

  async function handleSignOut() {
    if (await confirm('Sair da conta', 'Você precisará entrar de novo com telefone e senha.', 'Sair')) {
      onClose();
      await signOut();
    }
  }

  return (
    <Modal visible transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View
          style={[styles.panel, { paddingTop: insets.top, paddingBottom: insets.bottom, paddingLeft: spacing.md + insets.left }]}
          // Caixa de diálogo modal com nome: sem o papel, o aria-modal da web fica inválido (axe)
          role="dialog"
          aria-label="Menu"
          accessibilityViewIsModal
        >
          <View style={styles.header}>
            <View style={styles.headerText}>
              <Text style={styles.title} accessibilityRole="header">Menu</Text>
              {!!user?.name && <Text style={styles.subtitle} numberOfLines={1}>{user.name}</Text>}
            </View>
            <Pressable
              onPress={onClose}
              accessibilityRole="button"
              accessibilityLabel="Fechar menu"
              style={styles.close}
              hitSlop={8}
            >
              <Text style={styles.closeText}>✕</Text>
            </Pressable>
          </View>

          <MenuItem label="Início" onPress={onClose} />
          {items.map((item) => (
            <MenuItem key={item.label} label={item.label} onPress={() => go(item.href)} />
          ))}
          <View style={styles.separator} />
          <MenuItem label="Sair da conta" onPress={handleSignOut} danger />
        </View>

        {/* Fundo: tocar fora do painel fecha o menu */}
        <Pressable
          style={styles.backdrop}
          onPress={onClose}
          accessibilityRole="button"
          accessibilityLabel="Fechar menu"
        />
      </View>
    </Modal>
  );
}

function MenuItem({ label, onPress, danger }: { label: string; onPress: () => void; danger?: boolean }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [styles.item, pressed && styles.itemPressed]}
    >
      <Text style={[styles.itemText, danger && styles.itemDanger]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  overlay: { flex: 1, flexDirection: 'row' },
  panel: {
    width: '80%',
    maxWidth: 320,
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.md,
  },
  backdrop: { flex: 1, backgroundColor: colors.overlay },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    marginBottom: spacing.sm,
  },
  headerText: { flex: 1 },
  title: { fontSize: 20, fontWeight: '700', color: colors.text },
  subtitle: { fontSize: 14, color: colors.textMuted, marginTop: 2 },
  close: { minWidth: MIN_TOUCH, minHeight: MIN_TOUCH, alignItems: 'center', justifyContent: 'center' },
  closeText: { fontSize: 20, color: colors.text },
  item: { minHeight: MIN_TOUCH + 4, justifyContent: 'center', paddingHorizontal: spacing.sm, borderRadius: 8 },
  itemPressed: { backgroundColor: colors.background },
  itemText: { fontSize: 17, color: colors.text },
  itemDanger: { color: colors.danger, fontWeight: '600' },
  separator: { height: 1, backgroundColor: colors.border, marginVertical: spacing.sm },
});
