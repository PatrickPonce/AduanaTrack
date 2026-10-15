import { Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

/** Título de sección con línea inferior, como "Datos generales del despacho" en los prototipos. */
export function TituloSeccion({ children, accion }: { children: ReactNode; accion?: ReactNode }) {
  return (
    <Stack
      direction="row"
      sx={{
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: 1,
        flexWrap: 'wrap',
        pb: 1.25,
        mb: 2,
        borderBottom: 1,
        borderColor: 'divider',
      }}
    >
      <Typography component="h2" sx={{ fontSize: 15, fontWeight: 700 }}>
        {children}
      </Typography>
      {accion}
    </Stack>
  )
}

/** Rejilla de formulario de dos columnas (una en pantallas angostas). */
export const DOS_COLUMNAS = {
  display: 'grid',
  columnGap: 2,
  rowGap: 2.5,
  gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
} as const
