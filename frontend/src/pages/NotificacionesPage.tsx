import { Paper, Typography } from '@mui/material'

import { useEncabezado } from '../components/Encabezado'

/**
 * STUB TEMPORAL (Sprint 3): lo reemplaza el centro de notificaciones de Fabio (HU-14 · T-14.3).
 * Borrar este archivo al integrar su PR «Centro de notificaciones».
 */
export function NotificacionesPage() {
  useEncabezado('Gestión', 'Notificaciones')
  return (
    <Paper sx={{ p: { xs: 2.5, md: 3 } }}>
      <Typography component="h2" sx={{ fontSize: 18, fontWeight: 700 }}>
        Notificaciones
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        No tienes notificaciones por ahora.
      </Typography>
    </Paper>
  )
}
