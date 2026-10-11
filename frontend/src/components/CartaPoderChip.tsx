import type { EstadoCartaPoder } from '../types'
import { ChipSuave, type TonoChip } from './ChipSuave'

const ESTADOS: Record<EstadoCartaPoder, { texto: string; tono: TonoChip }> = {
  VIGENTE: { texto: 'Vigente', tono: 'verde' },
  POR_VENCER: { texto: 'Por vencer', tono: 'naranja' },
  VENCIDA: { texto: 'Vencida', tono: 'rojo' },
  SIN_CARTA: { texto: 'Sin carta', tono: 'gris' },
}

export function CartaPoderChip({ estado }: { estado: EstadoCartaPoder }) {
  const config = ESTADOS[estado] ?? ESTADOS.SIN_CARTA
  return <ChipSuave label={config.texto} tono={config.tono} />
}
