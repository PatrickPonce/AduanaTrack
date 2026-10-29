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
  TableRow,
  TextField,
} from '@mui/material'
import { useEffect, useState, type FormEvent } from 'react'

import { leerError, mensajeDeError } from '../api/http'
import { generarLiquidacion, registrarPago } from '../api/liquidacion'
import type { EstadoDespacho, Liquidacion, PagoRequest } from '../types'
import { alcanzoEstado } from '../utils/estados'
import { formatoMonto, hoyIso } from '../utils/formato'
import { MEDIOS_PAGO } from '../utils/opciones'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

/** Valores que el agente escribe en el formulario de pago; el monto se edita como texto. */
export interface FormPago {
  fecha: string
  monto: string
  medioPago: string
  numeroOperacion: string
}

const MONTO = /^\d+(\.\d{1,2})?$/

function montoTexto(valor: string): string {
  return valor.trim().replace(',', '.')
}

/** Mismas reglas que LiquidacionRequest (contrato C3): flete y seguro obligatorios, ≥ 0 y con 2 decimales. */
export function validarLiquidacion(flete: string, seguro: string): Record<string, string> {
  const errores: Record<string, string> = {}
  const comprobar = (campo: 'flete' | 'seguro', valor: string, nombre: string) => {
    const texto = montoTexto(valor)
    if (!texto) errores[campo] = `Ingrese el ${nombre} (USD)`
    else if (!MONTO.test(texto)) errores[campo] = `${nombre[0]!.toUpperCase()}${nombre.slice(1)} no válido (USD, máximo 2 decimales)`
  }
  comprobar('flete', flete, 'flete')
  comprobar('seguro', seguro, 'seguro')
  return errores
}

/** Mismas reglas que PagoRequest (contrato C3); `hoy` es AAAA-MM-DD y el pago no puede ser futuro. */
export function validarPago(form: FormPago, hoy: string): Record<string, string> {
  const errores: Record<string, string> = {}
  if (!form.fecha) errores.fecha = 'Ingrese la fecha del pago'
  else if (form.fecha > hoy) errores.fecha = 'La fecha del pago no puede ser futura'
  const monto = montoTexto(form.monto)
  if (!monto) errores.monto = 'Ingrese el monto pagado'
  else if (!MONTO.test(monto)) errores.monto = 'Monto no válido (USD, máximo 2 decimales)'
  else if (Number(monto) <= 0) errores.monto = 'El monto debe ser mayor a 0'
  if (!form.medioPago) errores.medioPago = 'Seleccione el medio de pago'
  if (!form.numeroOperacion.trim()) errores.numeroOperacion = 'Ingrese el número de operación'
  else if (form.numeroOperacion.trim().length > 50) errores.numeroOperacion = 'Máximo 50 caracteres'
  return errores
}

function aPago(form: FormPago): PagoRequest {
  return {
    fecha: form.fecha,
    monto: Number(montoTexto(form.monto)),
    medioPago: form.medioPago,
    numeroOperacion: form.numeroOperacion.trim(),
  }
}

interface Props {
  despachoId: number
  estado: EstadoDespacho
  /** ADMIN y AGENTE registran; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Se llama cuando el despacho pasa a LIQUIDADO o a TRIBUTOS_PAGADOS, para actualizar la cabecera y las pestañas. */
  onEstadoCambiado?: () => void
}

/**
 * HU-08 · T-08.3: pestaña «Liquidación» con el cálculo referencial de tributos (CA1 y CA2) y el
 * registro del pago hecho fuera del sistema (CA3 y CA4), según el contrato C3. El detalle de los
 * montos solo llega al generar la liquidación: no hay un GET que la vuelva a entregar.
 */
