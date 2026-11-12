import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
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
  Typography,
} from '@mui/material'
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'

import { leerError, mensajeDeError } from '../api/http'
import { listarMisDespachos, obtenerMiDespacho, urlDocumentoPortal } from '../api/portalCliente'
import { useAuth } from '../auth/AuthContext'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip, textoEstado } from '../components/EstadoDespachoChip'
import { CanalChip, EtiquetaColor } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import { COLORES, fadeInUp } from '../theme'
import type { DespachoPortal, DetalleDespachoPortal, EstadoDespacho } from '../types'
import { ESTADOS_VISIBLES } from '../utils/estados'
import { formatoFecha, formatoFechaHora, formatoTamano } from '../utils/formato'

interface Filtros {
  numero: string
  estado: EstadoDespacho | ''
  etaDesde: string
  etaHasta: string
}

const SIN_FILTROS: Filtros = { numero: '', estado: '', etaDesde: '', etaHasta: '' }
const COLUMNAS = 7

/** Filtra en pantalla: el backend ya entrega solo los despachos de la empresa del usuario. */
function filtrar(despachos: DespachoPortal[], f: Filtros): DespachoPortal[] {
  const numero = f.numero.trim().toLowerCase()
  return despachos.filter(
    (d) =>
      (!numero || d.numero.toLowerCase().includes(numero)) &&
      (!f.estado || d.estado === f.estado) &&
      (!f.etaDesde || (d.fechaEta !== null && d.fechaEta.slice(0, 10) >= f.etaDesde)) &&
      (!f.etaHasta || (d.fechaEta !== null && d.fechaEta.slice(0, 10) <= f.etaHasta)),
  )
}

/**
 * HU-16 · Portal del cliente: «Mis despachos». El usuario CLIENTE consulta, en solo lectura, los
 * despachos de su empresa con su línea de tiempo y sus documentos. No hay botones de crear,
 * editar ni cargar. Quien decide qué despachos son suyos es el backend (contrato C11).
 */
