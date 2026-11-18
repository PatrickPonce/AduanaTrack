import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined'
import { Badge, IconButton, Tooltip } from '@mui/material'
import { useEffect, useState } from 'react'
import { Link as RouterLink, useLocation } from 'react-router'

import { cantidadNoLeidas, EVENTO_NOTIFICACIONES } from '../api/notificaciones'

/** Cada cuánto se vuelve a consultar el contador (contrato C9: 60 s). */
const INTERVALO_MS = 60_000

/**
 * Campana del encabezado con el contador de no leídas (HU-14 · contrato C9). Se actualiza al cambiar de
 * pantalla, cada 60 s y apenas otra pantalla avisa de un cambio (p. ej. al marcar una como leída).
 */
export function CampanaNotificaciones() {
  const { pathname } = useLocation()
  const [cantidad, setCantidad] = useState(0)

  useEffect(() => {
    let activo = true
    const consultar = () => {
      cantidadNoLeidas()
        .then((n) => activo && setCantidad(n))
        .catch(() => activo && setCantidad(0))
    }
    consultar()
    const temporizador = window.setInterval(consultar, INTERVALO_MS)
    window.addEventListener(EVENTO_NOTIFICACIONES, consultar)
    return () => {
      activo = false
      window.clearInterval(temporizador)
      window.removeEventListener(EVENTO_NOTIFICACIONES, consultar)
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
