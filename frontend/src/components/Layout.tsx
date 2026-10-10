import LogoutIcon from '@mui/icons-material/Logout'
import { AppBar, Box, Button, Container, Stack, Toolbar, Typography } from '@mui/material'
import { NavLink, Outlet, useNavigate } from 'react-router'

import { useAuth } from '../auth/AuthContext'

const MENU = [
  { a: '/dashboard', texto: 'Dashboard', exacto: true },
  { a: '/clientes', texto: 'Clientes', exacto: false },
  { a: '/despachos/nuevo', texto: 'Nuevo despacho', exacto: true },
]

const ROLES: Record<string, string> = { ADMIN: 'Administrador', AGENTE: 'Agente de aduana', CLIENTE: 'Cliente' }

export function Layout() {
  const { usuario, cerrarSesion } = useAuth()
  const navegar = useNavigate()

  async function salir() {
    await cerrarSesion()
    navegar('/login', { replace: true })
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky" elevation={1}>
        <Toolbar sx={{ gap: 2, flexWrap: 'wrap' }}>
          <Typography variant="h6" component="span" sx={{ fontWeight: 700, mr: 2 }}>
            AduanaTrack
          </Typography>
          <Stack component="nav" direction="row" spacing={1} sx={{ flexGrow: 1 }}>
            {MENU.map((item) => (
              <Button
                key={item.a}
                component={NavLink}
                to={item.a}
                end={item.exacto}
                color="inherit"
                sx={{ '&.active': { bgcolor: 'rgba(255,255,255,0.16)' } }}
              >
                {item.texto}
              </Button>
            ))}
          </Stack>
          {usuario && (
            <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
              <Box sx={{ textAlign: 'right', lineHeight: 1.2 }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {usuario.nombre}
                </Typography>
                <Typography variant="caption">{ROLES[usuario.rol] ?? usuario.rol}</Typography>
              </Box>
              <Button color="inherit" variant="outlined" startIcon={<LogoutIcon />} onClick={salir}>
                Cerrar sesión
              </Button>
            </Stack>
          )}
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
