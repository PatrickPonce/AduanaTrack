import { Box, CircularProgress } from '@mui/material'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'

import { ProtectedRoute } from './auth/ProtectedRoute'
import { ROLES_AGENCIA } from './auth/roles'
import { Layout } from './components/Layout'

// Cada pantalla se descarga al visitarla por primera vez.
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ClientesPage = lazy(() => import('./pages/ClientesPage').then((m) => ({ default: m.ClientesPage })))
const ClienteFormPage = lazy(() => import('./pages/ClienteFormPage').then((m) => ({ default: m.ClienteFormPage })))
const DespachoFormPage = lazy(() => import('./pages/DespachoFormPage').then((m) => ({ default: m.DespachoFormPage })))
const DespachoDetallePage = lazy(() =>
  import('./pages/DespachoDetallePage').then((m) => ({ default: m.DespachoDetallePage })),
)
const EmbarquesPage = lazy(() => import('./pages/EmbarquesPage').then((m) => ({ default: m.EmbarquesPage })))
const EmbarquePage = lazy(() => import('./pages/EmbarquePage').then((m) => ({ default: m.EmbarquePage })))
const LevantePage = lazy(() => import('./pages/LevantePage').then((m) => ({ default: m.LevantePage })))
const ReportesPage = lazy(() => import('./pages/ReportesPage').then((m) => ({ default: m.ReportesPage })))
const ModuloDespachoPage = lazy(() =>
  import('./pages/ModuloDespachoPage').then((m) => ({ default: m.ModuloDespachoPage })),
)
const NotificacionesPage = lazy(() =>
  import('./pages/NotificacionesPage').then((m) => ({ default: m.NotificacionesPage })),
)
const MisDespachosPage = lazy(() => import('./pages/MisDespachosPage').then((m) => ({ default: m.MisDespachosPage })))
const UsuariosClientePage = lazy(() =>
  import('./pages/UsuariosClientePage').then((m) => ({ default: m.UsuariosClientePage })),
)

const cargando = (
  <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
    <CircularProgress aria-label="Cargando" />
  </Box>
)

/**
 * Rutas por rol (HU-16): quien abre a mano una pantalla que no es de su rol vuelve a su inicio
 * (el CLIENTE a /portal-cliente/mis-despachos). El backend filtra igual los datos: esto es solo UI.
 */
export function App() {
  return (
    <Suspense fallback={cargando}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            {/* Administrador y agente de aduana. */}
            <Route element={<ProtectedRoute roles={ROLES_AGENCIA} />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/clientes" element={<ClientesPage />} />
              <Route path="/clientes/nuevo" element={<ClienteFormPage />} />
              <Route path="/clientes/:id" element={<ClienteFormPage />} />
              <Route path="/despachos/nuevo" element={<DespachoFormPage />} />
              <Route path="/despachos/:id/editar" element={<DespachoFormPage />} />
              <Route path="/despachos/:id" element={<DespachoDetallePage />} />
              <Route path="/embarques" element={<EmbarquesPage />} />
              <Route path="/embarques/:despachoId" element={<EmbarquePage />} />
              <Route path="/documentos" element={<ModuloDespachoPage modulo="documentos" />} />
              <Route path="/clasificacion" element={<ModuloDespachoPage modulo="clasificacion" />} />
              <Route path="/tributos" element={<ModuloDespachoPage modulo="tributos" />} />
              <Route path="/dam" element={<ModuloDespachoPage modulo="dam" />} />
              <Route path="/canal-control" element={<ModuloDespachoPage modulo="canal" />} />
              <Route path="/citas" element={<ModuloDespachoPage modulo="citas" />} />
              <Route path="/inspecciones" element={<ModuloDespachoPage modulo="inspeccion" />} />
              <Route path="/levante" element={<ModuloDespachoPage modulo="levante" />} />
              <Route path="/levante/:despachoId" element={<LevantePage />} />
              <Route path="/reportes" element={<ReportesPage />} />
            </Route>

            {/* Solo el administrador. */}
            <Route element={<ProtectedRoute roles={['ADMIN']} />}>
              <Route path="/administracion/usuarios-cliente" element={<UsuariosClientePage />} />
            </Route>

            {/* Solo el cliente / importador (HU-16). */}
            <Route element={<ProtectedRoute roles={['CLIENTE']} />}>
              <Route path="/portal-cliente/mis-despachos" element={<MisDespachosPage />} />
            </Route>

            {/* Todos los roles. */}
            <Route path="/notificaciones" element={<NotificacionesPage />} />
          </Route>
        </Route>
        {/* /dashboard manda al login sin sesión y al portal al CLIENTE. */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  )
}
