import { Paper, Stack, Typography, type SxProps, type Theme } from '@mui/material'
import type { FormEventHandler, ReactNode } from 'react'

interface Props {
  /** Título de la sección, con una línea divisoria debajo. */
  titulo?: ReactNode
  /** Botones alineados a la derecha del título. */
  acciones?: ReactNode
  children: ReactNode
  component?: 'section' | 'form'
  onSubmit?: FormEventHandler<HTMLFormElement>
  noValidate?: boolean
  sx?: SxProps<Theme>
}

/** Contenedor blanco con borde suave que agrupa cada bloque de una pantalla. */
export function Tarjeta({ titulo, acciones, children, component = 'section', sx, ...resto }: Props) {
  return (
    <Paper variant="outlined" component={component} sx={[{ p: { xs: 2, md: 3 } }, ...(Array.isArray(sx) ? sx : [sx])]} {...resto}>
      {titulo && <Seccion titulo={titulo} acciones={acciones} nivel="h2" />}
      {children}
    </Paper>
  )
}

/** Subtítulo de sección con línea divisoria, como «Contenedores asociados». */
export function Seccion({ titulo, acciones, nivel = 'h3' }: { titulo: ReactNode; acciones?: ReactNode; nivel?: 'h2' | 'h3' }) {
  return (
    <Stack
      direction="row"
      sx={{
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 1,
        pb: 1.25,
        mb: 2.5,
        borderBottom: 1,
        borderColor: 'divider',
      }}
    >
      <Typography variant="subtitle1" component={nivel} sx={{ fontWeight: 700 }}>
        {titulo}
      </Typography>
      {acciones}
    </Stack>
  )
}

/** Rejilla de formulario: 1 columna en celular y 2 en pantallas medianas. */
export const rejilla = {
  display: 'grid',
  columnGap: 3,
  rowGap: 2.5,
  gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
} as const

/** Estilo de los campos de solo lectura (fondo gris claro). */
export const soloLectura = { '& .MuiInputBase-root': { bgcolor: '#f6f8fb' } } as const
