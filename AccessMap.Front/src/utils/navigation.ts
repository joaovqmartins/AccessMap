import { router, type Href } from 'expo-router';

/**
 * Volta uma tela; sem histórico (link direto, recarga no navegador, chegada por `replace`), vai para `fallback`.
 * Sem isso o `router.back()` não faz nada e a pessoa fica presa na tela.
 */
export function goBackOr(fallback: Href) {
  if (router.canGoBack()) {
    router.back();
  } else {
    router.replace(fallback);
  }
}

/**
 * Só aceita caminho interno do app como destino depois do login: começa com uma barra, sem "//"
 * (que o navegador lê como outro domínio), sem esquema ("https://") e sem barra invertida.
 */
export function safeRedirect(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  if (!value.startsWith('/') || value.startsWith('//')) return null;
  if (value.includes('://') || value.includes('\\')) return null;
  return value;
}

// Destino guardado ao enviar login/cadastro e usado pelo _layout quando a sessão abre.
// Fica fora do estado do React porque a tela de login deixa de existir assim que o status vira 'signedIn'.
let pendingRedirect: string | null = null;

export function setPendingRedirect(path: string | null) {
  pendingRedirect = path;
}

export function takePendingRedirect(): string | null {
  const path = pendingRedirect;
  pendingRedirect = null;
  return path;
}

/** Caminho do formulário de avaliação, para levar quem entrou pelo "Entrar para avaliar" direto até ele. */
export function reviewFormPath(placeId: string, name?: string) {
  const base = `/locais/${encodeURIComponent(placeId)}/avaliar`;
  return name ? `${base}?name=${encodeURIComponent(name)}` : base;
}
