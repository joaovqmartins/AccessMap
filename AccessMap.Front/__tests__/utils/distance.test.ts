import type { PlaceSummary } from '../../src/api/types';
import { distanceInMeters, formatDistance } from '../../src/utils/distance';
import { adequateTagCount } from '../../src/utils/placeSummary';

describe('distanceInMeters', () => {
  it('é zero para o mesmo ponto', () => {
    const p = { latitude: -23.61, longitude: -46.66 };
    expect(distanceInMeters(p, p)).toBe(0);
  });

  it('calcula ~1,11 km para 0,01° de latitude', () => {
    const d = distanceInMeters({ latitude: 0, longitude: 0 }, { latitude: 0.01, longitude: 0 });
    expect(d).toBeGreaterThan(1100);
    expect(d).toBeLessThan(1125);
  });
});

describe('formatDistance', () => {
  it.each([
    [3, 'menos de 10 m'],
    [354, '350 m'],
    [994, '990 m'],
    [999, '1,0 km'],
    [1240, '1,2 km'],
  ])('%d m vira "%s"', (meters, expected) => {
    expect(formatDistance(meters)).toBe(expected);
  });
});

describe('adequateTagCount', () => {
  const stats = (adequado: number, inadequado: number, inexistente: number) => ({
    adequadoCount: adequado,
    inadequadoCount: inadequado,
    inexistenteCount: inexistente,
  });

  it('conta só as características com maioria estrita de ADEQUADO', () => {
    const summary = {
      tagStats: {
        RAMPAS_E_ENTRADAS: stats(3, 1, 0), // maioria adequada
        ELEVADORES: stats(1, 1, 0), // empate não conta
        SINALIZACAO: stats(2, 1, 1), // 2 contra 2 não é maioria
      },
    } as unknown as PlaceSummary;

    expect(adequateTagCount(summary)).toBe(1);
  });
});
