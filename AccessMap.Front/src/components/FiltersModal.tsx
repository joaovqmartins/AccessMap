import { useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { AccessibilityNeed, AccessibilityTag } from '../api/types';
import { ALL_TAGS, NEED_LABELS, TAG_LABELS } from '../constants/labels';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { NO_FILTERS, type AccessibilityFilters } from '../hooks/useNearbyPlaces';
import Button from './Button';

type Props = {
  value: AccessibilityFilters;
  /** Necessidades do perfil; vazio esconde a opção e explica onde cadastrar. */
  needs: AccessibilityNeed[];
  onApply: (filters: AccessibilityFilters) => void;
  onClose: () => void;
};

/**
 * Escolha dos filtros de acessibilidade. Só aplica ao tocar em "Aplicar": cada mudança na busca
 * refaz a chamada ao backend, e marcar várias características uma a uma geraria uma chamada por toque.
 * Montado só enquanto está aberto, para o rascunho sempre começar do filtro em uso.
 */
export default function FiltersModal({ value, needs, onApply, onClose }: Props) {
  const [draft, setDraft] = useState(value);

  function toggleTag(tag: AccessibilityTag) {
    setDraft((d) => ({
      ...d,
      tags: d.tags.includes(tag) ? d.tags.filter((t) => t !== tag) : [...d.tags, tag],
    }));
  }

  return (
    <Modal visible animationType="slide" onRequestClose={onClose}>
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title} accessibilityRole="header">Filtros de acessibilidade</Text>
          <Pressable onPress={onClose} accessibilityRole="button" accessibilityLabel="Fechar sem aplicar"
            style={styles.close} hitSlop={8}>
            <Text style={styles.closeText}>✕</Text>
          </Pressable>
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <Text style={styles.sectionTitle} accessibilityRole="header">Minhas necessidades</Text>
          {needs.length > 0 ? (
            <CheckRow
              label="Usar minhas necessidades"
              detail={needs.map((n) => NEED_LABELS[n]).join(', ')}
              checked={draft.useMyNeeds}
              onPress={() => setDraft((d) => ({ ...d, useMyNeeds: !d.useMyNeeds }))}
            />
          ) : (
            <Text style={styles.hint}>
              Cadastre suas necessidades de acessibilidade no perfil para filtrar por elas.
            </Text>
          )}

          <Text style={styles.sectionTitle} accessibilityRole="header">Características</Text>
          <Text style={styles.hint}>Mostra os locais em que a maioria das avaliações diz que é adequado.</Text>
          {ALL_TAGS.map((tag) => (
            <CheckRow key={tag} label={TAG_LABELS[tag]} checked={draft.tags.includes(tag)} onPress={() => toggleTag(tag)} />
          ))}
        </ScrollView>

        <View style={styles.footer}>
          <View style={styles.footerButton}>
            <Button title="Limpar" variant="secondary" onPress={() => setDraft(NO_FILTERS)} />
          </View>
          <View style={styles.footerButton}>
            <Button title="Aplicar" onPress={() => onApply(draft)} />
          </View>
        </View>
      </SafeAreaView>
    </Modal>
  );
}

function CheckRow({ label, detail, checked, onPress }: {
  label: string;
  detail?: string;
  checked: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      accessibilityLabel={detail ? `${label}: ${detail}` : label}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}
    >
      <View style={[styles.box, checked && styles.boxChecked]}>
        {checked && <Text style={styles.check}>✓</Text>}
      </View>
      <View style={styles.rowText}>
        <Text style={styles.label}>{label}</Text>
        {!!detail && <Text style={styles.detail}>{detail}</Text>}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: spacing.md,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  title: { flex: 1, fontSize: 18, fontWeight: '700', color: colors.text },
  close: { width: MIN_TOUCH, height: MIN_TOUCH, alignItems: 'center', justifyContent: 'center' },
  closeText: { fontSize: 20, color: colors.textMuted },
  content: { padding: spacing.md },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: colors.text, marginTop: spacing.md, marginBottom: spacing.sm },
  hint: { fontSize: 14, color: colors.textMuted, marginBottom: spacing.sm },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: MIN_TOUCH,
    backgroundColor: colors.surface,
    borderRadius: 8,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginBottom: spacing.xs,
  },
  pressed: { opacity: 0.7 },
  box: {
    width: 24,
    height: 24,
    borderRadius: 4,
    borderWidth: 2,
    borderColor: colors.textMuted,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  boxChecked: { backgroundColor: colors.primary, borderColor: colors.primary },
  check: { color: colors.onPrimary, fontWeight: '700' },
  rowText: { flex: 1 },
  label: { fontSize: 16, color: colors.text },
  detail: { fontSize: 13, color: colors.textMuted, marginTop: 2 },
  footer: {
    flexDirection: 'row',
    gap: spacing.sm,
    padding: spacing.md,
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  footerButton: { flex: 1 },
});
