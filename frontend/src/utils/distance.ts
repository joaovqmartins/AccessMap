type Point = { latitude: number; longitude: number };

const EARTH_RADIUS_M = 6_371_000;

/** Distância em linha reta (haversine), em metros. */
export function distanceInMeters(a: Point, b: Point) {
  const rad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = rad(b.latitude - a.latitude);
  const dLng = rad(b.longitude - a.longitude);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(h));
}

/** "menos de 10 m", "350 m", "1,2 km". */
export function formatDistance(meters: number) {
  // O Google devolve o mesmo ponto para vários locais de um endereço: "0 m" soaria como erro
  if (meters < 10) return 'menos de 10 m';
  if (meters < 1000) return `${Math.round(meters / 10) * 10} m`;
  return `${(meters / 1000).toFixed(1).replace('.', ',')} km`;
}
