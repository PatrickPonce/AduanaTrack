import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import {
  Avatar,
  Box,
  ButtonBase,
  Drawer,
  IconButton,
  ListItemIcon,
  Menu,
  MenuItem,
  Stack,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router'

import { useAuth } from '../auth/AuthContext'
import { COLORES } from '../theme'
import type { Rol } from '../types'
import { EncabezadoContext, type Encabezado } from './Encabezado'

interface ItemMenu {
  texto: string
  icono: string
  /** Sin ruta: módulo de los Sprints 2 y 3, se muestra deshabilitado. */
  a?: string
  prefijo?: string
  /** Solo para quien registra datos (administrador y agente). */
  soloRegistro?: boolean
}

const MENU: ItemMenu[] = [
  { texto: 'Dashboard', icono: '🏠', a: '/dashboard', prefijo: '/dashboard' },
  { texto: 'Despachos', icono: '📋', a: '/despachos/nuevo', prefijo: '/despachos', soloRegistro: true },
  { texto: 'Clientes', icono: '🏢', a: '/clientes', prefijo: '/clientes' },
  { texto: 'Embarques', icono: '🚢', a: '/embarques', prefijo: '/embarques' },
  { texto: 'Documentos', icono: '📎' },
  { texto: 'Clasificación', icono: '🏷️' },
  { texto: 'Tributos', icono: '💰' },
  { texto: 'DAM (Registro)', icono: '📄' },
  { texto: 'Canal de Control', icono: '🚦' },
  { texto: 'Depósitos / Citas', icono: '🏭' },
  { texto: 'Inspecciones', icono: '🔍' },
  { texto: 'Levante', icono: '✅' },
  { texto: 'Notificaciones', icono: '🔔' },
  { texto: 'Reportes', icono: '📊' },
]

export const ROLES: Record<Rol, string> = {
  ADMIN: 'Administrador',
  AGENTE: 'Agente de Aduana',
  CLIENTE: 'Cliente / Importador',
}

const ANCHO_MENU = 224

function iniciales(nombre: string): string {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('')
}

/** Barra lateral, barra superior con migas y título, y el contenido de cada pantalla. */
export function Layout() {
  const { usuario, cerrarSesion } = useAuth()
  const navegar = useNavigate()
  const ruta = useLocation().pathname
  const [encabezado, setEncabezado] = useState<Encabezado>({ migas: '', titulo: '' })
  const [menuMovil, setMenuMovil] = useState(false)
  const [anclaCuenta, setAnclaCuenta] = useState<HTMLElement | null>(null)
  const registra = usuario?.rol !== 'CLIENTE'

  async function salir() {
    setAnclaCuenta(null)
    await cerrarSesion()
    navegar('/login', { replace: true })
  }

  const menu = (
    <Box sx={{ height: '100%', bgcolor: COLORES.marino, color: '#fff', overflowY: 'auto' }}>
      <Stack
        direction="row"
        spacing={1.25}
        sx={{ alignItems: 'center', px: 2, py: 2.25, borderBottom: '1px solid rgba(255,255,255,0.08)' }}
      >
        <Box
          aria-hidden
          sx={{
            width: 30,
            height: 30,
            borderRadius: 1.5,
            bgcolor: COLORES.verde,
            display: 'grid',
            placeItems: 'center',
            fontSize: 16,
          }}
        >
          🚢
        </Box>
        <Typography sx={{ fontWeight: 800, fontSize: 16, letterSpacing: 0.3 }}>ADUANATRACK</Typography>
      </Stack>
      <Box component="nav" aria-label="Menú principal" sx={{ py: 1 }}>
        {MENU.map((item) => {
          const habilitado = item.a && (!item.soloRegistro || registra)
          const activo = Boolean(item.prefijo && ruta.startsWith(item.prefijo))
          const estilo = {
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            width: '100%',
            justifyContent: 'flex-start',
            px: 2,
            py: 1.1,
            fontSize: 13.5,
            borderLeft: '3px solid transparent',
            color: 'rgba(255,255,255,0.86)',
          } as const
          const contenido = (
            <>
              <Box component="span" aria-hidden sx={{ width: 20, textAlign: 'center', fontSize: 14 }}>
                {item.icono}
              </Box>
              {item.texto}
            </>
          )
          if (!habilitado) {
            return (
              <Box
                key={item.texto}
                aria-disabled
                title="Disponible en los próximos sprints"
                sx={{ ...estilo, opacity: 0.5, cursor: 'default' }}
              >
                {contenido}
              </Box>
            )
          }
          return (
            <ButtonBase
              key={item.texto}
              component={RouterLink}
              to={item.a!}
              onClick={() => setMenuMovil(false)}
              aria-current={activo ? 'page' : undefined}
              sx={{
                ...estilo,
                ...(activo && {
                  bgcolor: COLORES.marinoActivo,
                  borderLeftColor: COLORES.verdeActivo,
                  color: '#fff',
                  fontWeight: 700,
                }),
                '&:hover': { bgcolor: activo ? COLORES.marinoActivo : 'rgba(255,255,255,0.06)' },
                '&.Mui-focusVisible': { outline: '2px solid #fff', outlineOffset: -2 },
              }}
            >
              {contenido}
            </ButtonBase>
          )
        })}
      </Box>
    </Box>
  )

  return (
    <EncabezadoContext.Provider value={setEncabezado}>
      <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
        <Box sx={{ width: ANCHO_MENU, flexShrink: 0, display: { xs: 'none', md: 'block' } }}>
          <Box sx={{ position: 'fixed', top: 0, bottom: 0, width: ANCHO_MENU }}>{menu}</Box>
        </Box>
        <Drawer
          open={menuMovil}
          onClose={() => setMenuMovil(false)}
          sx={{ display: { md: 'none' }, '& .MuiDrawer-paper': { width: ANCHO_MENU, border: 0, borderRadius: 0 } }}
        >
          {menu}
        </Drawer>

        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Stack
            component="header"
            direction="row"
            spacing={2}
            sx={{
              alignItems: 'center',
              bgcolor: '#fff',
              borderBottom: 1,
              borderColor: 'divider',
              px: { xs: 2, md: 3 },
              py: 1.5,
            }}
          >
            <IconButton
              aria-label="Abrir menú"
              onClick={() => setMenuMovil(true)}
              sx={{ display: { md: 'none' }, ml: -1 }}
            >
              <MenuIcon />
            </IconButton>
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography variant="caption" color="text.secondary" component="p">
                {encabezado.migas}
              </Typography>
              <Typography component="h1" sx={{ fontSize: { xs: 18, md: 20 }, fontWeight: 700, lineHeight: 1.3 }}>
                {encabezado.titulo}
              </Typography>
            </Box>
            {usuario && (
              <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' }, fontSize: 13 }}>
                  {usuario.nombre} · {ROLES[usuario.rol] ?? usuario.rol}
                </Typography>
                <IconButton
                  aria-label="Cuenta de usuario"
                  aria-haspopup="menu"
                  onClick={(e) => setAnclaCuenta(e.currentTarget)}
                  sx={{ p: 0 }}
                >
                  <Avatar sx={{ width: 34, height: 34, bgcolor: COLORES.verde, fontSize: 13, fontWeight: 700 }}>
                    {iniciales(usuario.nombre)}
                  </Avatar>
                </IconButton>
                <Menu
                  anchorEl={anclaCuenta}
                  open={Boolean(anclaCuenta)}
                  onClose={() => setAnclaCuenta(null)}
                  anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                  transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                >
                  <MenuItem onClick={() => void salir()}>
                    <ListItemIcon>
                      <LogoutIcon fontSize="small" />
                    </ListItemIcon>
                    Cerrar sesión
                  </MenuItem>
                </Menu>
              </Stack>
            )}
          </Stack>
          <Box component="main" sx={{ p: { xs: 2, md: 3 } }}>
            <Outlet />
          </Box>
        </Box>
      </Box>
    </EncabezadoContext.Provider>
  )
}
