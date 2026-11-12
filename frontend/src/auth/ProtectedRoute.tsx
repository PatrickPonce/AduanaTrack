import { Box, CircularProgress } from '@mui/material'
import { Navigate, Outlet, useLocation } from 'react-router'

import type { Rol } from '../types'
import { useAuth } from './AuthContext'
import { inicioDe } from './roles'

/**
 * Todas las rutas menos /login exigen sesión; sin ella se redirige al login. Con `roles`, quien
 * tiene otro rol vuelve a su pantalla de inicio (HU-16: el CLIENTE a «Mis despachos»).
 */
export function ProtectedRoute({ roles }: { roles?: Rol[] }) {
  const { usuario, cargando } = useAuth()
  const ubicacion = useLocation()

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}>
        <CircularProgress aria-label="Cargando sesión" />
      </Box>
    )
  }
  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />
  }
  if (roles && !roles.includes(usuario.rol)) {
    return <Navigate to={inicioDe(usuario.rol)} replace />
  }
  return <Outlet />
}
