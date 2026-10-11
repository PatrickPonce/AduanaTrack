import DirectionsBoat from '@mui/icons-material/DirectionsBoat'
import { Alert, Box, Button, Checkbox, Chip, FormControlLabel, Paper, Stack, TextField, Typography } from '@mui/material'
import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'

import { leerError } from '../api/http'
import { useAuth } from '../auth/AuthContext'
import { COLORES } from '../tema'
import { HISTORIAS } from '../utils/historias'

export const MENSAJE_CREDENCIALES = 'Usuario o contraseña incorrectos'
export const MENSAJE_BLOQUEO = 'Cuenta bloqueada temporalmente. Intente nuevamente en 15 minutos.'

/** HU-01 · T-01.3: inicio de sesión con «Recordarme» (sesión de 8 h). */
export function LoginPage() {
  const { usuario: sesion, iniciarSesion } = useAuth()
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')
  const [recordarme, setRecordarme] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [intentado, setIntentado] = useState(false)

  if (sesion) return <Navigate to="/dashboard" replace />

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setIntentado(true)
    if (!usuario.trim() || !password) return
    setEnviando(true)
    setError(null)
    try {
      await iniciarSesion(usuario.trim(), password, recordarme)
      const desde = (ubicacion.state as { desde?: string } | null)?.desde
      navegar(desde && desde !== '/login' ? desde : '/dashboard', { replace: true })
    } catch (e) {
      const { status } = leerError(e)
      if (status === 423) setError(MENSAJE_BLOQUEO)
      else if (status === 401 || status === 400) setError(MENSAJE_CREDENCIALES)
      else setError('No se pudo conectar con el servidor. Intente nuevamente.')
      setPassword('')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        p: 2,
        background: `linear-gradient(135deg, ${COLORES.marino} 0%, #123a63 55%, #164a7a 100%)`,
      }}
    >
      <Paper component="form" onSubmit={enviar} noValidate sx={{ p: { xs: 3, sm: 4 }, width: '100%', maxWidth: 400, borderRadius: 3 }}>
        <Stack spacing={2.25}>
          <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
            <Box
              sx={{
                width: 36,
                height: 36,
                borderRadius: 1.5,
                bgcolor: COLORES.acento,
                color: '#fff',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <DirectionsBoat fontSize="small" />
            </Box>
            <Typography sx={{ fontWeight: 800, letterSpacing: 0.6, color: COLORES.marino }}>ADUANATRACK</Typography>
          </Stack>

          <Box>
            <Chip
              label="HU-01"
              size="small"
              sx={{ bgcolor: '#dcf5e6', color: '#15803d', fontWeight: 700, borderRadius: 1, mb: 1 }}
            />
            <Typography variant="h1" sx={{ fontSize: '1.3rem' }}>
              {HISTORIAS['HU-01']}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Sistema de Seguimiento Aduanero de Importaciones Marítimas
            </Typography>
          </Box>

          {error && (
            <Alert severity={error === MENSAJE_BLOQUEO ? 'warning' : 'error'} role="alert">
              {error}
            </Alert>
          )}

          <TextField
            label="Usuario / Correo corporativo"
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            required
            autoFocus
            autoComplete="username"
            placeholder="usuario o correo@empresa.pe"
            error={intentado && !usuario.trim()}
            helperText={intentado && !usuario.trim() ? 'Ingrese su usuario o correo corporativo' : undefined}
          />
          <TextField
            label="Contraseña"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
            error={intentado && !password}
            helperText={intentado && !password ? 'Ingrese su contraseña' : undefined}
          />
          <FormControlLabel
            control={<Checkbox size="small" checked={recordarme} onChange={(e) => setRecordarme(e.target.checked)} />}
            label="Recordarme"
            slotProps={{ typography: { variant: 'body2' } }}
          />
          <Button type="submit" variant="contained" size="large" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar al sistema'}
          </Button>
          <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>
            Acceso restringido · Agencia de Aduanas · v0.1.0
          </Typography>
        </Stack>
      </Paper>
    </Box>
  )
}
