import AssessmentOutlined from '@mui/icons-material/AssessmentOutlined'
import AttachFileOutlined from '@mui/icons-material/AttachFileOutlined'
import BusinessOutlined from '@mui/icons-material/BusinessOutlined'
import CategoryOutlined from '@mui/icons-material/CategoryOutlined'
import DescriptionOutlined from '@mui/icons-material/DescriptionOutlined'
import DirectionsBoat from '@mui/icons-material/DirectionsBoat'
import LogoutOutlined from '@mui/icons-material/LogoutOutlined'
import ManageSearchOutlined from '@mui/icons-material/ManageSearchOutlined'
import MenuIcon from '@mui/icons-material/Menu'
import NotificationsNoneOutlined from '@mui/icons-material/NotificationsNoneOutlined'
import PaidOutlined from '@mui/icons-material/PaidOutlined'
import PostAddOutlined from '@mui/icons-material/PostAddOutlined'
import SpaceDashboardOutlined from '@mui/icons-material/SpaceDashboardOutlined'
import TaskAltOutlined from '@mui/icons-material/TaskAltOutlined'
import TrafficOutlined from '@mui/icons-material/TrafficOutlined'
import WarehouseOutlined from '@mui/icons-material/WarehouseOutlined'
import {
  Avatar,
  Box,
  Chip,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useEffect, useState, type ReactElement } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'

import { useAuth } from '../auth/AuthContext'
import { COLORES } from '../tema'
import { HISTORIAS, PROXIMAS } from '../utils/historias'
import { EncabezadoProvider, useEncabezadoActual } from './Encabezado'

const ANCHO_MENU = 248

const MENU = [
  { a: '/dashboard', texto: 'Dashboard', exacto: true, icono: <SpaceDashboardOutlined /> },
  { a: '/despachos/nuevo', texto: 'Nuevo despacho', exacto: true, icono: <PostAddOutlined /> },
  { a: '/clientes', texto: 'Clientes', exacto: false, icono: <BusinessOutlined /> },
]

const ICONOS_PROXIMOS: Record<string, ReactElement> = {
  'HU-06': <AttachFileOutlined />,
  'HU-07': <CategoryOutlined />,
  'HU-08': <PaidOutlined />,
  'HU-09': <DescriptionOutlined />,
  'HU-10': <TrafficOutlined />,
  'HU-11': <WarehouseOutlined />,
  'HU-12': <ManageSearchOutlined />,
  'HU-13': <TaskAltOutlined />,
  'HU-14': <NotificationsNoneOutlined />,
  'HU-15': <AssessmentOutlined />,
}

const ROLES: Record<string, string> = { ADMIN: 'Administrador', AGENTE: 'Agente de aduana', CLIENTE: 'Cliente' }

function iniciales(nombre: string) {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('')
}

export function Layout() {
  return (
    <EncabezadoProvider>
      <Estructura />
    </EncabezadoProvider>
  )
}

