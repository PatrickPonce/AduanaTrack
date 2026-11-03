import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Chip,
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
import { useEffect, useState, type FormEvent } from 'react'

import {
  listarCitas,
  listarDepositos,
  registrarCita,
  type Cita,
  type CitaRequest,
  type Deposito,
  type EstadoCita,
  type TipoCita,
} from '../api/citas'
import { leerError, mensajeDeError } from '../api/http'
import type { EstadoDespacho } from '../types'
import { alcanzoEstado } from '../utils/estados'
import { formatoFechaHora } from '../utils/formato'
import { validarRuc } from '../utils/validaciones'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

const MAX_NUMERO_CITA = 30
const MAX_RAZON_SOCIAL = 150
const MAX_PLACA = 10
const MAX_CONDUCTOR = 100
const FORMATO_PLACA = /^[A-Z0-9][A-Z0-9-]*$/

export const TIPOS_CITA: Record<TipoCita, string> = {
  INSPECCION: 'Inspección',
  RETIRO: 'Retiro de mercancía',
}

const ESTADOS_CITA: Record<EstadoCita, { texto: string; color: 'info' | 'success' | 'default' }> = {
  PROGRAMADA: { texto: 'Programada', color: 'info' },
  REALIZADA: { texto: 'Realizada', color: 'success' },
  CANCELADA: { texto: 'Cancelada', color: 'default' },
}

/** Datos que el agente escribe; la fecha y hora salen del campo datetime-local (AAAA-MM-DDTHH:mm). */
export interface FormCita {
  deposito: Deposito | null
  numeroCita: string
  tipo: TipoCita | ''
  fechaHoraConfirmada: string
  ruc: string
  razonSocial: string
  placa: string
  conductor: string
}

function formVacio(tipo: TipoCita | ''): FormCita {
  return {
    deposito: null,
    numeroCita: '',
    tipo,
    fechaHoraConfirmada: '',
    ruc: '',
    razonSocial: '',
    placa: '',
    conductor: '',
  }
}

/**
 * Tipos de cita que el despacho admite ahora (contrato C6): la inspección en canal naranja o rojo, desde
 * CANAL_ASIGNADO y mientras no haya una inspección conforme (CA2); el retiro desde LEVANTE_OTORGADO (CA3).
 */
export function tiposPermitidos(estado: EstadoDespacho, canal: string | null): TipoCita[] {
  const tipos: TipoCita[] = []
  const requiereInspeccion = canal === 'NARANJA' || canal === 'ROJO'
  if (requiereInspeccion && (estado === 'CANAL_ASIGNADO' || estado === 'INSPECCION_PROGRAMADA')) {
    tipos.push('INSPECCION')
  }
  if (alcanzoEstado(estado, 'LEVANTE_OTORGADO')) tipos.push('RETIRO')
  return tipos
}

/** El tipo pedido (p. ej. RETIRO desde el levante) si está permitido; si no, el primero que lo esté. */
function tipoPorDefecto(permitidos: TipoCita[], pedido?: TipoCita): TipoCita | '' {
  return pedido && permitidos.includes(pedido) ? pedido : (permitidos[0] ?? '')
}

/**
 * Mismas reglas que el backend, con las mismas claves de error: obligatorios (la fecha y hora es el CA4),
 * N° de cita de hasta 30 caracteres y, en el retiro, los cuatro datos del transportista con el RUC
 * validado por su dígito verificador.
 */
export function validarCita(form: FormCita): Record<string, string> {
  const errores: Record<string, string> = {}
  if (!form.deposito) errores.depositoId = 'Seleccione el depósito o almacén'
  const numero = form.numeroCita.trim()
  if (!numero) errores.numeroCita = 'Ingrese el N° de cita o ticket'
  else if (numero.length > MAX_NUMERO_CITA) errores.numeroCita = `Máximo ${MAX_NUMERO_CITA} caracteres`
  if (!form.tipo) errores.tipo = 'Seleccione el tipo de cita'
  if (!form.fechaHoraConfirmada) errores.fechaHoraConfirmada = 'Ingrese la fecha y hora confirmada de la cita'
  if (form.tipo === 'RETIRO') {
    const ruc = form.ruc.trim()
    if (!ruc) errores['transportista.ruc'] = 'Ingrese el RUC del transportista'
    else if (validarRuc(ruc)) errores['transportista.ruc'] = 'RUC no válido'
    const razonSocial = form.razonSocial.trim()
    if (!razonSocial) errores['transportista.razonSocial'] = 'Ingrese la razón social del transportista'
    else if (razonSocial.length > MAX_RAZON_SOCIAL) {
      errores['transportista.razonSocial'] = `Máximo ${MAX_RAZON_SOCIAL} caracteres`
    }
    const placa = form.placa.trim().toUpperCase()
    if (!placa) errores['transportista.placa'] = 'Ingrese la placa del vehículo'
    else if (placa.length > MAX_PLACA || !FORMATO_PLACA.test(placa)) {
      errores['transportista.placa'] = 'Placa no válida (letras, números y guion)'
    }
    const conductor = form.conductor.trim()
    if (!conductor) errores['transportista.conductor'] = 'Ingrese el nombre del conductor'
    else if (conductor.length > MAX_CONDUCTOR) errores['transportista.conductor'] = `Máximo ${MAX_CONDUCTOR} caracteres`
  }
  return errores
}

