// Versão web (expo start --web): Alert.alert do React Native não faz nada no navegador,
// então usamos as caixas nativas do browser.

/** Pede confirmação; resolve `true` se a pessoa confirmar. */
export async function confirm(title: string, message: string, _confirmLabel: string): Promise<boolean> {
  return window.confirm(`${title}\n\n${message}`);
}

/** Aviso com um único botão; resolve quando a pessoa fecha. */
export async function notify(title: string, message: string): Promise<void> {
  window.alert(`${title}\n\n${message}`);
}
