import UploadFileIcon from '@mui/icons-material/UploadFile'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link,
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
import { useEffect, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'

import { listarRectificacionesDam, obtenerDam, rectificarDam, registrarDam, urlCargoDam } from '../api/dam'
import { leerError, mensajeDeError } from '../api/http'
import type { Dam, EstadoDespacho, RectificacionDam } from '../types'
import { formatoFecha, formatoFechaHora, formatoTamano, hoyIso } from '../utils/formato'
import { validarArchivoPdf } from '../utils/validaciones'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

const MAX_NUMERO_DAM = 30
const MAX_MOTIVO = 200
const FORMATO_DAM = /^[A-Za-z0-9][A-Za-z0-9/-]*$/

/** Datos que el agente escribe; la fecha vacía deja que el API use la de hoy (o la actual al rectificar). */
export interface FormDam {
  numeroDam: string
  fechaNumeracion: string
  motivo: string
}

/** Mismas reglas que el backend (contrato C4): número obligatorio, hasta 30 caracteres, letras, números, guiones y barras. */
export function validarDam(form: FormDam, hoy: string, { rectifica }: { rectifica: boolean }): Record<string, string> {
  const errores: Record<string, string> = {}
  const numero = form.numeroDam.trim()
  if (!numero) errores.numeroDam = 'Ingrese el N° de DAM'
  else if (numero.length > MAX_NUMERO_DAM) errores.numeroDam = `Máximo ${MAX_NUMERO_DAM} caracteres`
  else if (!FORMATO_DAM.test(numero)) errores.numeroDam = 'Use solo letras, números, guiones y barras'
  if (form.fechaNumeracion && form.fechaNumeracion > hoy) {
    errores.fechaNumeracion = 'La fecha de numeración no puede ser futura'
  }
  if (rectifica) {
    const motivo = form.motivo.trim()
    if (!motivo) errores.motivo = 'Indique el motivo de la rectificación'
    else if (motivo.length > MAX_MOTIVO) errores.motivo = `Máximo ${MAX_MOTIVO} caracteres`
  }
  return errores
}

interface Props {
  despachoId: number
  estado: EstadoDespacho
  /** ADMIN y AGENTE registran y rectifican; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Se llama cuando el despacho pasa a DAM_NUMERADA, para actualizar la cabecera y las pestañas. */
  onDamNumerada?: () => void
}

/**
 * HU-09 · T-09.2: pestaña «DAM» para registrar a mano el número que el agente obtuvo en el sistema
 * externo de despacho (CA1 a CA4) y, según la regla de negocio 3, rectificarlo dejando un historial.
 * Contrato C4.
 */
export function NumeracionDam({ despachoId, estado, puedeEditar, onDamNumerada }: Props) {
  const [dam, setDam] = useState<Dam | null>(null)
  const [rectificaciones, setRectificaciones] = useState<RectificacionDam[]>([])
  const [cargando, setCargando] = useState(true)
  const [rectificando, setRectificando] = useState(false)
  const [form, setForm] = useState<FormDam>({ numeroDam: '', fechaNumeracion: hoyIso(), motivo: '' })
  const [cargo, setCargo] = useState<File | null>(null)
  const [problemaCargo, setProblemaCargo] = useState<string | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCargando(true)
    Promise.all([obtenerDam(despachoId), listarRectificacionesDam(despachoId)])
      .then(([actual, historial]) => {
        setDam(actual)
        setRectificaciones(historial)
      })
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar la numeración de la DAM.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  const registrada = dam?.numeroDam != null

  function cambiar(campo: keyof FormDam, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => {
      const restantes = { ...e }
      delete restantes[campo]
      return restantes
    })
    setMensaje(null)
  }

  function elegirCargo(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    e.target.value = ''
    if (!archivo) return
    const problema = validarArchivoPdf(archivo)
    setProblemaCargo(problema)
    setCargo(problema ? null : archivo)
  }

  function abrirRectificacion() {
    setForm({
      numeroDam: dam?.numeroDam ?? '',
      fechaNumeracion: dam?.fechaNumeracion ?? '',
      motivo: '',
    })
    limpiarFormulario()
    setRectificando(true)
  }

  function limpiarFormulario() {
    setCargo(null)
    setProblemaCargo(null)
    setErrores({})
    setMensaje(null)
    setError(null)
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const nuevos = validarDam(form, hoyIso(), { rectifica: rectificando })
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    const solicitud = {
      numeroDam: form.numeroDam.trim(),
      fechaNumeracion: form.fechaNumeracion || null,
      cargo,
    }
    setEnviando(true)
    try {
      if (rectificando) {
        setDam(await rectificarDam(despachoId, { ...solicitud, motivo: form.motivo.trim() }))
        setRectificaciones(await listarRectificacionesDam(despachoId))
        setRectificando(false)
        setMensaje('DAM rectificada. El cambio quedó registrado en el historial.')
      } else {
        setDam(await registrarDam(despachoId, solicitud))
        setMensaje('Numeración de la DAM registrada. El despacho pasó a «DAM numerada».')
        onDamNumerada?.()
      }
      setCargo(null)
      setProblemaCargo(null)
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
        <CircularProgress aria-label="Cargando numeración de la DAM" />
      </Box>
    )
  }

  const muestraFormulario = puedeEditar && (rectificando || (!registrada && estado === 'TRIBUTOS_PAGADOS'))

  return (
    <Box>
      <TituloSeccion
        accion={
          puedeEditar &&
          registrada &&
          !rectificando && (
            <Button size="small" onClick={abrirRectificacion}>
              Rectificar DAM
            </Button>
          )
        }
      >
        Numeración de la DAM
      </TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert severity="info">
          La DAM se numera en el sistema externo de despacho aduanero; aquí solo se deja constancia del número oficial.
        </Alert>
        {mensaje && <Alert severity="success">{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {!registrada && !puedeEditar && <Alert severity="info">Todavía no se registró la numeración de la DAM.</Alert>}
      </Stack>

      {registrada && (
        <Box sx={{ ...DOS_COLUMNAS, mb: 3 }} aria-label="DAM registrada">
          <Dato titulo="N° de DAM">{dam.numeroDam}</Dato>
          <Dato titulo="Fecha de numeración">{formatoFecha(dam.fechaNumeracion)}</Dato>
          <Dato titulo="Cargo de numeración">
            {dam.nombreCargo ? (
              <Link href={urlCargoDam(despachoId)} target="_blank" rel="noopener">
                Descargar cargo · {dam.nombreCargo}
              </Link>
            ) : (
              'Sin cargo adjunto'
            )}
          </Dato>
        </Box>
      )}

      {muestraFormulario && (
        <Paper component="form" onSubmit={enviar} noValidate variant="outlined" sx={{ p: 2, mb: 3 }}>
          <Typography component="h3" sx={{ fontSize: 14, fontWeight: 700, mb: 1.5 }}>
            {rectificando ? 'Rectificar la numeración de la DAM' : 'Registrar numeración de la DAM'}
          </Typography>
          <Box sx={DOS_COLUMNAS}>
            <TextField
              label="N° de DAM (obtenido del sistema de despacho aduanero)"
              value={form.numeroDam}
              onChange={(e) => cambiar('numeroDam', e.target.value)}
              error={Boolean(errores.numeroDam)}
              helperText={errores.numeroDam}
              slotProps={{ htmlInput: { maxLength: MAX_NUMERO_DAM } }}
              required
            />
            <TextField
              label="Fecha de numeración"
              type="date"
              value={form.fechaNumeracion}
              onChange={(e) => cambiar('fechaNumeracion', e.target.value)}
              error={Boolean(errores.fechaNumeracion)}
              helperText={errores.fechaNumeracion}
              slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoyIso() } }}
            />
            {rectificando && (
              <Box sx={{ gridColumn: '1 / -1' }}>
                <TextField
                  label="Motivo de la rectificación"
                  value={form.motivo}
                  onChange={(e) => cambiar('motivo', e.target.value)}
                  error={Boolean(errores.motivo)}
                  helperText={errores.motivo}
                  slotProps={{ htmlInput: { maxLength: MAX_MOTIVO } }}
                  fullWidth
                  required
                />
              </Box>
            )}
            <Box sx={{ gridColumn: '1 / -1' }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
                <Button component="label" variant="outlined" size="small" startIcon={<UploadFileIcon />}>
                  Adjuntar cargo de numeración (PDF)
                  <input
                    hidden
                    type="file"
                    accept="application/pdf"
                    onChange={elegirCargo}
                    aria-label="Archivo del cargo de numeración"
                  />
                </Button>
                <Typography
                  variant="body2"
                  role="status"
                  sx={{ fontSize: 12.5 }}
                  color={problemaCargo || errores.cargo ? 'error' : 'text.secondary'}
                >
                  {problemaCargo ||
                    errores.cargo ||
                    (cargo
                      ? `${cargo.name} · ${formatoTamano(cargo.size)}`
                      : rectificando
                        ? 'Opcional. Si adjunta uno nuevo, reemplaza al cargo anterior.'
                        : 'Opcional. Solo PDF, máximo 10 MB')}
                </Typography>
              </Stack>
            </Box>
          </Box>
          <Stack direction="row" spacing={1.5} sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" disabled={enviando}>
              {enviando ? 'Guardando…' : rectificando ? 'Guardar rectificación' : 'Registrar numeración de DAM'}
            </Button>
            {rectificando && (
              <Button
                onClick={() => {
                  setRectificando(false)
                  limpiarFormulario()
                }}
                disabled={enviando}
              >
                Cancelar
              </Button>
            )}
          </Stack>
        </Paper>
      )}

      {registrada && (
        <>
          <TituloSeccion>Historial de rectificaciones</TituloSeccion>
          {rectificaciones.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              El número de DAM no se rectificó.
            </Typography>
          ) : (
            <TableContainer>
              <Table size="small" aria-label="Historial de rectificaciones de la DAM">
                <TableHead>
                  <TableRow>
                    <TableCell>Fecha y hora</TableCell>
                    <TableCell>N° anterior</TableCell>
                    <TableCell>N° nuevo</TableCell>
                    <TableCell>Fecha de numeración</TableCell>
                    <TableCell>Motivo</TableCell>
                    <TableCell>Usuario</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rectificaciones.map((r) => (
                    <TableRow key={`${r.fecha}-${r.numeroNuevo}`}>
                      <TableCell>{formatoFechaHora(r.fecha)}</TableCell>
                      <TableCell>{r.numeroAnterior}</TableCell>
                      <TableCell>{r.numeroNuevo}</TableCell>
                      <TableCell>
                        {r.fechaAnterior === r.fechaNueva
                          ? formatoFecha(r.fechaNueva)
                          : `${formatoFecha(r.fechaAnterior)} → ${formatoFecha(r.fechaNueva)}`}
                      </TableCell>
                      <TableCell>{r.motivo}</TableCell>
                      <TableCell>{r.usuario}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </>
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

/** 413 y 415 del cargo (mismos códigos que los documentos); el resto, el mensaje del API (409 DAM_DUPLICADA, etc.). */
function mensajeEnvio(error: unknown): string {
  const { status } = leerError(error)
  if (status === 413) return 'El cargo supera el tamaño máximo de 10 MB.'
  if (status === 415) return 'El cargo no es un PDF válido.'
  return mensajeDeError(error, 'No se pudo guardar la numeración de la DAM.')
}
