import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined'
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined'
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined'
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import DirectionsBoatOutlinedIcon from '@mui/icons-material/DirectionsBoatOutlined'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import InventoryOutlinedIcon from '@mui/icons-material/InventoryOutlined'
import LogoutIcon from '@mui/icons-material/Logout'
import ManageSearchOutlinedIcon from '@mui/icons-material/ManageSearchOutlined'
import MenuIcon from '@mui/icons-material/Menu'
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined'
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import TrafficOutlinedIcon from '@mui/icons-material/TrafficOutlined'
import WarehouseOutlinedIcon from '@mui/icons-material/WarehouseOutlined'
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
import { useState, type ReactNode } from 'react'
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router'

import { useAuth } from '../auth/AuthContext'
import { COLORES, fadeIn } from '../theme'
import type { Rol } from '../types'
import { EncabezadoContext, type Encabezado } from './Encabezado'

interface ItemMenu {
  texto: string
  icono: ReactNode
  /** Sin ruta: módulo del Sprint 3, se muestra deshabilitado con la marca «Pronto». */
  a?: string
  prefijo?: string
  /** Solo para quien registra datos (administrador y agente). */
  soloRegistro?: boolean
}

interface SeccionMenu {
  titulo: string
  items: ItemMenu[]
}

const MENU: SeccionMenu[] = [
  {
    titulo: 'General',
    items: [
      { texto: 'Dashboard', icono: <DashboardOutlinedIcon />, a: '/dashboard', prefijo: '/dashboard' },
      {
        texto: 'Despachos',
        icono: <InventoryOutlinedIcon />,
        a: '/despachos/nuevo',
        prefijo: '/despachos',
        soloRegistro: true,
      },
      { texto: 'Clientes', icono: <BusinessOutlinedIcon />, a: '/clientes', prefijo: '/clientes' },
      { texto: 'Embarques', icono: <DirectionsBoatOutlinedIcon />, a: '/embarques', prefijo: '/embarques' },
    ],
  },
  {
    titulo: 'Operación aduanera',
    items: [
      { texto: 'Documentos', icono: <DescriptionOutlinedIcon />, a: '/documentos', prefijo: '/documentos' },
      { texto: 'Clasificación', icono: <CategoryOutlinedIcon />, a: '/clasificacion', prefijo: '/clasificacion' },
      { texto: 'Tributos', icono: <PaymentsOutlinedIcon />, a: '/tributos', prefijo: '/tributos' },
      { texto: 'DAM (Registro)', icono: <ReceiptLongOutlinedIcon />, a: '/dam', prefijo: '/dam' },
      { texto: 'Canal de Control', icono: <TrafficOutlinedIcon />, a: '/canal-control', prefijo: '/canal-control' },
    ],
  },
  {
    titulo: 'Próximamente',
    items: [
      { texto: 'Depósitos / Citas', icono: <WarehouseOutlinedIcon /> },
      { texto: 'Inspecciones', icono: <ManageSearchOutlinedIcon /> },
      { texto: 'Levante', icono: <FactCheckOutlinedIcon /> },
      { texto: 'Notificaciones', icono: <NotificationsNoneOutlinedIcon /> },
      { texto: 'Reportes', icono: <AssessmentOutlinedIcon /> },
    ],
  },
]

export const ROLES: Record<Rol, string> = {
  ADMIN: 'Administrador',
  AGENTE: 'Agente de Aduana',
  CLIENTE: 'Cliente / Importador',
}

const ANCHO_MENU = 248

function iniciales(nombre: string): string {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('')
}

