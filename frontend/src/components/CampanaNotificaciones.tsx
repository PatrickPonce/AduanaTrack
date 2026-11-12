import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined'
import { IconButton, Tooltip } from '@mui/material'
import { Link as RouterLink } from 'react-router'

/**
 * STUB TEMPORAL (Sprint 3): lo reemplaza la campana de Fabio (HU-14 · T-14.3) con el contador de
 * no leídas del contrato C9. Borrar este archivo al integrar su PR «Centro de notificaciones».
 */
export function CampanaNotificaciones() {
  return (
    <Tooltip title="Notificaciones">
      <IconButton component={RouterLink} to="/notificaciones" aria-label="Notificaciones">
        <NotificationsNoneOutlinedIcon />
      </IconButton>
    </Tooltip>
  )
}
