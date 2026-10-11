import FileDownloadIcon from '@mui/icons-material/FileDownload'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import {
  Alert,
  Box,
  Button,
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
import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'

import { listarAduanas } from '../api/catalogos'
import { obtenerCliente } from '../api/clientes'
import { exportarExcel, listarDespachosTablero, obtenerIndicadores } from '../api/dashboard'
import { mensajeDeError } from '../api/http'
import { ClienteAutocomplete } from '../components/ClienteAutocomplete'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip } from '../components/EstadoDespachoChip'
import { Tarjeta } from '../components/Tarjeta'
import type { Aduana, ClienteResumen, DespachoTablero, FiltrosTablero, Indicadores } from '../types'
import { formatoFecha } from '../utils/formato'

const TARJETAS: { clave: keyof Indicadores; titulo: string; color: string }[] = [
  { clave: 'despachosActivos', titulo: 'Despachos activos', color: '#0f2a47' },
  { clave: 'enCanalNaranjaRojo', titulo: 'En canal naranja/rojo', color: 'warning.main' },
  { clave: 'conLevante', titulo: 'Con levante registrado', color: 'success.main' },
  { clave: 'conAlertasCriticas', titulo: 'Con alertas críticas', color: 'error.main' },
]

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
  useEncabezado('HU-02', 'Dashboard')
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
    <Stack spacing={3}>
      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, 1fr)' } }}>
        {TARJETAS.map((t) => (
          <Paper key={t.clave} variant="outlined" sx={{ px: 2.5, py: 2 }}>
            <Typography variant="h4" component="p" sx={{ fontWeight: 700, color: t.color }} aria-label={t.titulo}>
              {indicadores ? indicadores[t.clave] : '—'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t.titulo}
            </Typography>
          </Paper>
        ))}
      </Box>

      <Tarjeta sx={{ py: 2 }}>
        <Box sx={{ display: 'grid', gap: 2, alignItems: 'end', gridTemplateColumns: { xs: '1fr', md: '1fr 2fr auto' } }}>
          <TextField
            select
            label="Aduana"
            value={aduanaSel}
            onChange={(e) => setAduanaSel(e.target.value === '' ? '' : Number(e.target.value))}
            slotProps={{ select: { displayEmpty: true } }}
          >
            <MenuItem value="">Todas</MenuItem>
            {aduanas.map((a) => (
              <MenuItem key={a.id} value={a.id}>
                {a.codigo} - {a.nombre}
              </MenuItem>
            ))}
          </TextField>
          <ClienteAutocomplete label="Cliente" value={clienteSel} onChange={setClienteSel} size="small" />
          <Stack direction="row" spacing={1}>
            <Button variant="contained" onClick={aplicar}>
              Aplicar filtro
            </Button>
            <Button variant="outlined" onClick={limpiar}>
              Limpiar
            </Button>
          </Stack>
        </Box>
      </Tarjeta>

      <Tarjeta
        titulo="Despachos recientes"
        acciones={
          <Button
            variant="outlined"
            startIcon={<FileDownloadIcon />}
            onClick={exportar}
            disabled={exportando || !despachos?.length}
          >
            Exportar a Excel
          </Button>
        }
      >
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>N° despacho</TableCell>
                <TableCell>Cliente</TableCell>
                <TableCell>Aduana</TableCell>
                <TableCell>BL</TableCell>
                <TableCell>ETA</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell align="right">Días de almacenaje</TableCell>
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
                  sx={{ cursor: 'pointer', '& td': { py: 1.25 }, '&:focus-visible': { outline: 2, outlineColor: 'primary.main' } }}
                >
                  <TableCell sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}>{d.numero}</TableCell>
                  <TableCell>{d.cliente}</TableCell>
                  <TableCell>{d.aduana ? `${d.aduanaCodigo} - ${d.aduana}` : '—'}</TableCell>
                  <TableCell>{d.numeroBl ?? '—'}</TableCell>
                  <TableCell>{formatoFecha(d.fechaEta)}</TableCell>
                  <TableCell>
                    <EstadoDespachoChip estado={d.estado} />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={0.5} sx={{ justifyContent: 'flex-end', alignItems: 'center' }}>
                      {d.alertaCritica && (
                        <Tooltip title={motivoAlerta(d)}>
                          <WarningAmberIcon color="warning" fontSize="small" aria-label={motivoAlerta(d)} />
                        </Tooltip>
                      )}
                      <span>{d.diasAlmacenaje ?? '—'}</span>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
              {despachos?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No hay despachos que coincidan con los filtros.
                  </TableCell>
                </TableRow>
              )}
              {despachos === null && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Cargando…
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Tarjeta>
    </Stack>
  )
}

function motivoAlerta(d: DespachoTablero): string {
  const motivos: string[] = []
  if (d.alertaAlmacenaje) motivos.push('plazo de almacenaje por vencer')
  if (d.estadoCartaPoder === 'VENCIDA') motivos.push('carta poder vencida')
  if (d.estadoCartaPoder === 'POR_VENCER') motivos.push('carta poder por vencer')
  return `Alerta crítica: ${motivos.join(', ')}`
}