/** El campo datetime-local entrega minutos; el API espera también los segundos. */
export function aSolicitud(form: FormCita): CitaRequest {
  const fecha = form.fechaHoraConfirmada
  return {
    depositoId: form.deposito!.id,
    numeroCita: form.numeroCita.trim(),
    tipo: form.tipo as TipoCita,
    fechaHoraConfirmada: fecha.length === 16 ? `${fecha}:00` : fecha,
    transportista:
      form.tipo === 'RETIRO'
        ? {
            ruc: form.ruc.trim(),
            razonSocial: form.razonSocial.trim(),
            placa: form.placa.trim().toUpperCase(),
            conductor: form.conductor.trim(),
          }
        : null,
  }
}

/** Por qué no se puede registrar ninguna cita todavía, según el estado y el canal del despacho. */
function motivoSinCitas(estado: EstadoDespacho, canal: string | null): string {
  if (!alcanzoEstado(estado, 'CANAL_ASIGNADO')) {
    return 'Las citas se registran cuando el despacho tiene el canal de control asignado.'
  }
  if (canal === 'VERDE') {
    return 'El canal verde no requiere inspección: la cita de retiro se registra después del levante.'
  }
  return 'La inspección ya fue conforme: la cita de retiro se registra después del levante.'
}

interface Props {
  despachoId: number
  estado: EstadoDespacho
  /** Canal de control del despacho (VERDE, NARANJA o ROJO); decide si hay cita de inspección. */
  canal: string | null
  /** ADMIN y AGENTE registran; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Tipo con el que se abre el formulario, p. ej. RETIRO al llegar desde «Registrar cita de retiro». */
  tipoInicial?: TipoCita
  /** Se llama tras registrar una cita, para actualizar el estado de la cabecera. */
  onCitaRegistrada?: () => void
}

/**
 * HU-11 · T-11.3: pestaña «Citas». El agente registra la cita que ya coordinó en la plataforma del
 * depósito temporal o almacén (CA1): la de inspección deja el despacho en INSPECCION_PROGRAMADA (CA2) y la
 * de retiro guarda los datos del transportista (CA3). Sin fecha y hora confirmada no se guarda (CA4).
 * Contrato C6.
 */
