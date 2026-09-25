import React from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView } from 'react-native';

export default function HomeScreen() {
  return (
    <SafeAreaView style={styles.container}>
      
      {/* 1. CABEÇALHO: Menu, Foto do Utilizador e Busca */}
      <View style={styles.header}>
        <View style={styles.topRow}>
          {/* Menu Hamburger */}
          <TouchableOpacity style={styles.iconButton}>
            <Text style={styles.iconText}>☰</Text>
          </TouchableOpacity>
          
          {/* Imagem do Utilizador (Placeholder com iniciais) */}
          <View style={styles.userAvatar}>
            <Text style={styles.avatarText}>GR</Text>
          </View>
        </View>

        {/* Barra de Busca */}
        <TextInput
          style={styles.searchInput}
          placeholder="Buscar locais ou restaurantes..."
          placeholderTextColor="#888"
        />
      </View>

      {/* 2. OPÇÕES PRÉ-DEFINIDAS (Filtros) */}
      <View style={styles.optionsContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          {['Mais Próximos', 'Abertos Agora', 'Bem Avaliados', 'Promoções'].map((opcao, index) => (
            <TouchableOpacity key={index} style={styles.optionPill}>
              <Text style={styles.optionText}>{opcao}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* 3. ÁREA DO MAPA (Centro) */}
      <View style={styles.mapContainer}>
        <Text style={styles.mapPlaceholderText}>[ COMPONENTE DE MAPA AQUI ]</Text>
      </View>

      {/* 4. BARRA DE NAVEGAÇÃO INFERIOR */}
      <View style={styles.bottomNavigation}>
        <TouchableOpacity style={styles.navButton}>
          <Text style={[styles.navText, styles.navTextActive]}>Home</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.navButton}>
          <Text style={styles.navText}>Restaurantes</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.navButton}>
          <Text style={styles.navText}>Review</Text>
        </TouchableOpacity>
      </View>

    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  
  // --- Estilos do Cabeçalho ---
  header: {
    padding: 16,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  iconButton: {
    padding: 8,
  },
  iconText: {
    fontSize: 24,
    color: '#333',
  },
  userAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007BFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
  searchInput: {
    backgroundColor: '#F0F0F0',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 16,
  },

  // --- Estilos das Opções (Filtros) ---
  optionsContainer: {
    paddingVertical: 12,
    backgroundColor: '#FFFFFF',
  },
  optionPill: {
    backgroundColor: '#E0E0E0',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    marginLeft: 16,
  },
  optionText: {
    color: '#333',
    fontWeight: '500',
  },

  // --- Estilos do Mapa ---
  mapContainer: {
    flex: 1, // O flex: 1 faz esta área expandir e ocupar todo o espaço restante
    backgroundColor: '#D1D5DB', // Cor de fundo simulando um mapa carregando
    justifyContent: 'center',
    alignItems: 'center',
  },
  mapPlaceholderText: {
    color: '#6B7280',
    fontWeight: 'bold',
    letterSpacing: 1,
  },

  // --- Estilos da Navegação Inferior ---
  bottomNavigation: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: '#FFFFFF',
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
  },
  navButton: {
    alignItems: 'center',
  },
  navText: {
    color: '#888',
    fontSize: 14,
    fontWeight: '600',
  },
  navTextActive: {
    color: '#007BFF', // Destaca a rota atual
  },
});