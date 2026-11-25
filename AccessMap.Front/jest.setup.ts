// Mocks comuns a todos os testes do app (jest-expo + React Native Testing Library).

// expo-secure-store é nativo: nos testes vira um mapa em memória, zerado entre os testes.
jest.mock('expo-secure-store', () => {
  const store = new Map<string, string>();
  return {
    getItemAsync: jest.fn(async (key: string) => store.get(key) ?? null),
    setItemAsync: jest.fn(async (key: string, value: string) => {
      store.set(key, value);
    }),
    deleteItemAsync: jest.fn(async (key: string) => {
      store.delete(key);
    }),
    __reset: () => store.clear(),
  };
});

// Mock oficial da biblioteca: margens fixas, sem precisar do SafeAreaProvider em cada teste
jest.mock('react-native-safe-area-context', () => require('react-native-safe-area-context/jest/mock').default);

beforeEach(() => {
  (jest.requireMock('expo-secure-store') as { __reset: () => void }).__reset();
});
