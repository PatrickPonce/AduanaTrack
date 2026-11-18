import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined'
import { Badge, IconButton, Tooltip } from '@mui/material'
import { useEffect, useState } from 'react'
import { Link as RouterLink, useLocation } from 'react-router'

import { cantidadNoLeidas } from '../api/notificaciones'

/** Campana del encabezado con el contador de no leídas (HU-14 · contrato C9). Se actualiza al cambiar de pantalla. */
export function CampanaNotificaciones() {
  const { pathname } = useLocation()
  const [cantidad, setCantidad] = useState(0)

  useEffect(() => {
    let activo = true
    cantidadNoLeidas()
      .then((n) => activo && setCantidad(n))
      .catch(() => activo && setCantidad(0))
    return () => {
      activo = false
    }
  }, [pathname])

  return (
    <Tooltip title="Notificaciones">
      <IconButton
        component={RouterLink}
        to="/notificaciones"
        aria-label={cantidad > 0 ? `Notificaciones (${cantidad} sin leer)` : 'Notificaciones'}
      >
        <Badge badgeContent={cantidad} color="error" max={99}>
          <NotificationsNoneOutlinedIcon />
        </Badge>
      </IconButton>
    </Tooltip>
  )
}
