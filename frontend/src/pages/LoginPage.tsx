import { Alert, Box, Button, Checkbox, FormControlLabel, Link, Paper, Stack, TextField, Typography } from '@mui/material'
import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'

import { leerError } from '../api/http'
import { useAuth } from '../auth/AuthContext'
import { COLORES, fadeInUp } from '../theme'

export const MENSAJE_CREDENCIALES = 'Usuario o contraseña incorrectos'
export const MENSAJE_BLOQUEO = 'Cuenta bloqueada temporalmente. Intente nuevamente en 15 minutos.'
const MENSAJE_OLVIDO = 'Solicite el restablecimiento de su contraseña al administrador del sistema.'
const VERSION = 'v0.1.0'

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
  const [aviso, setAviso] = useState<string | null>(null)
  const [intentado, setIntentado] = useState(false)

  useEffect(() => {
    document.title = 'Iniciar sesión · AduanaTrack'
  }, [])

  if (sesion) return <Navigate to="/dashboard" replace />

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setIntentado(true)
    setAviso(null)
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
        position: 'relative',
        overflow: 'hidden',
        background: `
          radial-gradient(900px circle at 85% 10%, rgba(46,139,87,0.25), transparent 55%),
          radial-gradient(700px circle at 10% 90%, rgba(26,111,220,0.25), transparent 55%),
          linear-gradient(160deg, ${COLORES.marino} 0%, #123a63 100%)`,
      }}
    >
      {/* Decoración: olas sutiles al pie de la pantalla, sin tocar logros o pruebas (aria-hidden). */}
      <Box
        aria-hidden
        sx={{
          position: 'absolute',
          inset: 0,
          opacity: 0.5,
          pointerEvents: 'none',
          backgroundImage:
            "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1200' height='120' viewBox='0 0 1200 120'%3E%3Cpath d='M0 60c150 40 350 40 500 0s350-40 500 0 150 40 200 0v60H0z' fill='%231a6fdc' fill-opacity='0.18'/%3E%3C/svg%3E\")",
          backgroundPosition: 'bottom',
          backgroundRepeat: 'repeat-x',
          backgroundSize: '1200px 120px',
        }}
      />
      <Paper
        component="form"
        onSubmit={enviar}
        noValidate
        sx={{
          position: 'relative',
          p: { xs: 3, sm: 3.5 },
          width: '100%',
          maxWidth: 380,
          border: 0,
          boxShadow: '0 20px 50px rgba(0,0,0,0.3)',
          animation: `${fadeInUp} .5s ease`,
        }}
      >
        <Stack spacing={2.25}>
          <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
            <Box
              aria-hidden
              sx={{
                width: 32,
                height: 32,
                borderRadius: 1.5,
                bgcolor: COLORES.verde,
                display: 'grid',
                placeItems: 'center',
                fontSize: 17,
              }}
            >
              🚢
            </Box>
            <Typography sx={{ fontWeight: 800, fontSize: 17, letterSpacing: 0.3 }}>ADUANATRACK</Typography>
          </Stack>

          <Box>
            <Typography component="h1" sx={{ fontSize: 19, fontWeight: 700 }}>
              Iniciar sesión
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ fontSize: 12.5 }}>
              Sistema de Seguimiento Aduanero de Importaciones Marítimas
            </Typography>
          </Box>

          {error && (
            <Alert severity={error === MENSAJE_BLOQUEO ? 'warning' : 'error'} role="alert">
              {error}
            </Alert>
          )}
          {aviso && <Alert severity="info">{aviso}</Alert>}

          <TextField
            label="Usuario / Correo corporativo"
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            required
            autoFocus
            autoComplete="username"
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
          <Stack
            direction="row"
            sx={{ justifyContent: 'space-between', alignItems: 'center', mt: '6px !important', gap: 1 }}
          >
            <FormControlLabel
              control={
                <Checkbox size="small" checked={recordarme} onChange={(e) => setRecordarme(e.target.checked)} />
              }
              label="Recordarme"
              slotProps={{ typography: { sx: { fontSize: 13, fontWeight: 600, color: '#475569' } } }}
            />
            <Link
              component="button"
              type="button"
              underline="hover"
              onClick={() => setAviso(MENSAJE_OLVIDO)}
              sx={{ fontSize: 12.5 }}
            >
              ¿Olvidó su contraseña?
            </Link>
          </Stack>
          <Button type="submit" variant="contained" size="large" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar al sistema'}
          </Button>
          <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>
            Acceso restringido · Agencia de Aduanas · {VERSION}
          </Typography>
        </Stack>
      </Paper>
    </Box>
  )
}
