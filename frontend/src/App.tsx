import { Box, CircularProgress } from '@mui/material'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'

import { ProtectedRoute } from './auth/ProtectedRoute'
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
const ModuloDespachoPage = lazy(() =>
  import('./pages/ModuloDespachoPage').then((m) => ({ default: m.ModuloDespachoPage })),
)

const cargando = (
  <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
    <CircularProgress aria-label="Cargando" />
  </Box>
)

export function App() {
  return (
    <Suspense fallback={cargando}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
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
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  )
}
