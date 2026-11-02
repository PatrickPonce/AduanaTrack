import UploadFileIcon from '@mui/icons-material/UploadFile'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  FormControlLabel,
  Link,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'
import { Link as RouterLink } from 'react-router'

import { leerError, mensajeDeError } from '../api/http'
import {
  listarItemsInspeccion,
  obtenerUltimaInspeccion,
  registrarInspeccion,
  urlActaInspeccion,
  type InspeccionRegistrada,
  type ItemInspeccion,
  type ResultadoInspeccion,
} from '../api/inspeccion'
import type { EstadoDespacho } from '../types'
import { formatoFechaHora, formatoTamano } from '../utils/formato'
import { validarArchivoPdf } from '../utils/validaciones'
import { EtiquetaColor } from './EtiquetaColor'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

export const MAX_OBSERVACION_ITEM = 255
export const MAX_OBSERVACIONES = 500

/** Fila del checklist en pantalla: la observación se edita como texto y se envía nula si el ítem es conforme. */
export interface FilaChecklist {
  descripcion: string
  conforme: boolean
  observacion: string
}

export function filasIniciales(descripciones: string[]): FilaChecklist[] {
  return descripciones.map((descripcion) => ({ descripcion, conforme: true, observacion: '' }))
}

/** CA1: el resultado lo decide el sistema; en pantalla se calcula con la misma regla (todos conformes). */
export function resultadoDe(filas: Pick<FilaChecklist, 'conforme'>[]): ResultadoInspeccion {
  return filas.every((f) => f.conforme) ? 'CONFORME' : 'CON_OBSERVACIONES'
}

/** Un ítem que no es conforme necesita su observación (contrato C7); devuelve el error por posición de fila. */
export function validarChecklist(filas: FilaChecklist[], observaciones: string): Record<string, string> {
  const errores: Record<string, string> = {}
  filas.forEach((fila, i) => {
    const texto = fila.observacion.trim()
    if (!fila.conforme && !texto) errores[`item-${i}`] = 'Indique la observación del ítem'
    else if (texto.length > MAX_OBSERVACION_ITEM) errores[`item-${i}`] = `Máximo ${MAX_OBSERVACION_ITEM} caracteres`
  })
  if (observaciones.trim().length > MAX_OBSERVACIONES) {
    errores.observaciones = `Máximo ${MAX_OBSERVACIONES} caracteres`
  }
  return errores
}

function items(filas: FilaChecklist[]): ItemInspeccion[] {
  return filas.map((f) => ({
    descripcion: f.descripcion,
    conforme: f.conforme,
    observacion: f.conforme ? null : f.observacion.trim(),
  }))
}

function ResultadoChip({ resultado }: { resultado: ResultadoInspeccion }) {
  return resultado === 'CONFORME' ? (
    <EtiquetaColor texto="Conforme" tono="verde" />
  ) : (
    <EtiquetaColor texto="Con observaciones" tono="naranja" />
  )
}

interface Props {
  despachoId: number
  estado: EstadoDespacho
  /** ADMIN y AGENTE registran; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Se llama tras registrar la inspección, para actualizar el estado de la cabecera. */
  onInspeccionRegistrada?: () => void
}

/**
 * HU-12 · T-12.3: pestaña «Inspección». El agente marca cada ítem verificado en la inspección física; el
 * resultado sale de los ítems (CA1) y, con observaciones, el sistema avisa al agente (CA2). Una inspección
 * conforme habilita el levante (CA3) y el acta en PDF es opcional (CA4). Contrato C7.
 */
