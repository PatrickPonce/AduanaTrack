import DownloadIcon from '@mui/icons-material/FileDownloadOutlined'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
  Paper,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  type TextFieldProps,
  Tooltip,
  Typography,
} from '@mui/material'
import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link as RouterLink, useLocation, useNavigate, useParams } from 'react-router'

import {
  actualizarCliente,
  crearCliente,
  existeRuc,
  historialCliente,
  listarCartasPoder,
  obtenerCliente,
  subirCartaPoder,
  urlArchivoCartaPoder,
} from '../api/clientes'
import { leerError, mensajeDeError } from '../api/http'
import { CartaPoderChip } from '../components/CartaPoderChip'
import { useEncabezado } from '../components/Encabezado'
import { DOS_COLUMNAS, TituloSeccion } from '../components/Seccion'
import type { CambioCliente, CartaPoderResumen, Cliente, ClienteRequest } from '../types'
import { formatoFecha, formatoFechaHora } from '../utils/formato'
import { tipoContribuyente } from '../utils/opciones'
import { validarArchivoPdf, validarRuc } from '../utils/validaciones'

const VACIO: ClienteRequest = { ruc: '', razonSocial: '', direccion: '', correo: '', telefono: '', contacto: '' }

const ETIQUETAS: Record<string, string> = {
  ruc: 'RUC',
  razonSocial: 'Razón social',
  direccion: 'Dirección fiscal',
  correo: 'Correo de contacto',
  telefono: 'Teléfono',
  contacto: 'Representante / contacto operativo',
}

interface Duplicado {
  clienteId: number
  razonSocial: string
}

interface CartaPoderForm {
  archivo: File | null
  fechaEmision: string
  fechaVencimiento: string
}

const CARTA_VACIA: CartaPoderForm = { archivo: null, fechaEmision: '', fechaVencimiento: '' }