export function LiquidacionDespacho({ despachoId, estado, puedeEditar, onEstadoCambiado }: Props) {
  const [estadoActual, setEstadoActual] = useState(estado)
  const [flete, setFlete] = useState('')
  const [seguro, setSeguro] = useState('')
  const [liquidacion, setLiquidacion] = useState<Liquidacion | null>(null)
  const [pago, setPago] = useState<FormPago>({ fecha: hoyIso(), monto: '', medioPago: '', numeroOperacion: '' })
  const [erroresLiquidacion, setErroresLiquidacion] = useState<Record<string, string>>({})
  const [erroresPago, setErroresPago] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState<'liquidacion' | 'pago' | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => setEstadoActual(estado), [estado])

  const liquidado = estadoActual === 'LIQUIDADO'
  const pagado = alcanzoEstado(estadoActual, 'TRIBUTOS_PAGADOS')

  async function generar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const nuevos = validarLiquidacion(flete, seguro)
    setErroresLiquidacion(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setEnviando('liquidacion')
    try {
      const resultado = await generarLiquidacion(despachoId, {
        flete: Number(montoTexto(flete)),
        seguro: Number(montoTexto(seguro)),
      })
      setLiquidacion(resultado)
      setEstadoActual(resultado.estado)
      setPago((p) => ({ ...p, monto: resultado.total.toFixed(2) }))
      setMensaje('Liquidación referencial generada.')
      onEstadoCambiado?.()
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErroresLiquidacion(body.campos)
      else setError(mensajeDeError(e, 'No se pudo generar la liquidación.'))
    } finally {
      setEnviando(null)
    }
  }

  async function pagar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const nuevos = validarPago(pago, hoyIso())
    setErroresPago(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setEnviando('pago')
    try {
      const resultado = await registrarPago(despachoId, aPago(pago))
      setEstadoActual(resultado.estado)
      setMensaje('Pago de tributos registrado. El despacho pasó a «Tributos pagados».')
      onEstadoCambiado?.()
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErroresPago(body.campos)
      else setError(mensajeDeError(e, 'No se pudo registrar el pago.'))
    } finally {
      setEnviando(null)
    }
  }

  function cambiarPago(campo: keyof FormPago, valor: string) {
    setPago((p) => ({ ...p, [campo]: valor }))
    setErroresPago((e) => {
      const restantes = { ...e }
      delete restantes[campo]
      return restantes
    })
    setMensaje(null)
  }

  return (
    <Box>
      <TituloSeccion>Liquidación referencial de tributos</TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert severity="info">
          Cálculo referencial de la agencia, en USD. El monto oficial y el pago se gestionan en el banco o en SUNAT,
          fuera de este sistema.
        </Alert>
        {mensaje && <Alert severity="success">{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {pagado && !mensaje && (
          <Alert severity="success">Los tributos ya fueron pagados. El despacho está en «Tributos pagados».</Alert>
        )}
        {liquidado && !liquidacion && (
          <Alert severity="info">
            La liquidación ya fue generada. Para ver el detalle de los montos, genérela de nuevo con el mismo flete y
            seguro.
          </Alert>
        )}
        {!puedeEditar && !pagado && (
          <Alert severity="info">Solo el agente de aduana puede generar la liquidación y registrar el pago.</Alert>
        )}
      </Stack>

      {puedeEditar && !pagado && (
        <Paper component="form" onSubmit={generar} noValidate variant="outlined" sx={{ p: 2, mb: 3 }}>
          <Box sx={DOS_COLUMNAS}>
            <TextField
              label="Flete (USD)"
              value={flete}
              onChange={(e) => setFlete(e.target.value)}
              error={Boolean(erroresLiquidacion.flete)}
              helperText={erroresLiquidacion.flete}
              slotProps={{ htmlInput: { inputMode: 'decimal' } }}
              required
            />
            <TextField
              label="Seguro (USD)"
              value={seguro}
              onChange={(e) => setSeguro(e.target.value)}
              error={Boolean(erroresLiquidacion.seguro)}
              helperText={erroresLiquidacion.seguro}
              slotProps={{ htmlInput: { inputMode: 'decimal' } }}
              required
            />
          </Box>
          <Stack direction="row" sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" disabled={enviando !== null}>
              {enviando === 'liquidacion' ? 'Calculando…' : liquidacion || liquidado ? 'Volver a generar liquidación' : 'Generar liquidación'}
            </Button>
          </Stack>
        </Paper>
      )}

      {liquidacion && (
        <TableContainer sx={{ mb: 3 }}>
          <Table size="small" aria-label="Detalle de la liquidación">
            <TableBody>
              <Fila concepto="Valor FOB" monto={liquidacion.fob} />
              <Fila concepto="Flete" monto={liquidacion.flete} />
              <Fila concepto="Seguro" monto={liquidacion.seguro} />
              <Fila concepto="Valor CIF" monto={liquidacion.cif} destacada />
              <Fila concepto="Ad Valorem" monto={liquidacion.adValorem} />
              <Fila concepto={`IGV (${liquidacion.tasas.igv} %)`} monto={liquidacion.igv} />
              <Fila concepto={`IPM (${liquidacion.tasas.ipm} %)`} monto={liquidacion.ipm} />
              <Fila concepto={`Percepción (${liquidacion.tasas.percepcion} %)`} monto={liquidacion.percepcion} />
              <Fila concepto="Total de tributos" monto={liquidacion.total} destacada />
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {puedeEditar && liquidado && (
        <>
          <TituloSeccion>Registro de pago de tributos</TituloSeccion>
          <Paper component="form" onSubmit={pagar} noValidate variant="outlined" sx={{ p: 2 }}>
            <Box sx={DOS_COLUMNAS}>
              <TextField
                label="Fecha del pago"
                type="date"
                value={pago.fecha}
                onChange={(e) => cambiarPago('fecha', e.target.value)}
                error={Boolean(erroresPago.fecha)}
                helperText={erroresPago.fecha}
                slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoyIso() } }}
                required
              />
              <TextField
                label="Monto pagado (USD)"
                value={pago.monto}
                onChange={(e) => cambiarPago('monto', e.target.value)}
                error={Boolean(erroresPago.monto)}
                helperText={erroresPago.monto}
                slotProps={{ htmlInput: { inputMode: 'decimal' } }}
                required
              />
              <TextField
                select
                label="Medio de pago"
                value={pago.medioPago}
                onChange={(e) => cambiarPago('medioPago', e.target.value)}
                error={Boolean(erroresPago.medioPago)}
                helperText={erroresPago.medioPago}
                required
              >
                {MEDIOS_PAGO.map((m) => (
                  <MenuItem key={m.valor} value={m.valor}>
                    {m.texto}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="N° de operación"
                value={pago.numeroOperacion}
                onChange={(e) => cambiarPago('numeroOperacion', e.target.value)}
                error={Boolean(erroresPago.numeroOperacion)}
                helperText={erroresPago.numeroOperacion}
                slotProps={{ htmlInput: { maxLength: 50 } }}
                required
              />
            </Box>
            <Stack direction="row" sx={{ mt: 2 }}>
              <Button type="submit" variant="contained" disabled={enviando !== null}>
                {enviando === 'pago' ? 'Registrando…' : 'Registrar pago realizado'}
              </Button>
            </Stack>
          </Paper>
        </>
      )}
    </Box>
  )
}

function Fila({ concepto, monto, destacada = false }: { concepto: string; monto: number; destacada?: boolean }) {
  return (
    <TableRow>
      <TableCell sx={{ fontWeight: destacada ? 700 : undefined }}>{concepto}</TableCell>
      <TableCell align="right" sx={{ fontWeight: destacada ? 700 : undefined }} aria-label={concepto}>
        {formatoMonto(monto)}
      </TableCell>
    </TableRow>
  )
}
