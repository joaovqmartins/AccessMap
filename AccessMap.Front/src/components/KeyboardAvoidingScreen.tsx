import { useContext, type ReactNode } from 'react';
import { KeyboardAvoidingView, type StyleProp, type ViewStyle } from 'react-native';
import { HeaderHeightContext } from 'expo-router/react-navigation';

type Props = {
  style?: StyleProp<ViewStyle>;
  children: ReactNode;
};

/**
 * Conteúdo de tela com formulário: sobe acima do teclado.
 *
 * - `padding` nos dois sistemas: o app é edge-to-edge no Android, então a janela não encolhe
 *   sozinha com o teclado, e sem `behavior` o campo fica embaixo dele.
 * - O deslocamento é a altura do cabeçalho da pilha: o teclado é medido a partir do topo da
 *   tela, e este componente começa abaixo do cabeçalho. Fora de uma tela com cabeçalho (login,
 *   testes), vale 0.
 */
export default function KeyboardAvoidingScreen({ style, children }: Props) {
  const headerHeight = useContext(HeaderHeightContext) ?? 0;
  return (
    <KeyboardAvoidingView style={[{ flex: 1 }, style]} behavior="padding" keyboardVerticalOffset={headerHeight}>
      {children}
    </KeyboardAvoidingView>
  );
}
