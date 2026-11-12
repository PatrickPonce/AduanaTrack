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
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'

import { obtenerDespacho } from '../api/despachos'
import { listarDocumentos, urlArchivoDocumento } from '../api/documentos'
import { listarHitos } from '../api/hitos'
import { leerError, mensajeDeError } from '../api/http'
import { listarMisDespachos } from '../api/portalCliente'
import { useMiEmpresa } from '../auth/useMiEmpresa'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip, textoEstado } from '../components/EstadoDespachoChip'
import { CanalChip, EtiquetaColor } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import { COLORES, fadeInUp } from '../theme'
import type { Despacho, DespachoPortal, DocumentoRequerido, EstadoDespacho, FiltrosPortal, Hito } from '../types'
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
const NO_ENCONTRADO = 'Despacho no encontrado'

/**
 * HU-16 · Portal del cliente: «Mis despachos» (contrato C11). El usuario CLIENTE consulta, en solo lectura,
 * los despachos de su empresa con su línea de tiempo y sus documentos. No hay botones de crear, editar ni
 * cargar. El backend filtra por la empresa de la sesión; los filtros de la tabla se envían al servidor.
 */
export function MisDespachosPage() {
  useEncabezado('Portal del cliente', 'Mis despachos')
  const { empresa, cargando: cargandoEmpresa, error: errorEmpresa } = useMiEmpresa(true)
  const conEmpresa = empresa?.clienteId != null
  const [params, setParams] = useSearchParams()
  const seleccionado = params.get('despacho')

  const [despachos, setDespachos] = useState<DespachoPortal[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [borrador, setBorrador] = useState<Filtros>(SIN_FILTROS)
  const [buscadoPorNumero, setBuscadoPorNumero] = useState(false)

  const consultar = useCallback(async (f: Filtros) => {
    setDespachos(null)
    setError(null)
    setErrores({})
    const filtros: FiltrosPortal = {
      numero: f.numero,
      estado: f.estado || undefined,
      etaDesde: f.etaDesde,
      etaHasta: f.etaHasta,
    }
    try {
      setDespachos(await listarMisDespachos(filtros))
      setBuscadoPorNumero(Boolean(f.numero.trim()))
    } catch (e) {
      setDespachos([])
      setBuscadoPorNumero(false)
      const { status, body } = leerError(e)
      // 400 del C11: { estado, etaDesde, etaHasta } se muestran junto a cada filtro.
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeDeError(e, 'No se pudo cargar la lista de despachos.'))
    }
  }, [])

  useEffect(() => {
    if (conEmpresa) void consultar(SIN_FILTROS)
  }, [conEmpresa, consultar])

  function buscar(evento: FormEvent) {
    evento.preventDefault()
    void consultar(borrador)
  }

  function limpiar() {
    setBorrador(SIN_FILTROS)
    void consultar(SIN_FILTROS)
  }

  function verDetalle(id: number) {
    const nuevos = new URLSearchParams(params)
    nuevos.set('despacho', String(id))
    setParams(nuevos)
  }

  if (cargandoEmpresa) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando datos de su empresa" />
      </Box>
    )
  }
  if (errorEmpresa) return <Alert severity="error">{errorEmpresa}</Alert>
  if (!conEmpresa) {
    return <Alert severity="warning">Aún no tiene una empresa asignada; contacte al administrador.</Alert>
  }

  const campo = (clave: keyof Filtros) => ({
    value: borrador[clave],
    onChange: (e: { target: { value: string } }) => {
      setBorrador((b) => ({ ...b, [clave]: e.target.value }))
      setErrores((actuales) => {
        const { [clave]: _quitado, ...resto } = actuales
        return resto
      })
    },
    error: Boolean(errores[clave]),
    helperText: errores[clave],
  })

  const fila = despachos?.find((d) => String(d.id) === seleccionado) ?? null

  return (
    <Stack spacing={2.5}>
      {/* status y no alert (el de MUI por defecto): es un aviso permanente, no debe interrumpir al lector de pantalla. */}
      <Alert severity="info" role="status" sx={{ animation: `${fadeInUp} .4s ease` }}>
        Solo se muestran los despachos de <strong>{empresa!.razonSocial}</strong> (RUC {empresa!.ruc}). La información es
        de solo lectura.
      </Alert>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion>Mis despachos</TituloSeccion>

        <Box
          component="form"
          onSubmit={buscar}
          noValidate
          aria-label="Filtrar mis despachos"
          sx={{
            display: 'grid',
            gap: 2,
            alignItems: 'start',
            mb: 2,
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1.2fr 1.2fr 1fr 1fr auto' },
          }}
        >
          <TextField label="N° de despacho" placeholder="DESP-2026-0318" {...campo('numero')} />
          <TextField select label="Estado" {...campo('estado')}>
            <MenuItem value="">Todos</MenuItem>
            {ESTADOS_VISIBLES.map((e) => (
              <MenuItem key={e} value={e}>
                {textoEstado(e)}
              </MenuItem>
            ))}
          </TextField>
          <TextField type="date" label="ETA desde" {...campo('etaDesde')} slotProps={{ inputLabel: { shrink: true } }} />
          <TextField type="date" label="ETA hasta" {...campo('etaHasta')} slotProps={{ inputLabel: { shrink: true } }} />
          <Stack direction="row" spacing={1} sx={{ alignSelf: { lg: 'center' } }}>
            <Button type="submit" variant="contained" disabled={despachos === null}>
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
        {/* C11: búsqueda por número sin resultados → mensaje rojo del prototipo. */}
        {buscadoPorNumero && despachos?.length === 0 && (
          <Alert severity="error" sx={{ mb: 1.5 }}>
            {NO_ENCONTRADO}
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
              {despachos?.map((d) => (
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
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(d.ultimaActualizacion)}</TableCell>
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
              {despachos?.length === 0 && !buscadoPorNumero && !error && Object.keys(errores).length === 0 && (
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

      {seleccionado && <DetalleMiDespacho key={seleccionado} idTexto={seleccionado} fila={fila} />}
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

interface Detalle {
  despacho: Despacho
  hitos: Hito[]
  documentos: DocumentoRequerido[]
}

/**
 * Detalle de solo lectura con las rutas de siempre (GET /despachos/{id}, /hitos y /documentos). El backend
 * responde 404 «Despacho no encontrado» si el despacho es de otra empresa. BL, ETA y última actualización
 * salen de la fila de la tabla cuando está a la vista (GET /despachos/{id} no los trae).
 */
function DetalleMiDespacho({ idTexto, fila }: { idTexto: string; fila: DespachoPortal | null }) {
  const id = Number(idTexto)
  const valido = Number.isInteger(id) && id > 0
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const [error, setError] = useState<string | null>(valido ? null : NO_ENCONTRADO)
  const titulo = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (!valido) return
    let activo = true
    Promise.all([obtenerDespacho(id), listarHitos(id), listarDocumentos(id)])
      .then(([despacho, hitos, documentos]) => activo && setDetalle({ despacho, hitos, documentos: documentos.documentos }))
      .catch((e) => {
        if (!activo) return
        // 404 (otra empresa, borrador o inexistente) y 403 se muestran igual: no se revela si existe.
        const { status } = leerError(e)
        setError(
          status === 404 || status === 403 ? NO_ENCONTRADO : mensajeDeError(e, 'No se pudo cargar el detalle del despacho.'),
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
      <Alert severity={error === NO_ENCONTRADO ? 'warning' : 'error'} role="alert">
        {error === NO_ENCONTRADO ? (
          <>
            <strong>{NO_ENCONTRADO}.</strong> No existe o no pertenece a su empresa.
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

  const { despacho, hitos, documentos } = detalle

  return (
    <Paper
      sx={{ p: { xs: 2, md: 2.5 }, animation: `${fadeInUp} .35s ease` }}
      component="section"
      aria-labelledby="titulo-detalle"
    >
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
          Despacho {despacho.numero ?? fila?.numero}
        </Typography>
        <EstadoDespachoChip estado={despacho.estado} />
        <CanalChip canal={despacho.canalControl} />
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
          <dd>{fila?.numeroBl ?? '—'}</dd>
        </div>
        <div>
          <dt>ETA</dt>
          <dd>{formatoFecha(fila?.fechaEta)}</dd>
        </div>
        <div>
          <dt>Aduana</dt>
          <dd>{despacho.aduana?.descripcion ?? '—'}</dd>
        </div>
        <div>
          <dt>Última actualización</dt>
          <dd>{formatoFechaHora(fila?.ultimaActualizacion)}</dd>
        </div>
      </Box>

      <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: '3fr 2fr' } }}>
        <Box>
          <TituloSeccion>Línea de tiempo</TituloSeccion>
          {hitos.length === 0 ? (
            <Alert severity="info" role="status">
              Todavía no hay hitos registrados.
            </Alert>
          ) : (
            <Box component="ol" aria-label="Línea de tiempo del despacho" sx={{ listStyle: 'none', m: 0, p: 0 }}>
              {hitos.map((h, i) => {
                const ultimo = i === hitos.length - 1
                return (
                  <Box component="li" key={`${h.fecha}-${i}`} sx={{ position: 'relative', pl: 3.5, pb: ultimo ? 0 : 2.25 }}>
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
          <Stack component="ul" spacing={1} aria-label="Documentos del despacho" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {documentos.map((doc) => (
              <Stack
                component="li"
                key={doc.tipo}
                direction="row"
                spacing={1.5}
                sx={{ alignItems: 'center', p: 1.25, border: 1, borderColor: 'divider', borderRadius: 2 }}
              >
                <InsertDriveFileOutlinedIcon aria-hidden sx={{ color: doc.documentoId ? COLORES.azul : 'text.disabled' }} />
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography sx={{ fontSize: 13.5, fontWeight: 600 }}>{doc.nombre}</Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', overflowWrap: 'anywhere' }}>
                    {doc.documentoId
                      ? `${doc.archivo ?? ''}${doc.tamanoBytes !== null ? ` · ${formatoTamano(doc.tamanoBytes)}` : ''} · ${formatoFecha(doc.cargadoEn)}`
                      : 'Aún no se ha cargado'}
                  </Typography>
                </Box>
                {doc.documentoId ? (
                  <Button
                    component="a"
                    href={urlArchivoDocumento(despacho.id, doc.documentoId)}
                    download
                    size="small"
                    startIcon={<DownloadOutlinedIcon />}
                    aria-label={`Descargar ${doc.nombre}`}
                  >
                    Descargar
                  </Button>
                ) : (
                  <EtiquetaColor texto="Pendiente" tono="gris" />
                )}
              </Stack>
            ))}
          </Stack>
        </Box>
      </Box>
    </Paper>
  )
}
