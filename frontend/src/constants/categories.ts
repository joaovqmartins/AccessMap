// Categorias de estabelecimento da busca por proximidade (#86). O backend não guarda categoria:
// ela vem do Google, então cada uma vira tipos da Tabela A da Places API (New), usados na Nearby Search.
// https://developers.google.com/maps/documentation/places/web-service/place-types

export interface PlaceCategory {
  key: string;
  label: string;
  types: readonly string[];
}

const CATEGORIES_WITH_TYPES: PlaceCategory[] = [
  { key: 'restaurantes', label: 'Restaurantes', types: ['restaurant', 'cafe'] },
  { key: 'escolas', label: 'Escolas', types: ['school', 'primary_school', 'secondary_school', 'university'] },
  { key: 'shoppings', label: 'Shoppings', types: ['shopping_mall'] },
  { key: 'hospitais', label: 'Hospitais', types: ['hospital'] },
  { key: 'lojas', label: 'Lojas', types: ['store', 'supermarket', 'department_store', 'clothing_store'] },
  { key: 'lazer', label: 'Lazer', types: ['park', 'amusement_park', 'tourist_attraction', 'museum'] },
  { key: 'cinemas', label: 'Cinemas', types: ['movie_theater'] },
  { key: 'academias', label: 'Academias', types: ['gym', 'fitness_center'] },
];

/**
 * "Mais próximos" junta os tipos de todas as categorias: sem tipo nenhum, o Google traria
 * pontos de ônibus, caixas eletrônicos e outros lugares que não interessam.
 */
export const ALL_NEARBY: PlaceCategory = {
  key: 'todos',
  label: 'Mais próximos',
  types: [...new Set(CATEGORIES_WITH_TYPES.flatMap((c) => c.types))],
};

/** Ordem dos chips na Home. */
export const PLACE_CATEGORIES: PlaceCategory[] = [ALL_NEARBY, ...CATEGORIES_WITH_TYPES];
