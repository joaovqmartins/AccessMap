import Constants from 'expo-constants';
import { Platform } from 'react-native';

const API_PORT = 8080;

/**
 * Endereço da API, sem barra no fim. Em ordem:
 * 1. `EXPO_PUBLIC_API_URL`, se definido (override para apontar para outro servidor);
 * 2. no navegador, o mesmo host da página (`npm run web` → localhost);
 * 3. no celular, o host do Metro que serviu o app: é o IP do computador na rede,
 *    então o Expo Go acha o backend sem ninguém configurar IP (e sem quebrar quando o DHCP troca o IP);
 * 4. sem nada disso, o emulador Android (10.0.2.2 é o computador visto de dentro dele).
 */
export function resolveApiBaseUrl(): string {
  const explicit = process.env.EXPO_PUBLIC_API_URL?.trim();
  if (explicit) return explicit.replace(/\/$/, '');

  if (Platform.OS === 'web' && typeof window !== 'undefined') {
    return `http://${window.location.hostname}:${API_PORT}`;
  }

  // hostUri vem como "192.168.1.16:8081"
  const metroHost = Constants.expoConfig?.hostUri?.split(':')[0];
  if (metroHost) return `http://${metroHost}:${API_PORT}`;

  return `http://10.0.2.2:${API_PORT}`;
}
