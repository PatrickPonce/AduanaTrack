import UploadFileIcon from '@mui/icons-material/UploadFile'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'

import { leerError, mensajeDeError } from '../api/http'
import { obtenerLevante, registrarLevante, urlConstanciaLevante, type Levante } from '../api/levante'
import type { EstadoDespacho } from '../types'
import { alcanzoEstado } from '../utils/estados'
import { formatoFecha, formatoTamano, hoyIso } from '../utils/formato'
import { validarArchivoPdf } from '../utils/validaciones'
import { EtiquetaColor } from './EtiquetaColor'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

const MAX_NUMERO_LEVANTE = 30
const FORMATO_LEVANTE = /^[A-Za-z0-9][A-Za-z0-9/-]*$/

export interface FormLevante {
  numeroLevante: string
  fechaLevante: string
}

/** Mismas reglas que el backend (contrato C8): número de hasta 30 caracteres y fecha obligatoria, no futura. */
export function validarLevante(form: FormLevante, hoy: string): Record<string, string> {
  const errores: Record<string, string> = {}
  const numero = form.numeroLevante.trim()
  if (!numero) errores.numeroLevante = 'Ingrese el N° de levante'
  else if (numero.length > MAX_NUMERO_LEVANTE) errores.numeroLevante = `Máximo ${MAX_NUMERO_LEVANTE} caracteres`
  else if (!FORMATO_LEVANTE.test(numero)) errores.numeroLevante = 'Use solo letras, números, guiones y barras'
  if (!form.fechaLevante) errores.fechaLevante = 'Ingrese la fecha del levante'
  else if (form.fechaLevante > hoy) errores.fechaLevante = 'La fecha del levante no puede ser futura'
  return errores
}

/** Lista `pendientes` del 409 LEVANTE_REQUISITOS_PENDIENTES; null si el error es otro. */
function requisitosPendientes(error: unknown): string[] | null {
  const { status, body } = leerError(error)
  if (status !== 409 || body?.codigo !== 'LEVANTE_REQUISITOS_PENDIENTES') return null
  return Array.isArray(body.pendientes) ? body.pendientes.map(String) : []
}

interface Props {
  despachoId: number
  estado: EstadoDespacho
  /** ADMIN y AGENTE registran; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Se llama tras registrar el levante, para actualizar el estado de la cabecera. */
  onLevanteRegistrado?: () => void
  /** «Registrar cita de retiro»: abre «Citas» con el tipo Retiro elegido (CA4). */
  onRegistrarRetiro?: () => void
}

/**
 * HU-13 · T-13.3: sección «Levante». El agente registra el levante que obtuvo en SUNAT Operaciones en Línea
 * con su número y fecha (CA1). Si falta el pago de tributos o la inspección conforme, el sistema advierte y
 * permite registrarlo de todas formas (CA2). La constancia en PDF es opcional (CA3) y, con el levante
 * otorgado, se habilita la cita de retiro (CA4). Contrato C8.
 */
