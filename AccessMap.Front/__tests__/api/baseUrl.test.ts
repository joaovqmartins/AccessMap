// Cada caso carrega o módulo de novo, com o Platform, o expo-constants e a env do cenário.

type Scenario = { os: 'ios' | 'android' | 'web'; hostUri?: string; envUrl?: string };

function resolveWith({ os, hostUri, envUrl }: Scenario): string {
  let result = '';
  jest.isolateModules(() => {
    jest.doMock('expo-constants', () => ({ __esModule: true, default: { expoConfig: { hostUri } } }));
    const { Platform } = require('react-native');
    jest.replaceProperty(Platform, 'OS', os);
    if (envUrl === undefined) delete process.env.EXPO_PUBLIC_API_URL;
    else process.env.EXPO_PUBLIC_API_URL = envUrl;
    result = require('../../src/api/baseUrl').resolveApiBaseUrl();
  });
  return result;
}

const originalEnv = process.env.EXPO_PUBLIC_API_URL;

afterEach(() => {
  jest.restoreAllMocks();
  if (originalEnv === undefined) delete process.env.EXPO_PUBLIC_API_URL;
  else process.env.EXPO_PUBLIC_API_URL = originalEnv;
});

describe('resolveApiBaseUrl', () => {
  it('usa o EXPO_PUBLIC_API_URL quando definido, sem a barra do fim', () => {
    expect(resolveWith({ os: 'ios', hostUri: '192.168.1.16:8081', envUrl: 'https://api.exemplo.com/' })).toBe(
      'https://api.exemplo.com',
    );
  });

  it('no celular, usa o IP do Metro na porta 8080', () => {
    expect(resolveWith({ os: 'ios', hostUri: '192.168.1.16:8081' })).toBe('http://192.168.1.16:8080');
  });

  it('sem Metro nem env, cai no emulador Android', () => {
    expect(resolveWith({ os: 'android' })).toBe('http://10.0.2.2:8080');
  });

  it('ignora um EXPO_PUBLIC_API_URL só com espaços', () => {
    expect(resolveWith({ os: 'android', hostUri: '10.0.0.5:8081', envUrl: '   ' })).toBe('http://10.0.0.5:8080');
  });
});
