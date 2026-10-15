import { Box } from '@mui/material'

/** Tonos suaves de las etiquetas de los prototipos (Rojo, Naranja, Verde, Vigente…). */
export const TONOS = {
  rojo: { fondo: '#fde4e4', texto: '#b42318' },
  naranja: { fondo: '#ffedd5', texto: '#c2410c' },
  verde: { fondo: '#dcfce7', texto: '#15803d' },
  gris: { fondo: '#eef1f5', texto: '#64748b' },
} as const

export type Tono = keyof typeof TONOS

export function EtiquetaColor({ texto, tono }: { texto: string; tono: Tono }) {
  const { fondo, texto: color } = TONOS[tono]
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-block',
        px: 1.25,
        py: 0.25,
        borderRadius: 999,
        bgcolor: fondo,
        color,
        fontSize: 12,
        fontWeight: 700,
        lineHeight: 1.6,
        whiteSpace: 'nowrap',
      }}
    >
      {texto}
    </Box>
  )
}

const CANALES: Record<string, { texto: string; tono: Tono }> = {
  ROJO: { texto: 'Rojo', tono: 'rojo' },
  NARANJA: { texto: 'Naranja', tono: 'naranja' },
  VERDE: { texto: 'Verde', tono: 'verde' },
}

/** Canal de control (HU-10, Sprint 2); sin canal asignado se muestra "-". */
export function CanalChip({ canal }: { canal: string | null }) {
  const config = canal ? CANALES[canal.toUpperCase()] : undefined
  return config ? <EtiquetaColor {...config} /> : <EtiquetaColor texto="-" tono="gris" />
}