export function LevanteDespacho({ despachoId, estado, puedeEditar, onLevanteRegistrado, onRegistrarRetiro }: Props) {
  const [levante, setLevante] = useState<Levante | null>(null)
  const [form, setForm] = useState<FormLevante>({ numeroLevante: '', fechaLevante: hoyIso() })
  const [constancia, setConstancia] = useState<File | null>(null)
  const [problemaConstancia, setProblemaConstancia] = useState<string | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [pendientes, setPendientes] = useState<string[] | null>(null)
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCargando(true)
    setError(null)
    obtenerLevante(despachoId)
      .then(setLevante)
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el levante del despacho.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  const registrado = levante?.numeroLevante != null

  function cambiar(campo: keyof FormLevante, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => {
      const restantes = { ...e }
      delete restantes[campo]
      return restantes
    })
    setMensaje(null)
  }

  function elegirConstancia(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = ''
    if (!archivo) return
    const problema = validarArchivoPdf(archivo)
    setProblemaConstancia(problema)
    setConstancia(problema ? null : archivo)
  }

  async function guardar(confirmarSinRequisitos: boolean) {
    setMensaje(null)
    setError(null)
    setEnviando(true)
    try {
      const guardado = await registrarLevante(despachoId, {
        numeroLevante: form.numeroLevante.trim(),
        fechaLevante: form.fechaLevante,
        confirmarSinRequisitos,
        constancia,
      })
      setLevante(guardado)
      setPendientes(null)
      setConstancia(null)
      setProblemaConstancia(null)
      setMensaje(
        guardado.conAdvertencia
          ? 'Levante registrado con advertencia: quedó anotado en la línea de tiempo que faltaban requisitos previos.'
          : 'Levante registrado. El despacho pasó a «Levante otorgado» y ya se puede programar el retiro.',
      )
      onLevanteRegistrado?.()
    } catch (e) {
      const faltan = requisitosPendientes(e)
      const { status, body } = leerError(e)
      if (faltan) setPendientes(faltan)
      else if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeEnvio(e))
    } finally {
      setEnviando(false)
    }
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const nuevos = validarLevante(form, hoyIso())
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    void guardar(false)
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando levante" />
      </Box>
    )
  }

  const muestraFormulario = puedeEditar && !registrado && levante !== null && alcanzoEstado(estado, 'CANAL_ASIGNADO')

  return (
    <Box>
      <TituloSeccion>Levante de la mercancía</TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert severity="info">
          El levante se obtiene en SUNAT Operaciones en Línea; aquí se deja constancia del número, la fecha y el
          documento de autorización.
        </Alert>
        {mensaje && <Alert severity={levante?.conAdvertencia ? 'warning' : 'success'}>{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {!registrado && levante !== null && !puedeEditar && (
          <Alert severity="info">Todavía no se registró el levante de este despacho.</Alert>
        )}
        {!registrado && levante !== null && puedeEditar && !alcanzoEstado(estado, 'CANAL_ASIGNADO') && (
          <Alert severity="info">El levante se registra cuando el despacho tiene el canal de control asignado.</Alert>
        )}
      </Stack>

      {registrado && levante && (
        <Box aria-label="Levante registrado">
          <Box sx={{ ...DOS_COLUMNAS, mb: 2 }}>
            <Dato titulo="N° de levante / código de autorización SUNAT">{levante.numeroLevante}</Dato>
            <Dato titulo="Fecha del levante">{formatoFecha(levante.fechaLevante)}</Dato>
            <Dato titulo="Constancia de levante">
              {levante.constanciaNombre ? (
                <Link href={urlConstanciaLevante(despachoId)} target="_blank" rel="noopener">
                  Descargar constancia · {levante.constanciaNombre}
                </Link>
              ) : (
                'Sin constancia adjunta'
              )}
            </Dato>
            <Dato titulo="Requisitos previos">
              {levante.conAdvertencia ? (
                <EtiquetaColor texto="Registrado con advertencia" tono="naranja" />
              ) : (
                <EtiquetaColor texto="Cumplidos" tono="verde" />
              )}
            </Dato>
          </Box>
          {puedeEditar && levante.retiroHabilitado && onRegistrarRetiro && (
            <Button variant="contained" size="small" onClick={onRegistrarRetiro}>
              Registrar cita de retiro
            </Button>
          )}
        </Box>
      )}

      {muestraFormulario && (
        <Paper component="form" onSubmit={enviar} noValidate variant="outlined" sx={{ p: 2 }}>
          <Typography component="h3" sx={{ fontSize: 14, fontWeight: 700, mb: 1.5 }}>
            Registrar levante otorgado
          </Typography>
          <Box sx={DOS_COLUMNAS}>
            <TextField
              label="N° de levante / código de autorización SUNAT"
              value={form.numeroLevante}
              onChange={(e) => cambiar('numeroLevante', e.target.value)}
              error={Boolean(errores.numeroLevante)}
              helperText={errores.numeroLevante}
              slotProps={{ htmlInput: { maxLength: MAX_NUMERO_LEVANTE } }}
              required
            />
            <TextField
              label="Fecha del levante"
              type="date"
              value={form.fechaLevante}
              onChange={(e) => cambiar('fechaLevante', e.target.value)}
              error={Boolean(errores.fechaLevante)}
              helperText={errores.fechaLevante}
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoyIso() } }}
              required
            />
            <Box sx={{ gridColumn: '1 / -1' }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
                <Button component="label" variant="outlined" size="small" startIcon={<UploadFileIcon />}>
                  Adjuntar constancia de levante (PDF)
                  <input
                    hidden
                    type="file"
                    accept="application/pdf"
                    onChange={elegirConstancia}
                    aria-label="Archivo de la constancia de levante"
                  />
                </Button>
                <Typography
                  variant="body2"
                  role="status"
                  sx={{ fontSize: 12.5 }}
                  color={problemaConstancia || errores.constancia ? 'error' : 'text.secondary'}
                >
                  {problemaConstancia ||
                    errores.constancia ||
                    (constancia
                      ? `${constancia.name} · ${formatoTamano(constancia.size)}`
                      : 'Opcional. Solo PDF, máximo 10 MB')}
                </Typography>
              </Stack>
            </Box>
          </Box>
          <Box sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" disabled={enviando}>
              {enviando ? 'Guardando…' : 'Registrar levante'}
            </Button>
          </Box>
        </Paper>
      )}

      <Dialog open={pendientes !== null} onClose={enviando ? undefined : () => setPendientes(null)}>
        <DialogTitle>Requisitos previos pendientes</DialogTitle>
        <DialogContent>
          <DialogContentText component="div">
            Antes del levante deberían estar cumplidos estos requisitos:
            <Box component="ul" aria-label="Requisitos pendientes" sx={{ mt: 1, mb: 1 }}>
              {(pendientes ?? []).map((p) => (
                <li key={p}>{p}</li>
              ))}
            </Box>
            Si registra el levante de todas formas, quedará marcado con advertencia en la línea de tiempo.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPendientes(null)} disabled={enviando}>
            Cancelar
          </Button>
          <Button variant="contained" color="warning" onClick={() => void guardar(true)} disabled={enviando}>
            {enviando ? 'Guardando…' : 'Registrar de todas formas'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

function Dato({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <Box>
      <Typography component="div" sx={{ fontSize: 12.5, fontWeight: 600, color: '#334155', mb: 0.5 }}>
        {titulo}
      </Typography>
      <Typography component="div">{children}</Typography>
    </Box>
  )
}

/** 413 y 415 de la constancia (mismos códigos que los documentos); el resto, el mensaje del API (409 de estado o duplicado). */
function mensajeEnvio(error: unknown): string {
  const { status } = leerError(error)
  if (status === 413) return 'La constancia supera el tamaño máximo de 10 MB.'
  if (status === 415) return 'La constancia no es un PDF válido.'
  return mensajeDeError(error, 'No se pudo registrar el levante.')
}
