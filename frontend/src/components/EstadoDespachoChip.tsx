import { Chip, type ChipProps } from '@mui/material'

import type { EstadoDespacho } from '../types'

/**
 * Estado → texto y color. Sprint 1: BORRADOR (gris) y APERTURA (azul); los estados de los
 * Sprints 2 y 3 se agregan aquí sin tocar las pantallas.
 */
const ESTADOS: Record<string, { texto: string; color: ChipProps['color'] }> = {
  BORRADOR: { texto: 'Borrador', color: 'default' },
  APERTURA: { texto: 'Apertura', color: 'primary' },
  DOCUMENTACION_COMPLETA: { texto: 'Documentación completa', color: 'info' },
  CLASIFICADO: { texto: 'Clasificado', color: 'info' },
  LIQUIDADO: { texto: 'Liquidado', color: 'warning' },
  TRIBUTOS_PAGADOS: { texto: 'Tributos pagados', color: 'success' },
  DAM_NUMERADA: { texto: 'DAM numerada', color: 'success' },
  CANAL_ASIGNADO: { texto: 'Canal asignado', color: 'secondary' },
}

/** Texto del estado tal como se muestra en las tablas ("Apertura"). */
export function textoEstado(estado: EstadoDespacho): string {
  return ESTADOS[estado]?.texto ?? estado
}

export function EstadoDespachoChip({ estado, size = 'small' }: { estado: EstadoDespacho; size?: ChipProps['size'] }) {
  const config = ESTADOS[estado] ?? { texto: estado, color: 'default' as const }
  return <Chip label={config.texto} color={config.color} size={size} />
}
