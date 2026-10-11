import { Chip, type ChipProps } from '@mui/material'

export type TonoChip = 'gris' | 'azul' | 'verde' | 'naranja' | 'rojo'

const TONOS: Record<TonoChip, { fondo: string; texto: string }> = {
  gris: { fondo: '#eef1f5', texto: '#475569' },
  azul: { fondo: '#e1edff', texto: '#1d4ed8' },
  verde: { fondo: '#dcf5e6', texto: '#15803d' },
  naranja: { fondo: '#fff1d6', texto: '#b45309' },
  rojo: { fondo: '#fde2e2', texto: '#b91c1c' },
}

/** Etiqueta de estado con fondo suave (estilo «píldora»). */
export function ChipSuave({ label, tono, size = 'small' }: { label: string; tono: TonoChip; size?: ChipProps['size'] }) {
  const { fondo, texto } = TONOS[tono]
  return <Chip label={label} size={size} sx={{ bgcolor: fondo, color: texto, borderRadius: 999 }} />
}