/** Barra lateral por secciones, barra superior con migas y título, y el contenido de cada pantalla. */
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

  const estiloItem = {
    display: 'flex',
    alignItems: 'center',
    gap: 1.5,
    width: 'calc(100% - 16px)',
    mx: 1,
    my: 0.25,
    justifyContent: 'flex-start',
    px: 1.5,
    py: 1,
    borderRadius: 2,
    fontSize: 13.5,
    color: 'rgba(255,255,255,0.78)',
    position: 'relative',
    '& .icono': { display: 'grid', placeItems: 'center', '& svg': { fontSize: 20 } },
  } as const

  const menu = (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        color: '#fff',
        overflowY: 'auto',
        background: `linear-gradient(180deg, ${COLORES.marino} 0%, #0a2140 60%, #081a33 100%)`,
      }}
    >
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', px: 2.5, py: 2.5 }}>
        <Box
          aria-hidden
          sx={{
            width: 36,
            height: 36,
            borderRadius: 2,
            display: 'grid',
            placeItems: 'center',
            background: `linear-gradient(135deg, ${COLORES.verdeActivo}, ${COLORES.verde})`,
            boxShadow: '0 6px 16px rgba(52,196,124,0.35)',
            '& svg': { fontSize: 22, color: '#fff' },
          }}
        >
          <DirectionsBoatOutlinedIcon />
        </Box>
        <Box>
          <Typography sx={{ fontWeight: 800, fontSize: 16, letterSpacing: 0.6, lineHeight: 1.1 }}>
            ADUANATRACK
          </Typography>
          <Typography sx={{ fontSize: 11, color: 'rgba(255,255,255,0.55)', letterSpacing: 0.3 }}>
            Importaciones marítimas
          </Typography>
        </Box>
      </Stack>

      <Box component="nav" aria-label="Menú principal" sx={{ flexGrow: 1, pb: 2 }}>
        {MENU.map((seccion) => (
          <Box key={seccion.titulo} sx={{ mt: 1.5 }}>
            <Typography
              sx={{
                px: 2.75,
                pb: 0.75,
                fontSize: 10.5,
                fontWeight: 700,
                letterSpacing: 1.2,
                textTransform: 'uppercase',
                color: 'rgba(255,255,255,0.4)',
              }}
            >
              {seccion.titulo}
            </Typography>
            {seccion.items.map((item) => {
              const habilitado = item.a && (!item.soloRegistro || registra)
              const activo = Boolean(item.prefijo && ruta.startsWith(item.prefijo))
              const contenido = (
                <>
                  <Box component="span" aria-hidden className="icono">
                    {item.icono}
                  </Box>
                  <Box component="span" sx={{ flexGrow: 1, textAlign: 'left' }}>
                    {item.texto}
                  </Box>
                </>
              )
              if (!habilitado) {
                return (
                  <Box
                    key={item.texto}
                    aria-disabled
                    title={item.a ? 'Solo para administrador y agente' : 'Disponible en el Sprint 3'}
                    sx={{ ...estiloItem, opacity: 0.45, cursor: 'default' }}
                  >
                    {contenido}
                    {!item.a && (
                      <Box
                        component="span"
                        sx={{
                          fontSize: 9.5,
                          fontWeight: 700,
                          letterSpacing: 0.5,
                          px: 0.75,
                          py: 0.25,
                          borderRadius: 1,
                          bgcolor: 'rgba(255,255,255,0.12)',
                        }}
                      >
                        PRONTO
                      </Box>
                    )}
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
                    ...estiloItem,
                    transition: 'background-color .2s ease, color .2s ease, transform .2s ease',
                    ...(activo && {
                      color: '#fff',
                      fontWeight: 700,
                      background: 'linear-gradient(90deg, rgba(52,196,124,0.22), rgba(26,111,220,0.18))',
                      boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.08)',
                      '&::before': {
                        content: '""',
                        position: 'absolute',
                        left: -8,
                        top: 8,
                        bottom: 8,
                        width: 3,
                        borderRadius: 2,
                        bgcolor: COLORES.verdeActivo,
                      },
                      '& .icono': { color: COLORES.verdeActivo, '& svg': { fontSize: 20 } },
                    }),
                    '&:hover': {
                      bgcolor: activo ? undefined : 'rgba(255,255,255,0.07)',
                      color: '#fff',
                      transform: 'translateX(2px)',
                    },
                    '&.Mui-focusVisible': { outline: '2px solid #fff', outlineOffset: -2 },
                  }}
                >
                  {contenido}
                </ButtonBase>
              )
            })}
          </Box>
        ))}
      </Box>

      <Box sx={{ px: 2.75, py: 2, borderTop: '1px solid rgba(255,255,255,0.08)' }}>
        <Typography sx={{ fontSize: 11, color: 'rgba(255,255,255,0.45)' }}>
          v0.2.0 · Sprint 2 · Grupo 2 USMP
        </Typography>
      </Box>
    </Box>
  )

  return (
    <EncabezadoContext.Provider value={setEncabezado}>
      <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
        <Box sx={{ width: ANCHO_MENU, flexShrink: 0, display: { xs: 'none', md: 'block' } }}>
          <Box
            sx={{
              position: 'fixed',
              top: 0,
              bottom: 0,
              width: ANCHO_MENU,
              boxShadow: '4px 0 24px rgba(13,42,75,0.18)',
              zIndex: 2,
            }}
          >
            {menu}
          </Box>
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
              position: 'sticky',
              top: 0,
              zIndex: 1,
              alignItems: 'center',
              bgcolor: 'rgba(255,255,255,0.82)',
              backdropFilter: 'blur(10px)',
              borderBottom: 1,
              borderColor: 'divider',
              boxShadow: '0 1px 12px rgba(15,31,58,0.05)',
              px: { xs: 2, md: 3.5 },
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
              <Typography
                variant="caption"
                component="p"
                sx={{ color: 'text.secondary', letterSpacing: 0.3, textTransform: 'uppercase', fontSize: 11 }}
              >
                {encabezado.migas}
              </Typography>
              <Typography component="h1" sx={{ fontSize: { xs: 18, md: 21 }, fontWeight: 700, lineHeight: 1.3 }}>
                {encabezado.titulo}
              </Typography>
            </Box>
            {usuario && (
              <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'right' }}>
                  <Typography sx={{ fontSize: 13.5, fontWeight: 600, lineHeight: 1.2 }}>{usuario.nombre}</Typography>
                  <Typography sx={{ fontSize: 11.5, color: 'text.secondary' }}>
                    {ROLES[usuario.rol] ?? usuario.rol}
                  </Typography>
                </Box>
                <IconButton
                  aria-label="Cuenta de usuario"
                  aria-haspopup="menu"
                  onClick={(e) => setAnclaCuenta(e.currentTarget)}
                  sx={{ p: 0.25, border: `2px solid ${COLORES.verdeActivo}40` }}
                >
                  <Avatar
                    sx={{
                      width: 36,
                      height: 36,
                      fontSize: 13,
                      fontWeight: 700,
                      background: `linear-gradient(135deg, ${COLORES.verde}, ${COLORES.azul})`,
                    }}
                  >
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
          <Box component="main" sx={{ p: { xs: 2, md: 3.5 } }}>
            {/* key={ruta} retriggera la animación de entrada al navegar entre pantallas. */}
            <Box key={ruta} sx={{ animation: `${fadeIn} .3s ease` }}>
              <Outlet />
            </Box>
          </Box>
        </Box>
      </Box>
    </EncabezadoContext.Provider>
  )
}
