import { Chip, type ChipProps } from '@mui/material'

import type { EstadoCartaPoder } from '../types'

const ESTADOS: Record<EstadoCartaPoder, { texto: string; color: ChipProps['color'] }> = {
  VIGENTE: { texto: 'Vigente', color: 'success' },
  POR_VENCER: { texto: 'Por vencer', color: 'warning' },
  VENCIDA: { texto: 'Vencida', color: 'error' },
  SIN_CARTA: { texto: 'Sin carta', color: 'default' },
}

export function CartaPoderChip({ estado }: { estado: EstadoCartaPoder }) {
  const config = ESTADOS[estado] ?? ESTADOS.SIN_CARTA
  return <Chip label={config.texto} color={config.color} size="small" variant="outlined" />
}
