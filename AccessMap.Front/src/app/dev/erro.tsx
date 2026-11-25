import { Redirect } from 'expo-router';
import { Text } from 'react-native';

// O e2e grava esta chave no sessionStorage para "consertar" a tela e conferir que o "Tentar de novo"
// realmente re-renderiza a rota. Não dá para usar um contador: o React repete a renderização sozinho
// depois de um erro, antes de mostrar o ErrorBoundary, e gastaria a falha.
const FIXED_KEY = 'accessmap.devErroConsertado';

function isFixed() {
  return typeof sessionStorage !== 'undefined' && sessionStorage.getItem(FIXED_KEY) === '1';
}

/**
 * Rota só de desenvolvimento para o e2e da tela de erro (#133). Em build de produção redireciona
 * para o início, então não fica nada quebrável exposto.
 */
export default function ErroDeTeste() {
  if (!__DEV__) return <Redirect href="/" />;
  if (!isFixed()) {
    throw new Error('Erro de teste da tela de erro');
  }
  return <Text>Tela recuperada</Text>;
}
