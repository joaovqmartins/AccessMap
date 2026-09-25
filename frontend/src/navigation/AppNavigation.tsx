// src/navigation/AppNavigation.tsx
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

// Importando as telas (passar as telas)
//import LoginScreen from '../screens/LoginScreen';
import HomeScreen from '../screens/HomeScreen'; 

// 1. Definindo as Rotas e Tipos (Extremamente importante no TypeScript)
// O "undefined" significa que a rota não exige que passemos nenhum parâmetro (como um ID) para acessá-la.
export type RootStackParamList = {
  Home: undefined; 
};

// 2. Criando o Stack Navigator com os tipos que acabamos de definir
const Stack = createNativeStackNavigator<RootStackParamList>();

export default function AppNavigation() {
  return (
    // NavigationContainer gerencia a árvore de navegação e o estado do app.
    // Ele deve envolver toda a estrutura de rotas.
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        {/* Cada Stack.Screen representa uma tela. O 'name' é a chave usada para navegar. */}
        <Stack.Screen 
          name="Home" 
          component={HomeScreen} 
          options={{ title: 'Página Inicial' }} 
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}