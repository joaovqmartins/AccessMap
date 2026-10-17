import * as Location from 'expo-location';
import { useCallback, useEffect, useState } from 'react';

export interface Coords {
  latitude: number;
  longitude: number;
}

export type LocationState =
  | { status: 'checking' }
  /** Ainda não perguntamos: a tela explica o motivo antes de abrir o pedido do sistema. */
  | { status: 'undetermined' }
  | { status: 'locating' }
  | { status: 'ready'; coords: Coords }
  /** `canAskAgain` falso: o sistema não mostra mais o pedido, só dá para liberar nas configurações. */
  | { status: 'denied'; canAskAgain: boolean }
  | { status: 'unavailable'; message: string };

// Uma posição de alguns minutos atrás basta para buscar locais por perto e sai na hora
const LAST_KNOWN_MAX_AGE_MS = 5 * 60 * 1000;
const TIMEOUT_MS = 15_000;

/**
 * Localização atual do usuário para a busca por proximidade.
 * Se a permissão já foi dada, localiza sozinho; senão espera `request()`, chamado pela tela.
 */
export function useUserLocation() {
  const [state, setState] = useState<LocationState>({ status: 'checking' });

  const locate = useCallback(async () => {
    setState({ status: 'locating' });
    try {
      if (!(await Location.hasServicesEnabledAsync())) {
        setState({ status: 'unavailable', message: 'A localização do aparelho está desligada.' });
        return;
      }
      const position =
        (await Location.getLastKnownPositionAsync({ maxAge: LAST_KNOWN_MAX_AGE_MS })) ??
        (await withTimeout(Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced })));
      const { latitude, longitude } = position.coords;
      setState({ status: 'ready', coords: { latitude, longitude } });
    } catch {
      setState({ status: 'unavailable', message: 'Não foi possível obter sua localização.' });
    }
  }, []);

  const handle = useCallback(
    (permission: Location.LocationPermissionResponse) => {
      if (permission.granted) return locate();
      if (permission.status === Location.PermissionStatus.UNDETERMINED) {
        setState({ status: 'undetermined' });
      } else {
        setState({ status: 'denied', canAskAgain: permission.canAskAgain });
      }
    },
    [locate],
  );

  useEffect(() => {
    Location.getForegroundPermissionsAsync()
      .then(handle)
      .catch(() => setState({ status: 'unavailable', message: 'Localização não suportada neste aparelho.' }));
  }, [handle]);

  /** Abre o pedido de permissão do sistema (ou tenta localizar de novo, se já tiver permissão). */
  const request = useCallback(async () => {
    try {
      await handle(await Location.requestForegroundPermissionsAsync());
    } catch {
      setState({ status: 'unavailable', message: 'Localização não suportada neste aparelho.' });
    }
  }, [handle]);

  return { state, request };
}

// getCurrentPositionAsync não tem timeout próprio: com GPS sem sinal ficaria esperando para sempre
function withTimeout<T>(promise: Promise<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout')), TIMEOUT_MS);
    promise.then(
      (value) => {
        clearTimeout(timer);
        resolve(value);
      },
      (error) => {
        clearTimeout(timer);
        reject(error);
      },
    );
  });
}