/** HU-04 · T-04.4: alta y edición del cliente con detección de duplicados, carta poder e historial. */
export function ClienteFormPage() {
  const { id } = useParams()
  const clienteId = id ? Number(id) : null
  const esEdicion = clienteId !== null
  const navegar = useNavigate()
  const ubicacion = useLocation()

  const [form, setForm] = useState<ClienteRequest>(VACIO)
  const [cliente, setCliente] = useState<Cliente | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [duplicado, setDuplicado] = useState<Duplicado | null>(null)
  const [carta, setCarta] = useState<CartaPoderForm>(CARTA_VACIA)
  const [erroresCarta, setErroresCarta] = useState<Record<string, string>>({})
  const [cartas, setCartas] = useState<CartaPoderResumen[]>([])
  const [historial, setHistorial] = useState<CambioCliente[]>([])
  const [pestana, setPestana] = useState(0)
  const [cargando, setCargando] = useState(esEdicion)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEncabezado(
    esEdicion ? `Clientes / ${cliente?.ruc ?? ''}` : 'Clientes / Nuevo',
    esEdicion ? 'Datos del Importador' : 'Registro de Datos del Importador',
  )

  // /clientes/nuevo → /clientes/:id reutiliza este componente: el aviso se lee en cada navegación.
  useEffect(() => {
    setMensaje((ubicacion.state as { mensaje?: string } | null)?.mensaje ?? null)
  }, [ubicacion.key, ubicacion.state])

  const cargar = useCallback(async () => {
    if (clienteId === null) return
    try {
      const [c, h, cp] = await Promise.all([
        obtenerCliente(clienteId),
        historialCliente(clienteId),
        listarCartasPoder(clienteId),
      ])
      setCliente(c)
      setHistorial(h)
      setCartas(cp)
      setForm({
        ruc: c.ruc,
        razonSocial: c.razonSocial,
        direccion: c.direccion ?? '',
        correo: c.correo ?? '',
        telefono: c.telefono ?? '',
        contacto: c.contacto ?? '',
      })
    } catch (e) {
      setError(mensajeDeError(e, 'No se pudo cargar el cliente.'))
    } finally {
      setCargando(false)
    }
  }, [clienteId])

  useEffect(() => {
    setForm(VACIO)
    setCliente(null)
    setCartas([])
    setDuplicado(null)
    setErrores({})
    setCarta(CARTA_VACIA)
    setPestana(0)
    setCargando(esEdicion)
    void cargar()
  }, [cargar, esEdicion])

  function cambiar(campo: keyof ClienteRequest) {
    return (e: ChangeEvent<HTMLInputElement>) => {
      const valor = campo === 'ruc' ? e.target.value.replace(/\D/g, '').slice(0, 11) : e.target.value
      setForm((f) => ({ ...f, [campo]: valor }))
      setErrores((er) => ({ ...er, [campo]: '' }))
      if (campo === 'ruc') setDuplicado(null)
    }
  }

  /** CA3: al salir del campo RUC se verifica si ya existe otro cliente con ese RUC. */
  async function verificarRuc() {
    const problema = validarRuc(form.ruc)
    if (problema) {
      if (form.ruc) setErrores((er) => ({ ...er, ruc: problema }))
      return
    }
    if (cliente && cliente.ruc === form.ruc) return
    try {
      const r = await existeRuc(form.ruc)
      setDuplicado(r.existe && r.clienteId !== clienteId ? { clienteId: r.clienteId!, razonSocial: r.razonSocial! } : null)
    } catch {
      // Si la verificación falla, el 409 del guardado sigue protegiendo el duplicado.
    }
  }

  function seleccionarArchivo(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0] ?? null
    e.target.value = ''
    if (!archivo) return
    const problema = validarArchivoPdf(archivo)
    setErroresCarta((er) => ({ ...er, archivo: problema ?? '' }))
    setCarta((c) => ({ ...c, archivo: problema ? null : archivo }))
  }

  function validar(): boolean {
    const nuevos: Record<string, string> = {}
    const problemaRuc = validarRuc(form.ruc)
    if (problemaRuc) nuevos.ruc = problemaRuc
    if (!form.razonSocial.trim()) nuevos.razonSocial = 'Ingrese la razón social'
    if (form.correo.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.correo.trim())) nuevos.correo = 'Correo no válido'
    setErrores(nuevos)

    const nuevosCarta: Record<string, string> = {}
    const hayCarta = carta.archivo || carta.fechaEmision || carta.fechaVencimiento
    if (hayCarta) {
      if (!carta.archivo) nuevosCarta.archivo = 'Seleccione el PDF de la carta poder'
      if (!carta.fechaEmision) nuevosCarta.fechaEmision = 'Ingrese la fecha de emisión'
      if (!carta.fechaVencimiento) nuevosCarta.fechaVencimiento = 'Ingrese la fecha de vigencia'
      if (carta.fechaEmision && carta.fechaVencimiento && carta.fechaVencimiento <= carta.fechaEmision) {
        nuevosCarta.fechaVencimiento = 'Debe ser posterior a la fecha de emisión'
      }
    }
    setErroresCarta(nuevosCarta)
    return Object.keys(nuevos).length === 0 && Object.keys(nuevosCarta).length === 0 && !duplicado
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    setMensaje(null)
    if (!validar()) return
    setGuardando(true)
    try {
      const guardado = clienteId !== null ? await actualizarCliente(clienteId, form) : await crearCliente(form)
      let aviso = esEdicion ? 'Cliente actualizado correctamente.' : 'Cliente registrado correctamente.'
      if (carta.archivo) {
        try {
          await subirCartaPoder(guardado.id, carta.archivo, carta.fechaEmision, carta.fechaVencimiento)
          aviso += ' Carta poder cargada.'
        } catch (e) {
          aviso += ` La carta poder no se pudo cargar: ${mensajeCartaPoder(e)}`
        }
      }
      setCarta(CARTA_VACIA)
      if (esEdicion) {
        setMensaje(aviso)
        await cargar()
      } else {
        navegar(`/clientes/${guardado.id}`, { replace: true, state: { mensaje: aviso } })
      }
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 409 && body?.clienteId) {
        setDuplicado({ clienteId: Number(body.clienteId), razonSocial: String(body.razonSocial ?? '') })
      } else if (status === 400 && body?.campos) {
        setErrores(body.campos)
      } else {
        setError(mensajeDeError(e, 'No se pudo guardar el cliente.'))
      }
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando cliente" />
      </Box>
    )
  }

  const campo = (nombre: keyof ClienteRequest, label: string, extra: Partial<TextFieldProps> = {}) => (
    <TextField
      label={label}
      value={form[nombre]}
      onChange={cambiar(nombre)}
      error={Boolean(errores[nombre])}
      helperText={errores[nombre]}
      {...extra}
    />
  )

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
        {esEdicion ? (
          <Tabs value={pestana} onChange={(_, v: number) => setPestana(v)}>
            <Tab label="Datos del cliente" />
            <Tab label="Historial de cambios" />
          </Tabs>
        ) : (
          <span />
        )}
        <Button component={RouterLink} to="/clientes">
          Volver a clientes
        </Button>
      </Stack>

      {mensaje && <Alert severity="success">{mensaje}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      {pestana === 0 && (
        <Paper component="form" onSubmit={guardar} noValidate sx={{ p: { xs: 2, md: 2.5 } }}>
          {duplicado && (
            <Alert
              severity="warning"
              sx={{ mb: 2 }}
              action={
                <Button color="inherit" size="small" onClick={() => navegar(`/clientes/${duplicado.clienteId}`)}>
                  Abrir cliente existente
                </Button>
              }
            >
              Este RUC ya está registrado: {duplicado.razonSocial}
            </Alert>
          )}

          <TituloSeccion>Información del importador</TituloSeccion>
          <Box sx={DOS_COLUMNAS}>
            {campo('razonSocial', 'Razón social', { required: true })}
            {campo('ruc', 'RUC', {
              required: true,
              onBlur: verificarRuc,
              helperText: errores.ruc || '11 dígitos',
              slotProps: { htmlInput: { inputMode: 'numeric', maxLength: 11, pattern: '\\d*' } },
            })}
            <TextField
              label="Tipo de contribuyente"
              value={tipoContribuyente(form.ruc) || 'Se determina con el RUC'}
              disabled
              slotProps={{ htmlInput: { readOnly: true } }}
            />
            {campo('direccion', 'Dirección fiscal')}
            {campo('correo', 'Correo de contacto', { type: 'email' })}
            {campo('telefono', 'Teléfono', { slotProps: { htmlInput: { inputMode: 'tel' } } })}
            {campo('contacto', 'Representante / contacto operativo')}
          </Box>

          <Box sx={{ mt: 3 }}>
            <TituloSeccion>Carta poder / autorización vigente</TituloSeccion>
            {cliente?.estadoCartaPoder === 'POR_VENCER' && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                La carta poder vence en 15 días o menos. Actualícela.
              </Alert>
            )}
            {cliente?.estadoCartaPoder === 'VENCIDA' && (
              <Alert severity="error" sx={{ mb: 2 }}>
                La carta poder está vencida. No se podrán abrir despachos.
              </Alert>
            )}
            <Table size="small" sx={{ mb: 2 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Documento</TableCell>
                  <TableCell>Vigencia</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell sx={{ width: 56 }} />
                </TableRow>
              </TableHead>
              <TableBody>
                {cartas.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell>{c.nombreArchivo}</TableCell>
                    <TableCell>hasta {formatoFecha(c.fechaVencimiento)}</TableCell>
                    <TableCell>
                      <CartaPoderChip estado={c.estado} />
                    </TableCell>
                    <TableCell>
                      <Tooltip title="Descargar PDF">
                        <IconButton
                          size="small"
                          component="a"
                          href={urlArchivoCartaPoder(clienteId!, c.id)}
                          aria-label={`Descargar ${c.nombreArchivo}`}
                        >
                          <DownloadIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
                {cartas.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} sx={{ color: 'text.secondary', py: 2 }}>
                      Sin carta poder registrada. Adjunte el PDF con su fecha de vigencia.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>

            <Box
              sx={{
                display: 'grid',
                gap: 2,
                alignItems: 'start',
                gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr' },
              }}
            >
              <Box>
                <Typography component="span" sx={{ display: 'block', fontSize: 12.5, fontWeight: 600, mb: 0.75 }}>
                  {cartas.length ? 'Adjuntar nueva carta poder' : 'Adjuntar carta poder'}
                </Typography>
                <Button component="label" variant="outlined" startIcon={<UploadFileIcon />}>
                  Seleccionar PDF
                  <input
                    hidden
                    type="file"
                    accept="application/pdf"
                    onChange={seleccionarArchivo}
                    aria-label="Archivo de carta poder"
                  />
                </Button>
                <Typography
                  variant="body2"
                  sx={{ mt: 0.5, fontSize: 12.5 }}
                  color={erroresCarta.archivo ? 'error' : 'text.secondary'}
                >
                  {erroresCarta.archivo || (carta.archivo ? carta.archivo.name : 'Solo PDF, máximo 10 MB')}
                </Typography>
              </Box>
              <TextField
                label="Fecha de emisión"
                type="date"
                value={carta.fechaEmision}
                onChange={(e) => setCarta((c) => ({ ...c, fechaEmision: e.target.value }))}
                error={Boolean(erroresCarta.fechaEmision)}
                helperText={erroresCarta.fechaEmision}
              />
              <TextField
                label="Fecha de vigencia"
                type="date"
                value={carta.fechaVencimiento}
                onChange={(e) => setCarta((c) => ({ ...c, fechaVencimiento: e.target.value }))}
                error={Boolean(erroresCarta.fechaVencimiento)}
                helperText={erroresCarta.fechaVencimiento}
              />
            </Box>
          </Box>

          <Box sx={{ mt: 3 }}>
            <Button type="submit" variant="contained" disabled={guardando}>
              {guardando ? 'Guardando…' : 'Guardar cliente'}
            </Button>
          </Box>
        </Paper>
      )}

      {pestana === 1 && (
        <Paper sx={{ p: 2 }}>
          {historial.length === 0 ? (
            <Typography color="text.secondary" sx={{ p: 2 }}>
              Sin cambios registrados.
            </Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Fecha</TableCell>
                  <TableCell>Campo</TableCell>
                  <TableCell>Antes</TableCell>
                  <TableCell>Después</TableCell>
                  <TableCell>Usuario</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {historial.map((h, i) => (
                  <TableRow key={`${h.fecha}-${h.campo}-${i}`}>
                    <TableCell>{formatoFechaHora(h.fecha)}</TableCell>
                    <TableCell>{ETIQUETAS[h.campo] ?? h.campo}</TableCell>
                    <TableCell>{h.valorAnterior ?? '—'}</TableCell>
                    <TableCell>{h.valorNuevo ?? '—'}</TableCell>
                    <TableCell>{h.usuario}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          {cliente && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, px: 1 }}>
              Registrado el {formatoFecha(cliente.creadoEn)}
            </Typography>
          )}
        </Paper>
      )}
    </Stack>
  )
}

function mensajeCartaPoder(error: unknown): string {
  const { status } = leerError(error)
  if (status === 413) return 'el archivo supera los 10 MB.'
  if (status === 415) return 'el archivo no es un PDF válido.'
  return mensajeDeError(error)
}
