import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined'
import TaskAltOutlinedIcon from '@mui/icons-material/TaskAltOutlined'
import TrafficOutlinedIcon from '@mui/icons-material/TrafficOutlined'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import {
  Alert,
  Box,
  Button,
  ButtonBase,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'

import { listarAduanas } from '../api/catalogos'
import { obtenerCliente } from '../api/clientes'
import { exportarExcel, listarDespachosTablero, obtenerIndicadores } from '../api/dashboard'
import { mensajeDeError } from '../api/http'
import { ClienteAutocomplete } from '../components/ClienteAutocomplete'
import { useEncabezado } from '../components/Encabezado'
import { textoEstado } from '../components/EstadoDespachoChip'
import { CanalChip } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import { COLORES, fadeInUp } from '../theme'
import type {
  Aduana,
  ClienteResumen,
  DespachoTablero,
  EstadoDespacho,
  FiltrosTablero,
  Indicadores,
} from '../types'
import { formatoFecha } from '../utils/formato'

const TARJETAS: { clave: keyof Indicadores; titulo: string; color: string; acento: string; icono: ReactNode }[] = [
  {
    clave: 'despachosActivos',
    titulo: 'Despachos activos',
    color: 'text.primary',
    acento: COLORES.azul,
    icono: <LocalShippingOutlinedIcon />,
  },
  {
    clave: 'enCanalNaranjaRojo',
    titulo: 'En canal naranja/rojo',
    color: 'warning.main',
    acento: '#d97706',
    icono: <TrafficOutlinedIcon />,
  },
  {
    clave: 'conLevante',
    titulo: 'Con levante registrado',
    color: 'success.main',
    acento: '#16a34a',
    icono: <TaskAltOutlinedIcon />,
  },
  {
    clave: 'conAlertasCriticas',
    titulo: 'Con alertas críticas',
    color: 'error.main',
    acento: '#dc2626',
    icono: <WarningAmberIcon />,
  },
]

/** Etapas del Sprint 2 y el módulo del menú donde se trabaja cada una. */
const ETAPAS: { estado: EstadoDespacho; a: string }[] = [
  { estado: 'APERTURA', a: '/documentos' },
  { estado: 'DOCUMENTACION_COMPLETA', a: '/clasificacion' },
  { estado: 'CLASIFICADO', a: '/tributos' },
  { estado: 'LIQUIDADO', a: '/tributos' },
  { estado: 'TRIBUTOS_PAGADOS', a: '/dam' },
  { estado: 'DAM_NUMERADA', a: '/canal-control' },
  { estado: 'CANAL_ASIGNADO', a: '/canal-control' },
]

const COLUMNAS = 7

function leerFiltros(params: URLSearchParams): FiltrosTablero {
  const filtros: FiltrosTablero = {}
  const aduanaId = Number(params.get('aduanaId'))
  const clienteId = Number(params.get('clienteId'))
  if (aduanaId > 0) filtros.aduanaId = aduanaId
  if (clienteId > 0) filtros.clienteId = clienteId
  return filtros
}

/** HU-02 · T-02.3 / T-02.4: panel de seguimiento con indicadores, filtros y tabla de despachos. */
export function DashboardPage() {
  useEncabezado('Dashboard', 'Panel de Seguimiento de Despachos')
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const [params, setParams] = useSearchParams()
  const filtros = useMemo(() => leerFiltros(params), [params])

  const [aduanas, setAduanas] = useState<Aduana[]>([])
  const [aduanaSel, setAduanaSel] = useState<number | ''>(filtros.aduanaId ?? '')
  const [clienteSel, setClienteSel] = useState<ClienteResumen | null>(null)

  const [indicadores, setIndicadores] = useState<Indicadores | null>(null)
  const [despachos, setDespachos] = useState<DespachoTablero[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [exportando, setExportando] = useState(false)

  useEffect(() => {
    listarAduanas()
      .then(setAduanas)
      .catch(() => setAduanas([]))
  }, [])

  // Al volver desde el detalle, los filtros de la URL se reflejan en los controles.
  useEffect(() => {
    setAduanaSel(filtros.aduanaId ?? '')
    if (!filtros.clienteId) {
      setClienteSel(null)
      return
    }
    let activo = true
    obtenerCliente(filtros.clienteId)
      .then((c) => activo && setClienteSel(c))
      .catch(() => activo && setClienteSel(null))
    return () => {
      activo = false
    }
  }, [filtros.aduanaId, filtros.clienteId])

  // CA4: al cambiar los filtros se recargan tarjetas y tabla.
  useEffect(() => {
    let activo = true
    setError(null)
    setDespachos(null)
    listarDespachosTablero(filtros)
      .then((d) => activo && setDespachos(d))
      .catch((e) => {
        if (activo) {
          setDespachos([])
          setError(mensajeDeError(e, 'No se pudo cargar la tabla de despachos.'))
        }
      })
    obtenerIndicadores(filtros)
      .then((i) => activo && setIndicadores(i))
      .catch(() => activo && setIndicadores(null))
    return () => {
      activo = false
    }
  }, [filtros])

  function aplicar() {
    const nuevos = new URLSearchParams()
    if (aduanaSel !== '') nuevos.set('aduanaId', String(aduanaSel))
    if (clienteSel) nuevos.set('clienteId', String(clienteSel.id))
    setParams(nuevos)
  }

  function limpiar() {
    setAduanaSel('')
    setClienteSel(null)
    setParams(new URLSearchParams())
  }

  function abrir(id: number) {
    navegar(`/despachos/${id}`, { state: { volverA: `/dashboard${ubicacion.search}` } })
  }

  async function exportar() {
    setExportando(true)
    try {
      await exportarExcel(filtros)
    } catch (e) {
      setError(mensajeDeError(e, 'No se pudo exportar a Excel.'))
    } finally {
      setExportando(false)
    }
  }

  return (
    <Stack spacing={2.5}>
      {/* Banner decorativo con la identidad marítima del sistema (aria-hidden, no afecta pruebas). */}
      <Box
        aria-hidden
        sx={{
          position: 'relative',
          overflow: 'hidden',
          borderRadius: 3,
          px: { xs: 2.5, md: 3.5 },
          py: { xs: 2.5, md: 3 },
          color: '#fff',
          display: { xs: 'none', sm: 'block' },
          background: `linear-gradient(120deg, ${COLORES.marino} 0%, #134574 55%, ${COLORES.azul} 100%)`,
          boxShadow: '0 12px 30px rgba(13,42,75,0.25)',
          animation: `${fadeInUp} .4s ease`,
          '&::after': {
            content: '""',
            position: 'absolute',
            inset: 0,
            backgroundImage:
              "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='80' viewBox='0 0 600 80'%3E%3Cpath d='M0 40c75 20 175 20 250 0s175-20 250 0 75 20 100 0v40H0z' fill='%23ffffff' fill-opacity='0.07'/%3E%3C/svg%3E\")",
            backgroundRepeat: 'repeat-x',
            backgroundPosition: 'bottom',
            backgroundSize: '600px 80px',
          },
          '&::before': {
            content: '""',
            position: 'absolute',
            width: 260,
            height: 260,
            right: -60,
            top: -120,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(52,196,124,0.35), transparent 70%)',
          },
        }}
      >
        <Typography sx={{ position: 'relative', fontSize: 12, letterSpacing: 1.2, opacity: 0.75, fontWeight: 600 }}>
          PANEL DE SEGUIMIENTO
        </Typography>
        <Typography sx={{ position: 'relative', fontSize: { sm: 20, md: 24 }, fontWeight: 800, mt: 0.5 }}>
          Seguimiento aduanero de importaciones marítimas
        </Typography>
        <Typography sx={{ position: 'relative', fontSize: 13.5, opacity: 0.8, mt: 0.5 }}>
          Del registro del despacho a la asignación del canal de control, en un solo lugar.
        </Typography>
      </Box>

      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', lg: 'repeat(4, 1fr)' } }}>
        {TARJETAS.map((t, i) => (
          <Paper
            key={t.clave}
            sx={{
              px: 2.5,
              py: 2.25,
              position: 'relative',
              overflow: 'hidden',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 1.5,
              animation: `${fadeInUp} .4s ease`,
              animationDelay: `${i * 0.06}s`,
              animationFillMode: 'backwards',
              '&::before': {
                content: '""',
                position: 'absolute',
                left: 0,
                top: 0,
                bottom: 0,
                width: 4,
                bgcolor: t.acento,
              },
              '&:hover': { boxShadow: '0 12px 28px rgba(15,31,58,0.12)', transform: 'translateY(-3px)' },
            }}
          >
            <Box>
              <Typography
                component="p"
                sx={{ fontSize: 30, fontWeight: 800, lineHeight: 1.15, color: t.color }}
                aria-label={t.titulo}
              >
                {indicadores ? indicadores[t.clave] : '—'}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ fontSize: 13 }}>
                {t.titulo}
              </Typography>
            </Box>
            <Box
              aria-hidden
              sx={{
                width: 46,
                height: 46,
                flexShrink: 0,
                borderRadius: 2.5,
                display: 'grid',
                placeItems: 'center',
                color: t.acento,
                bgcolor: `${t.acento}17`,
                '& svg': { fontSize: 24 },
              }}
            >
              {t.icono}
            </Box>
          </Paper>
        ))}
      </Box>

      <Paper sx={{ p: { xs: 2, md: 2.5 }, animation: `${fadeInUp} .45s ease` }}>
        <TituloSeccion>Flujo del despacho · Sprint 2</TituloSeccion>
        <Box
          sx={{
            display: 'grid',
            gap: 1.25,
            gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(4, 1fr)', lg: 'repeat(7, 1fr)' },
          }}
        >
          {ETAPAS.map((e, i) => {
            const cantidad = despachos?.filter((d) => d.estado === e.estado).length
            return (
              <ButtonBase
                key={e.estado}
                onClick={() => navegar(e.a)}
                sx={{
                  display: 'block',
                  textAlign: 'left',
                  p: 1.5,
                  borderRadius: 2,
                  border: 1,
                  borderColor: 'divider',
                  bgcolor: cantidad ? '#f5f9ff' : '#fff',
                  transition: 'transform .18s ease, box-shadow .18s ease, border-color .18s ease',
                  '&:hover': {
                    transform: 'translateY(-2px)',
                    borderColor: COLORES.azul,
                    boxShadow: '0 8px 18px rgba(26,111,220,0.12)',
                  },
                }}
              >
                <Typography sx={{ fontSize: 11, color: 'text.secondary', fontWeight: 600 }}>Paso {i + 1}</Typography>
                <Typography sx={{ fontSize: 22, fontWeight: 800, lineHeight: 1.2, color: COLORES.azul }}>
                  {despachos ? cantidad : '—'}
                </Typography>
                <Typography sx={{ fontSize: 12.5, fontWeight: 600, lineHeight: 1.25 }}>
                  {textoEstado(e.estado)}
                </Typography>
              </ButtonBase>
            )
          })}
        </Box>
      </Paper>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion
          accion={
            <Button variant="outlined" onClick={exportar} disabled={exportando || !despachos?.length}>
              {exportando ? 'Exportando…' : 'Exportar a Excel'}
            </Button>
          }
        >
          Despachos recientes
        </TituloSeccion>

        <Box
          sx={{
            display: 'grid',
            gap: 2,
            alignItems: 'end',
            mb: 2,
            gridTemplateColumns: { xs: '1fr', md: '1fr 2fr auto' },
          }}
        >
          <TextField
            select
            label="Aduana"
            value={aduanaSel}
            onChange={(e) => setAduanaSel(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <MenuItem value="">Todas</MenuItem>
            {aduanas.map((a) => (
              <MenuItem key={a.id} value={a.id}>
                {a.codigo} - {a.nombre}
              </MenuItem>
            ))}
          </TextField>
          <ClienteAutocomplete label="Cliente" value={clienteSel} onChange={setClienteSel} />
          <Stack direction="row" spacing={1}>
            <Button variant="contained" onClick={aplicar}>
              Aplicar filtro
            </Button>
            <Button onClick={limpiar}>Limpiar</Button>
          </Stack>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 1.5 }}>
            {error}
          </Alert>
        )}
        <TableContainer>
          <Table size="small" sx={{ '& td, & th': { py: 1.25 } }}>
            <TableHead>
              <TableRow>
                <TableCell>N° Despacho</TableCell>
                <TableCell>Cliente</TableCell>
                <TableCell>BL / Contenedor</TableCell>
                <TableCell>Aduana</TableCell>
                <TableCell>Canal</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell>ETA</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {despachos?.map((d) => (
                <TableRow
                  key={d.id}
                  hover
                  tabIndex={0}
                  onClick={() => abrir(d.id)}
                  onKeyDown={(e) => e.key === 'Enter' && abrir(d.id)}
                  sx={{ cursor: 'pointer', '&:focus-visible': { outline: 2, outlineColor: 'primary.main' } }}
                >
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{d.numero}</TableCell>
                  <TableCell>{d.cliente}</TableCell>
                  <TableCell>
                    {d.contenedor ?? d.numeroBl ?? '—'}
                    {d.totalContenedores > 1 && (
                      <Typography component="span" variant="caption" color="text.secondary">
                        {' '}
                        (+{d.totalContenedores - 1})
                      </Typography>
                    )}
                    {d.contenedor && d.numeroBl && (
                      <Typography variant="caption" color="text.secondary" component="div">
                        BL {d.numeroBl}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>{d.aduana ?? '—'}</TableCell>
                  <TableCell>
                    <CanalChip canal={d.canalControl} />
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                      <span>{textoEstado(d.estado)}</span>
                      {d.alertaCritica && (
                        <Tooltip title={motivoAlerta(d)}>
                          <WarningAmberIcon color="warning" fontSize="small" aria-label={motivoAlerta(d)} />
                        </Tooltip>
                      )}
                    </Stack>
                  </TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFecha(d.fechaEta)}</TableCell>
                </TableRow>
              ))}
              {despachos?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={COLUMNAS} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No hay despachos que coincidan con los filtros.
                  </TableCell>
                </TableRow>
              )}
              {despachos === null && (
                <TableRow>
                  <TableCell colSpan={COLUMNAS} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Cargando…
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Stack>
  )
}

function motivoAlerta(d: DespachoTablero): string {
  const motivos: string[] = []
  if (d.alertaAlmacenaje) {
    motivos.push(
      d.diasAlmacenaje !== null && d.diasAlmacenaje < 0
        ? `plazo de almacenaje vencido hace ${Math.abs(d.diasAlmacenaje)} días`
        : `quedan ${d.diasAlmacenaje} días de almacenaje`,
    )
  }
  if (d.estadoCartaPoder === 'VENCIDA') motivos.push('carta poder vencida')
  if (d.estadoCartaPoder === 'POR_VENCER') motivos.push('carta poder por vencer')
  return `Alerta crítica: ${motivos.join(', ')}`
}