function Estructura() {
  const theme = useTheme()
  const escritorio = useMediaQuery(theme.breakpoints.up('md'))
  const [menuAbierto, setMenuAbierto] = useState(false)
  const ubicacion = useLocation()

  // En celular, el menú se cierra al navegar.
  useEffect(() => setMenuAbierto(false), [ubicacion.pathname])

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <Drawer
        variant={escritorio ? 'permanent' : 'temporary'}
        open={escritorio || menuAbierto}
        onClose={() => setMenuAbierto(false)}
        sx={{ width: { md: ANCHO_MENU }, flexShrink: 0 }}
        slotProps={{ paper: { sx: { width: ANCHO_MENU, bgcolor: COLORES.marino, color: '#c7d2e0', border: 'none' } } }}
      >
        <MenuLateral />
      </Drawer>

      <Box sx={{ flexGrow: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <BarraSuperior onAbrirMenu={() => setMenuAbierto(true)} mostrarBotonMenu={!escritorio} />
        <Box component="main" sx={{ p: { xs: 2, md: 3.5 }, width: '100%', maxWidth: 1320, mx: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}

function MenuLateral() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Stack
        direction="row"
        spacing={1.25}
        sx={{ alignItems: 'center', px: 2.5, py: 2.25, borderBottom: '1px solid rgba(255,255,255,0.08)' }}
      >
        <Box
          sx={{
            width: 34,
            height: 34,
            borderRadius: 1.5,
            bgcolor: COLORES.acento,
            display: 'grid',
            placeItems: 'center',
            color: '#fff',
          }}
        >
          <DirectionsBoat fontSize="small" />
        </Box>
        <Typography sx={{ fontWeight: 800, letterSpacing: 0.6, color: '#fff' }}>ADUANATRACK</Typography>
      </Stack>

      <List component="nav" aria-label="Menú principal" sx={{ py: 1.5 }}>
        {MENU.map((item) => (
          <ListItemButton
            key={item.a}
            component={NavLink}
            to={item.a}
            end={item.exacto}
            sx={{
              color: 'inherit',
              py: 1.1,
              borderLeft: '3px solid transparent',
              '& .MuiListItemIcon-root': { color: 'inherit', minWidth: 36 },
              '&:hover': { bgcolor: 'rgba(255,255,255,0.06)' },
              '&.active': { bgcolor: COLORES.marinoClaro, color: '#fff', borderLeftColor: '#3ccf85' },
              '&.active .MuiListItemText-primary': { fontWeight: 700 },
            }}
          >
            <ListItemIcon>{item.icono}</ListItemIcon>
            <ListItemText primary={item.texto} slotProps={{ primary: { sx: { fontSize: '0.9rem' } } }} />
          </ListItemButton>
        ))}
      </List>

      <Typography
        variant="overline"
        sx={{ px: 2.75, pt: 1, color: 'rgba(199,210,224,0.55)', fontSize: '0.68rem', letterSpacing: 1 }}
      >
        Próximos sprints
      </Typography>
      <List dense aria-label="Módulos de próximos sprints" sx={{ pb: 2 }}>
        {PROXIMAS.map((m) => (
          <Tooltip key={m.hu} title={`${m.hu} · disponible en el Sprint ${m.sprint}`} placement="right">
            <ListItem aria-disabled="true" sx={{ py: 0.6, pl: 2.75, opacity: 0.5, cursor: 'default' }}>
              <ListItemIcon sx={{ color: 'inherit', minWidth: 36, '& svg': { fontSize: 20 } }}>
                {ICONOS_PROXIMOS[m.hu]}
              </ListItemIcon>
              <ListItemText primary={m.texto} slotProps={{ primary: { sx: { fontSize: '0.85rem' } } }} />
              <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>
                S{m.sprint}
              </Typography>
            </ListItem>
          </Tooltip>
        ))}
      </List>
    </Box>
  )
}

function BarraSuperior({ onAbrirMenu, mostrarBotonMenu }: { onAbrirMenu: () => void; mostrarBotonMenu: boolean }) {
  const { usuario, cerrarSesion } = useAuth()
  const encabezado = useEncabezadoActual()
  const navegar = useNavigate()

  async function salir() {
    await cerrarSesion()
    navegar('/login', { replace: true })
  }

  return (
    <Box
      component="header"
      sx={{
        position: 'sticky',
        top: 0,
        zIndex: (t) => t.zIndex.appBar,
        bgcolor: '#fff',
        borderBottom: 1,
        borderColor: 'divider',
        px: { xs: 2, md: 3.5 },
        py: 1.5,
        display: 'flex',
        alignItems: 'center',
        gap: 2,
      }}
    >
      {mostrarBotonMenu && (
        <IconButton edge="start" aria-label="Abrir menú" onClick={onAbrirMenu}>
          <MenuIcon />
        </IconButton>
      )}

      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        {encabezado && (
          <>
            <Typography variant="caption" color="text.secondary" component="p">
              {encabezado.migas.join(' / ')}
            </Typography>
            <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center', flexWrap: 'wrap', rowGap: 0.5 }}>
              <Chip
                label={encabezado.hu}
                size="small"
                sx={{ bgcolor: '#dcf5e6', color: '#15803d', fontWeight: 700, borderRadius: 1 }}
              />
              <Typography variant="h1" sx={{ fontSize: { xs: '1.1rem', md: '1.35rem' } }}>
                {HISTORIAS[encabezado.hu]}
              </Typography>
            </Stack>
          </>
        )}
      </Box>

      {usuario && (
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexShrink: 0 }}>
          <Typography variant="body2" color="text.secondary" sx={{ display: { xs: 'none', sm: 'block' } }}>
            <Box component="span" sx={{ color: 'text.primary', fontWeight: 600 }}>
              {usuario.nombre}
            </Box>{' '}
            · {ROLES[usuario.rol] ?? usuario.rol}
          </Typography>
          <Avatar sx={{ width: 34, height: 34, bgcolor: COLORES.acento, fontSize: '0.85rem', fontWeight: 700 }}>
            {iniciales(usuario.nombre)}
          </Avatar>
          <Tooltip title="Cerrar sesión">
            <IconButton aria-label="Cerrar sesión" onClick={salir} size="small">
              <LogoutOutlined fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      )}
    </Box>
  )
}
