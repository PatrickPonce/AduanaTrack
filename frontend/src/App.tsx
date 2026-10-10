import { Box, CircularProgress, Typography } from '@mui/material'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'

import { ProtectedRoute } from './auth/ProtectedRoute'
import { Layout } from './components/Layout'

// Cada pantalla se descarga al visitarla por primera vez.
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })))

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
            <Route
              path="/dashboard"
              element={
                <Typography variant="h5" component="h1">
                  Panel de seguimiento de despachos
                </Typography>
              }
            />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  )
}