export function CitasDespacho({ despachoId, estado, canal, puedeEditar, tipoInicial, onCitaRegistrada }: Props) {
  const permitidos = tiposPermitidos(estado, canal)

  const [depositos, setDepositos] = useState<Deposito[]>([])
  const [citas, setCitas] = useState<Cita[]>([])
  const [form, setForm] = useState<FormCita>(() => formVacio(tipoPorDefecto(permitidos, tipoInicial)))
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCargando(true)
    setError(null)
    Promise.all([listarDepositos(), listarCitas(despachoId)])
      .then(([catalogo, registradas]) => {
        setDepositos(catalogo)
        setCitas(registradas)
      })
      .catch((e) => setError(mensajeDeError(e, 'No se pudieron cargar las citas del despacho.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  // Si el despacho avanza (p. ej. llega el levante) el tipo elegido puede dejar de estar permitido.
  useEffect(() => {
    const tipos = tiposPermitidos(estado, canal)
    setForm((f) => (f.tipo && tipos.includes(f.tipo) ? f : { ...f, tipo: tipoPorDefecto(tipos, tipoInicial) }))
  }, [estado, canal, tipoInicial])

  function cambiar<K extends keyof FormCita>(campo: K, valor: FormCita[K], claveError: string = campo) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => {
      const restantes = { ...e }
      delete restantes[claveError]
      return restantes
    })
    setMensaje(null)
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const nuevos = validarCita(form)
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setEnviando(true)
    try {
      const guardada = await registrarCita(despachoId, aSolicitud(form))
      setCitas(await listarCitas(despachoId))
      setForm(formVacio(form.tipo))
      setMensaje(
        `Cita de ${TIPOS_CITA[guardada.tipo].toLowerCase()} ${guardada.numeroCita} registrada para el ` +
          `${formatoFechaHora(guardada.fechaHoraConfirmada)} en ${guardada.deposito}.`,
      )
      onCitaRegistrada?.()
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeDeError(e, 'No se pudo registrar la cita.'))
    } finally {
      setEnviando(false)
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando citas" />
      </Box>
    )
  }

  const muestraFormulario = puedeEditar && permitidos.length > 0

  const texto = (
    campo: 'numeroCita' | 'ruc' | 'razonSocial' | 'placa' | 'conductor',
    label: string,
    claveError: string,
    maximo: number,
  ) => (
    <TextField
      label={label}
      value={form[campo]}
      onChange={(e) => {
        const valor = campo === 'ruc' ? e.target.value.replace(/\D/g, '').slice(0, 11) : e.target.value
        cambiar(campo, valor, claveError)
      }}
      error={Boolean(errores[claveError])}
      helperText={errores[claveError]}
      slotProps={{ htmlInput: { maxLength: maximo, ...(campo === 'ruc' && { inputMode: 'numeric' }) } }}
      required
    />
  )

  return (
    <Box>
      <TituloSeccion>Citas en depósito temporal o almacén</TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert severity="info">
          Registre aquí la cita que ya coordinó en la plataforma del depósito o almacén, para tener su trazabilidad
          dentro del despacho.
        </Alert>
        {mensaje && <Alert severity="success">{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {puedeEditar && permitidos.length === 0 && <Alert severity="info">{motivoSinCitas(estado, canal)}</Alert>}
      </Stack>

      {muestraFormulario && (
        <Paper component="form" onSubmit={enviar} noValidate variant="outlined" sx={{ p: 2, mb: 3 }}>
          <Typography component="h3" sx={{ fontSize: 14, fontWeight: 700, mb: 1.5 }}>
            Registrar cita
          </Typography>
          <Box sx={DOS_COLUMNAS}>
            <Autocomplete
              value={form.deposito}
              options={depositos}
              getOptionLabel={(d) => `${d.codigo} · ${d.nombre}`}
              isOptionEqualToValue={(a, b) => a.id === b.id}
              noOptionsText="Sin depósitos"
              onChange={(_, deposito) => cambiar('deposito', deposito, 'depositoId')}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Depósito / Almacén"
                  required
                  error={Boolean(errores.depositoId)}
                  helperText={errores.depositoId}
                />
              )}
            />
            {texto('numeroCita', 'N° de cita / ticket', 'numeroCita', MAX_NUMERO_CITA)}
            <TextField
              select
              label="Tipo de cita"
              value={form.tipo}
              onChange={(e) => cambiar('tipo', e.target.value as TipoCita)}
              error={Boolean(errores.tipo)}
              helperText={
                errores.tipo ||
                (permitidos.includes('RETIRO') ? undefined : 'El retiro de mercancía se habilita después del levante')
              }
              required
            >
              {(Object.keys(TIPOS_CITA) as TipoCita[]).map((tipo) => (
                <MenuItem key={tipo} value={tipo} disabled={!permitidos.includes(tipo)}>
                  {TIPOS_CITA[tipo]}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Fecha y hora confirmada"
              type="datetime-local"
              value={form.fechaHoraConfirmada}
              onChange={(e) => cambiar('fechaHoraConfirmada', e.target.value)}
              error={Boolean(errores.fechaHoraConfirmada)}
              helperText={errores.fechaHoraConfirmada}
              slotProps={{ inputLabel: { shrink: true } }}
              required
            />
          </Box>

          {form.tipo === 'RETIRO' && (
            <Box sx={{ mt: 2.5 }} aria-label="Datos del transportista">
              <Typography component="h4" sx={{ fontSize: 13.5, fontWeight: 700, mb: 1.5 }}>
                Datos del transportista
              </Typography>
              <Box sx={DOS_COLUMNAS}>
                {texto('ruc', 'RUC del transportista', 'transportista.ruc', 11)}
                {texto('razonSocial', 'Razón social del transportista', 'transportista.razonSocial', MAX_RAZON_SOCIAL)}
                {texto('placa', 'Placa del vehículo', 'transportista.placa', MAX_PLACA)}
                {texto('conductor', 'Nombre del conductor', 'transportista.conductor', MAX_CONDUCTOR)}
              </Box>
            </Box>
          )}

          <Box sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" disabled={enviando}>
              {enviando ? 'Guardando…' : 'Registrar cita'}
            </Button>
          </Box>
        </Paper>
      )}

      <TituloSeccion>Citas registradas</TituloSeccion>
      {citas.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          Todavía no hay citas registradas para este despacho.
        </Typography>
      ) : (
        <TableContainer>
          <Table size="small" aria-label="Citas del despacho" sx={{ minWidth: 720 }}>
            <TableHead>
              <TableRow>
                <TableCell>Fecha y hora</TableCell>
                <TableCell>Depósito / Almacén</TableCell>
                <TableCell>N° de cita</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Transportista</TableCell>
                <TableCell>Estado</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {citas.map((c) => (
                <TableRow key={c.id}>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(c.fechaHoraConfirmada)}</TableCell>
                  <TableCell>{c.deposito}</TableCell>
                  <TableCell>{c.numeroCita}</TableCell>
                  <TableCell>{TIPOS_CITA[c.tipo] ?? c.tipo}</TableCell>
                  <TableCell>
                    {c.transportista ? (
                      <>
                        {c.transportista.razonSocial}
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                          Placa {c.transportista.placa} · {c.transportista.conductor}
                        </Typography>
                      </>
                    ) : (
                      '—'
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={ESTADOS_CITA[c.estado]?.texto ?? c.estado}
                      color={ESTADOS_CITA[c.estado]?.color ?? 'default'}
                      size="small"
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
