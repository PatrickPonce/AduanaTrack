import type { ChipProps } from '@mui/material'

import type { EstadoDespacho } from '../types'
import { ChipSuave, type TonoChip } from './ChipSuave'

/**
 * Estado → texto y color. Sprint 1: BORRADOR (gris) y APERTURA (azul); los estados de los
 * Sprints 2 y 3 se agregan aquí sin tocar las pantallas.
 */
const ESTADOS: Record<string, { texto: string; tono: TonoChip }> = {
  BORRADOR: { texto: 'Borrador', tono: 'gris' },
  APERTURA: { texto: 'Apertura', tono: 'azul' },
}

export function EstadoDespachoChip({ estado, size = 'small' }: { estado: EstadoDespacho; size?: ChipProps['size'] }) {
  const config = ESTADOS[estado] ?? { texto: estado, tono: 'gris' as const }
  return <ChipSuave label={config.texto} tono={config.tono} size={size} />
}