export function InspeccionDespacho({ despachoId, estado, puedeEditar, onInspeccionRegistrada }: Props) {
  const [filas, setFilas] = useState<FilaChecklist[]>([])
  const [registrada, setRegistrada] = useState<InspeccionRegistrada | null>(null)
  const [observaciones, setObservaciones] = useState('')
  const [acta, setActa] = useState<File | null>(null)
  const [problemaActa, setProblemaActa] = useState<string | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [nueva, setNueva] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCargando(true)
    setError(null)
    Promise.all([listarItemsInspeccion(), obtenerUltimaInspeccion(despachoId)])
      .then(([catalogo, ultima]) => {
        setFilas(filasIniciales(catalogo))
        setRegistrada(ultima)
      })
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar la inspección.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  const enInspeccion = estado === 'INSPECCION_PROGRAMADA'
  // Con la inspección programada el formulario está abierto; si ya hubo una con observaciones se vuelve a
  // abrir con «Registrar nueva inspección». En cualquier otro estado la pestaña es de solo lectura.
  const muestraFormulario = puedeEditar && enInspeccion && (registrada === null || nueva)
  const resultado = resultadoDe(filas)

  function cambiarFila(posicion: number, cambio: Partial<FilaChecklist>) {
    setFilas((actuales) => actuales.map((f, i) => (i === posicion ? { ...f, ...cambio } : f)))
    setErrores((e) => {
      const restantes = { ...e }
      delete restantes[`item-${posicion}`]
      return restantes
    })
    setMensaje(null)
  }

  function elegirActa(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = ''
    if (!archivo) return
    const problema = validarArchivoPdf(archivo)
    setProblemaActa(problema)
    setActa(problema ? null : archivo)
  }

  function abrirNueva() {
    setFilas((actuales) => filasIniciales(actuales.map((f) => f.descripcion)))
    setObservaciones('')
    setActa(null)
    setProblemaActa(null)
    setErrores({})
    setMensaje(null)
    setError(null)
    setNueva(true)
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const nuevos = validarChecklist(filas, observaciones)
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setEnviando(true)
    try {
      const guardada = await registrarInspeccion(despachoId, {
        items: items(filas),
        observaciones: observaciones.trim() || null,
        acta,
      })
      setRegistrada(guardada)
      setNueva(false)
      setActa(null)
      setProblemaActa(null)
      setMensaje(
        guardada.resultado === 'CONFORME'
          ? 'Inspección conforme registrada. El levante quedó habilitado.'
          : 'Inspección con observaciones registrada. Se avisó al agente responsable del despacho.',
      )
      onInspeccionRegistrada?.()
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeEnvio(e))
    } finally {
      setEnviando(false)
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando inspección" />
      </Box>
    )
  }

  return (
    <Box>
      <TituloSeccion
        accion={
          puedeEditar &&
          enInspeccion &&
          registrada !== null &&
          !nueva && (
            <Button size="small" onClick={abrirNueva}>
              Registrar nueva inspección
            </Button>
          )
        }
      >
        Resultado de la inspección aduanera
      </TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert severity="info">
          Registre lo que se verificó en la inspección física. Si algún ítem no es conforme, el resultado queda «Con
          observaciones» y el levante no se habilita hasta una nueva inspección conforme.
        </Alert>
        {mensaje && <Alert severity="success">{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {registrada === null && !puedeEditar && (
          <Alert severity="info">Todavía no se registró el resultado de la inspección.</Alert>
        )}
        {registrada === null && puedeEditar && !enInspeccion && (
          <Alert severity="info">La inspección se registra cuando el despacho tiene una cita de inspección programada.</Alert>
        )}
      </Stack>

      {registrada && !nueva && (
        <Box sx={{ mb: 3 }} aria-label="Inspección registrada">
          <Box sx={{ ...DOS_COLUMNAS, mb: 2 }}>
            <Dato titulo="Resultado de la inspección">
              <ResultadoChip resultado={registrada.resultado} />
            </Dato>
            <Dato titulo="Acta de inspección">
              {registrada.actaNombre ? (
                <Link href={urlActaInspeccion(despachoId)} target="_blank" rel="noopener">
                  Descargar acta · {registrada.actaNombre}
                </Link>
              ) : (
                'Sin acta adjunta'
              )}
            </Dato>
            {registrada.registradaEn && (
              <Dato titulo="Registrada">
                {formatoFechaHora(registrada.registradaEn)}
                {registrada.registradaPor ? ` · ${registrada.registradaPor}` : ''}
              </Dato>
            )}
            {registrada.observaciones && <Dato titulo="Observaciones generales">{registrada.observaciones}</Dato>}
          </Box>

          {registrada.items && registrada.items.length > 0 && (
            <TableContainer sx={{ mb: 2 }}>
              <Table size="small" aria-label="Ítems verificados">
                <TableHead>
                  <TableRow>
                    <TableCell>Ítem verificado</TableCell>
                    <TableCell sx={{ width: 140 }}>Resultado</TableCell>
                    <TableCell>Observación</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {registrada.items.map((item) => (
                    <TableRow key={item.descripcion}>
                      <TableCell>{item.descripcion}</TableCell>
                      <TableCell>
                        <EtiquetaColor
                          texto={item.conforme ? 'Conforme' : 'No conforme'}
                          tono={item.conforme ? 'verde' : 'rojo'}
                        />
                      </TableCell>
                      <TableCell>{item.observacion ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {registrada.resultado === 'CONFORME' && puedeEditar && (
            <Button component={RouterLink} to={`/levante/${despachoId}`} variant="contained" size="small">
              Ir a levante
            </Button>
          )}
        </Box>
      )}

      {muestraFormulario && (
        <Paper component="form" onSubmit={enviar} noValidate variant="outlined" sx={{ p: 2 }}>
          <Typography component="h3" sx={{ fontSize: 14, fontWeight: 700, mb: 1.5 }}>
            Ítems verificados
          </Typography>

          <Stack spacing={1.5} sx={{ mb: 2.5 }}>
            {filas.map((fila, i) => (
              <Box key={fila.descripcion}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={fila.conforme}
                      onChange={(e) => cambiarFila(i, { conforme: e.target.checked })}
                      slotProps={{ input: { 'aria-label': `Conforme: ${fila.descripcion}` } }}
                    />
                  }
                  label={
                    <Typography component="span" sx={{ fontSize: 14 }}>
                      {fila.descripcion}
                    </Typography>
                  }
                />
                {!fila.conforme && (
                  <TextField
                    label={`Observación: ${fila.descripcion}`}
                    value={fila.observacion}
                    onChange={(e) => cambiarFila(i, { observacion: e.target.value })}
                    error={Boolean(errores[`item-${i}`])}
                    helperText={errores[`item-${i}`]}
                    slotProps={{ htmlInput: { maxLength: MAX_OBSERVACION_ITEM } }}
                    size="small"
                    fullWidth
                    required
                    sx={{ mt: 1 }}
                  />
                )}
              </Box>
            ))}
          </Stack>

          <Box sx={DOS_COLUMNAS}>
            <Dato titulo="Resultado de la inspección">
              <Box role="status" aria-label="Resultado calculado">
                <ResultadoChip resultado={resultado} />
              </Box>
            </Dato>
            <Box sx={{ gridColumn: '1 / -1' }}>
              <TextField
                label="Observaciones generales"
                value={observaciones}
                onChange={(e) => {
                  setObservaciones(e.target.value)
                  setErrores((previos) => ({ ...previos, observaciones: '' }))
                }}
                error={Boolean(errores.observaciones)}
                helperText={errores.observaciones || 'Opcional'}
                slotProps={{ htmlInput: { maxLength: MAX_OBSERVACIONES } }}
                multiline
                minRows={2}
                fullWidth
              />
            </Box>
            <Box sx={{ gridColumn: '1 / -1' }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
                <Button component="label" variant="outlined" size="small" startIcon={<UploadFileIcon />}>
                  Adjuntar acta de inspección (PDF)
                  <input
                    hidden
                    type="file"
                    accept="application/pdf"
                    onChange={elegirActa}
                    aria-label="Archivo del acta de inspección"
                  />
                </Button>
                <Typography
                  variant="body2"
                  role="status"
                  sx={{ fontSize: 12.5 }}
                  color={problemaActa || errores.acta ? 'error' : 'text.secondary'}
                >
                  {problemaActa ||
                    errores.acta ||
                    (acta ? `${acta.name} · ${formatoTamano(acta.size)}` : 'Opcional. Solo PDF, máximo 10 MB')}
                </Typography>
              </Stack>
            </Box>
          </Box>

          <Stack direction="row" spacing={1.5} sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" disabled={enviando}>
              {enviando ? 'Guardando…' : 'Registrar resultado de inspección'}
            </Button>
            {nueva && (
              <Button onClick={() => setNueva(false)} disabled={enviando}>
                Cancelar
              </Button>
            )}
          </Stack>
        </Paper>
      )}
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

/** 413 y 415 del acta (mismos códigos que los documentos); el resto, el mensaje del API (p. ej. 409 de estado). */
function mensajeEnvio(error: unknown): string {
  const { status } = leerError(error)
  if (status === 413) return 'El acta supera el tamaño máximo de 10 MB.'
  if (status === 415) return 'El acta no es un PDF válido.'
  return mensajeDeError(error, 'No se pudo registrar la inspección.')
}