export function MisDespachosPage() {
  useEncabezado('Portal del cliente', 'Mis despachos')
  const { usuario, cargando: cargandoSesion } = useAuth()
  const empresa = usuario?.empresa ?? null
  const [params, setParams] = useSearchParams()
  const seleccionado = params.get('despacho')

  const [despachos, setDespachos] = useState<DespachoPortal[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [borrador, setBorrador] = useState<Filtros>(SIN_FILTROS)
  const [filtros, setFiltros] = useState<Filtros>(SIN_FILTROS)
  const [errorRango, setErrorRango] = useState<string | null>(null)

  useEffect(() => {
    if (!empresa) return
    let activo = true
    listarMisDespachos()
      .then((d) => activo && setDespachos(d))
      .catch((e) => {
        if (!activo) return
        setDespachos([])
        setError(mensajeDeError(e, 'No se pudo cargar la lista de despachos.'))
      })
    return () => {
      activo = false
    }
  }, [empresa])

  const visibles = useMemo(() => filtrar(despachos ?? [], filtros), [despachos, filtros])

  function buscar(evento: FormEvent) {
    evento.preventDefault()
    if (borrador.etaDesde && borrador.etaHasta && borrador.etaDesde > borrador.etaHasta) {
      setErrorRango('La fecha «ETA desde» no puede ser posterior a «ETA hasta».')
      return
    }
    setErrorRango(null)
    setFiltros(borrador)
  }

  function limpiar() {
    setErrorRango(null)
    setBorrador(SIN_FILTROS)
    setFiltros(SIN_FILTROS)
  }

  function verDetalle(id: number) {
    const nuevos = new URLSearchParams(params)
    nuevos.set('despacho', String(id))
    setParams(nuevos)
  }

  if (cargandoSesion) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando sesión" />
      </Box>
    )
  }
  if (!empresa) {
    return (
      <Alert severity="warning">
        Tu usuario todavía no tiene una empresa asignada, así que no hay despachos que mostrar. Solicita al
        administrador de la agencia que te asigne tu empresa.
      </Alert>
    )
  }

  const campo = (clave: keyof Filtros) => ({
    value: borrador[clave],
    onChange: (e: { target: { value: string } }) => setBorrador((b) => ({ ...b, [clave]: e.target.value })),
  })

  return (
    <Stack spacing={2.5}>
      {/* status y no alert (el de MUI por defecto): es un aviso permanente, no debe interrumpir al lector de pantalla. */}
      <Alert severity="info" role="status" sx={{ animation: `${fadeInUp} .4s ease` }}>
        Solo se muestran los despachos de <strong>{empresa.razonSocial}</strong> (RUC {empresa.ruc}). La información es
        de solo lectura.
      </Alert>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion>Mis despachos</TituloSeccion>

        <Box
          component="form"
          onSubmit={buscar}
          aria-label="Filtrar mis despachos"
          sx={{
            display: 'grid',
            gap: 2,
            alignItems: 'start',
            mb: 2,
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1.2fr 1.2fr 1fr 1fr auto' },
          }}
        >
          <TextField label="N° de despacho" placeholder="DESP-2026-0001" {...campo('numero')} />
          <TextField select label="Estado" {...campo('estado')}>
            <MenuItem value="">Todos</MenuItem>
            {ESTADOS_VISIBLES.map((e) => (
              <MenuItem key={e} value={e}>
                {textoEstado(e)}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            type="date"
            label="ETA desde"
            {...campo('etaDesde')}
            error={Boolean(errorRango)}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            type="date"
            label="ETA hasta"
            {...campo('etaHasta')}
            error={Boolean(errorRango)}
            helperText={errorRango}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <Stack direction="row" spacing={1} sx={{ alignSelf: { lg: 'center' } }}>
            <Button type="submit" variant="contained">
              Buscar
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
          <Table size="small" aria-label="Mis despachos" sx={{ '& td, & th': { py: 1.25 } }}>
            <TableHead>
              <TableRow>
                <TableCell>N° despacho</TableCell>
                <TableCell>BL</TableCell>
                <TableCell>ETA</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell>Canal</TableCell>
                <TableCell>Última actualización</TableCell>
                <TableCell>
                  <Box component="span" sx={visualmenteOculto}>
                    Acciones
                  </Box>
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visibles.map((d) => (
                <TableRow key={d.id} selected={seleccionado === String(d.id)}>
                  <TableCell sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>{d.numero}</TableCell>
                  <TableCell>{d.numeroBl ?? '—'}</TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFecha(d.fechaEta)}</TableCell>
                  <TableCell>
                    <EstadoDespachoChip estado={d.estado} />
                  </TableCell>
                  <TableCell>
                    <CanalChip canal={d.canalControl} />
                  </TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(d.actualizadoEn)}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      onClick={() => verDetalle(d.id)}
                      aria-label={`Ver detalle del despacho ${d.numero}`}
                      sx={{ whiteSpace: 'nowrap' }}
                    >
                      Ver detalle
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {despachos !== null && visibles.length === 0 && (
                <TableRow>
                  <TableCell colSpan={COLUMNAS} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    {despachos.length === 0
                      ? 'Tu empresa todavía no tiene despachos registrados.'
                      : 'No hay despachos que coincidan con los filtros.'}
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

      {seleccionado && <DetalleMiDespacho key={seleccionado} idTexto={seleccionado} />}
    </Stack>
  )
}

/** Oculta un texto a la vista pero no al lector de pantalla. */
const visualmenteOculto = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const

/** Detalle de solo lectura: datos, línea de tiempo y documentos descargables. */
function DetalleMiDespacho({ idTexto }: { idTexto: string }) {
  const id = Number(idTexto)
  const valido = Number.isInteger(id) && id > 0
  const [detalle, setDetalle] = useState<DetalleDespachoPortal | null>(null)
  const [error, setError] = useState<string | null>(valido ? null : 'Despacho no encontrado')
  const titulo = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (!valido) return
    let activo = true
    obtenerMiDespacho(id)
      .then((d) => activo && setDetalle(d))
      .catch((e) => {
        if (!activo) return
        // 403 y 404 se muestran igual: no se revela si el despacho existe en otra empresa.
        const { status } = leerError(e)
        setError(
          status === 404 || status === 403
            ? 'Despacho no encontrado'
            : mensajeDeError(e, 'No se pudo cargar el detalle del despacho.'),
        )
      })
    return () => {
      activo = false
    }
  }, [id, valido])

  // Al abrir el detalle, el foco va a su título para que el lector de pantalla lo anuncie.
  useEffect(() => {
    if (!detalle) return
    titulo.current?.focus()
    titulo.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  }, [detalle])

  if (error) {
    return (
      <Alert severity={error === 'Despacho no encontrado' ? 'warning' : 'error'} role="alert">
        {error === 'Despacho no encontrado' ? (
          <>
            <strong>Despacho no encontrado.</strong> No existe o no pertenece a tu empresa.
          </>
        ) : (
          error
        )}
      </Alert>
    )
  }
  if (!detalle) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando detalle del despacho" />
      </Box>
    )
  }

  return (
    <Paper sx={{ p: { xs: 2, md: 2.5 }, animation: `${fadeInUp} .35s ease` }} component="section" aria-labelledby="titulo-detalle">
      <Stack
        direction="row"
        sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1.25, pb: 1.25, mb: 2, borderBottom: 1, borderColor: 'divider' }}
      >
        <Typography
          id="titulo-detalle"
          ref={titulo}
          tabIndex={-1}
          component="h2"
          sx={{ fontSize: 17, fontWeight: 700, outline: 'none' }}
        >
          Despacho {detalle.numero}
        </Typography>
        <EstadoDespachoChip estado={detalle.estado} />
        <CanalChip canal={detalle.canalControl} />
      </Stack>

      <Box
        component="dl"
        sx={{
          display: 'grid',
          gap: 2,
          m: 0,
          mb: 3,
          gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, 1fr)' },
          '& dt': { fontSize: 12, color: 'text.secondary' },
          '& dd': { m: 0, fontSize: 14, fontWeight: 600, overflowWrap: 'anywhere' },
        }}
      >
        <div>
          <dt>N° de BL</dt>
          <dd>{detalle.numeroBl ?? '—'}</dd>
        </div>
        <div>
          <dt>ETA</dt>
          <dd>{formatoFecha(detalle.fechaEta)}</dd>
        </div>
        <div>
          <dt>Aduana</dt>
          <dd>{detalle.aduana ?? '—'}</dd>
        </div>
        <div>
          <dt>Última actualización</dt>
          <dd>{formatoFechaHora(detalle.actualizadoEn)}</dd>
        </div>
      </Box>

      <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: '3fr 2fr' } }}>
        <Box>
          <TituloSeccion>Línea de tiempo</TituloSeccion>
          {detalle.hitos.length === 0 ? (
            <Alert severity="info" role="status">Todavía no hay hitos registrados.</Alert>
          ) : (
            <Box component="ol" aria-label="Línea de tiempo del despacho" sx={{ listStyle: 'none', m: 0, p: 0 }}>
              {detalle.hitos.map((h, i) => {
                const ultimo = i === detalle.hitos.length - 1
                return (
                  <Box
                    component="li"
                    key={`${h.fecha}-${i}`}
                    sx={{ position: 'relative', pl: 3.5, pb: ultimo ? 0 : 2.25 }}
                  >
                    {/* Punto y línea de la línea de tiempo (decorativos). */}
                    <Box
                      aria-hidden
                      sx={{
                        position: 'absolute',
                        left: 4,
                        top: 4,
                        width: 12,
                        height: 12,
                        borderRadius: '50%',
                        bgcolor: ultimo ? COLORES.verde : COLORES.azul,
                        boxShadow: `0 0 0 3px ${ultimo ? COLORES.verde : COLORES.azul}26`,
                      }}
                    />
                    {!ultimo && (
                      <Box
                        aria-hidden
                        sx={{ position: 'absolute', left: 9, top: 20, bottom: 0, width: 2, bgcolor: 'divider' }}
                      />
                    )}
                    <Typography sx={{ fontSize: 14, fontWeight: 600 }}>
                      {h.descripcion} {h.sugerido && <EtiquetaColor texto="Sugerido" tono="naranja" />}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {formatoFechaHora(h.fecha)} · {h.usuario}
                    </Typography>
                  </Box>
                )
              })}
            </Box>
          )}
        </Box>

        <Box>
          <TituloSeccion>Documentos</TituloSeccion>
          {detalle.documentos.length === 0 ? (
            <Alert severity="info" role="status">Todavía no hay documentos cargados.</Alert>
          ) : (
            <Stack component="ul" spacing={1} aria-label="Documentos del despacho" sx={{ listStyle: 'none', m: 0, p: 0 }}>
              {detalle.documentos.map((doc) => (
                <Stack
                  component="li"
                  key={doc.id}
                  direction="row"
                  spacing={1.5}
                  sx={{ alignItems: 'center', p: 1.25, border: 1, borderColor: 'divider', borderRadius: 2 }}
                >
                  <InsertDriveFileOutlinedIcon aria-hidden sx={{ color: COLORES.azul }} />
                  <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                    <Typography sx={{ fontSize: 13.5, fontWeight: 600 }}>{doc.tipo}</Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', overflowWrap: 'anywhere' }}>
                      {doc.nombreArchivo}
                      {doc.tamanoBytes !== null && ` · ${formatoTamano(doc.tamanoBytes)}`} · {formatoFecha(doc.cargadoEn)}
                    </Typography>
                  </Box>
                  <Button
                    component="a"
                    href={urlDocumentoPortal(detalle.id, doc.id)}
                    download
                    size="small"
                    startIcon={<DownloadOutlinedIcon />}
                    aria-label={`Descargar ${doc.tipo}`}
                  >
                    Descargar
                  </Button>
                </Stack>
              ))}
            </Stack>
          )}
        </Box>
      </Box>
    </Paper>
  )
}
