import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/DeleteOutlined'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
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
} from '@mui/material'
import { useEffect, useState, type FormEvent } from 'react'

import { guardarEmbarque, obtenerEmbarque } from '../api/despachos'
import { leerError, mensajeDeError } from '../api/http'
import type { Contenedor, Embarque, EmbarqueRequest } from '../types'
import { diasEntre, formatoFecha, hoyIso, sumarDias } from '../utils/formato'
import { TIPOS_CONTENEDOR } from '../utils/opciones'
import { validarContenedor } from '../utils/validaciones'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

/** Valor por defecto de DIAS_ALMACENAJE (parametro_sistema) hasta que el API devuelva el plazo real. */
const DIAS_ALMACENAJE_POR_DEFECTO = 30
const DIAS_ALERTA_ALMACENAJE = 3

const FILA_VACIA: Contenedor = { numero: '', tipo: '', precinto: '', pesoBrutoKg: '' }
const VACIO: EmbarqueRequest = {
  numeroBl: '',
  tipoBl: '',
  naviera: '',
  buque: '',
  numeroViaje: '',
  puertoEmbarque: '',
  puertoDescarga: '',
  fechaEmbarque: '',
  fechaEta: '',
  contenedores: [{ ...FILA_VACIA }],
}

type CampoTexto = Exclude<keyof EmbarqueRequest, 'contenedores'>

const OBLIGATORIOS: [CampoTexto, string][] = [
  ['numeroBl', 'Ingrese el N° de BL'],
  ['tipoBl', 'Seleccione el tipo de BL'],
  ['naviera', 'Ingrese la naviera'],
  ['puertoEmbarque', 'Ingrese el puerto de embarque'],
  ['fechaEta', 'Ingrese la fecha ETA'],
]

function desdeEmbarque(e: Embarque): EmbarqueRequest {
  return {
    numeroBl: e.numeroBl,
    tipoBl: e.tipoBl,
    naviera: e.naviera,
    buque: e.buque ?? '',
    numeroViaje: e.numeroViaje ?? '',
    puertoEmbarque: e.puertoEmbarque,
    puertoDescarga: e.puertoDescarga ?? '',
    fechaEmbarque: e.fechaEmbarque ?? '',
    fechaEta: e.fechaEta,
    contenedores: e.contenedores.map((c) => ({
      numero: c.numero,
      tipo: c.tipo ?? '',
      precinto: c.precinto ?? '',
      pesoBrutoKg: c.pesoBrutoKg === null ? '' : String(c.pesoBrutoKg),
    })),
  }
}

/** Valida el formulario como el backend: obligatorios (CA4), ISO 6346 por fila (CA2) y sin repetidos. */
export function validarEmbarque(form: EmbarqueRequest): Record<string, string> {
  const errores: Record<string, string> = {}
  for (const [campo, mensaje] of OBLIGATORIOS) {
    if (!String(form[campo]).trim()) errores[campo] = mensaje
  }
  if (form.fechaEmbarque && form.fechaEta && form.fechaEmbarque > form.fechaEta) {
    errores.fechaEmbarque = 'La fecha de zarpe debe ser anterior o igual a la ETA'
  }
  if (form.contenedores.length === 0) errores.contenedores = 'Registre al menos un contenedor'
  const vistos = new Set<string>()
  form.contenedores.forEach((c, i) => {
    const numero = c.numero.trim().toUpperCase()
    const problema = validarContenedor(numero)
    if (problema) errores[`contenedores[${i}].numero`] = problema
    else if (vistos.has(numero)) errores[`contenedores[${i}].numero`] = 'Contenedor repetido en este embarque'
    vistos.add(numero)
    const peso = c.pesoBrutoKg.trim().replace(',', '.')
    if (peso && !(/^\d+(\.\d{1,2})?$/.test(peso) && Number(peso) > 0)) {
      errores[`contenedores[${i}].pesoBrutoKg`] = 'Peso no válido'
    }
  })
  return errores
}

/**
 * Plazo de almacenaje que se muestra mientras se edita la ETA (CA3): fecha límite = ETA +
 * días de almacenaje. Los días salen del último guardado (fecha límite − ETA) y, para un
 * embarque nuevo, del valor por defecto; al guardar, el API devuelve el plazo oficial.
 */
export function plazoAlmacenaje(fechaEta: string, embarque: Embarque | null, hoy = hoyIso()) {
  if (!fechaEta) return null
  const dias = embarque ? diasEntre(embarque.fechaEta, embarque.fechaLimiteAlmacenaje) : DIAS_ALMACENAJE_POR_DEFECTO
  const fechaLimite = sumarDias(fechaEta, dias)
  const diasRestantes = diasEntre(hoy, fechaLimite)
  return { fechaLimite, diasRestantes, alerta: diasRestantes <= DIAS_ALERTA_ALMACENAJE }
}

/** HU-05 · T-05.4: embarque/BL del despacho con su tabla de contenedores. */
export function EmbarqueForm({ despachoId }: { despachoId: number }) {
  const [form, setForm] = useState<EmbarqueRequest>(VACIO)
  const [embarque, setEmbarque] = useState<Embarque | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    obtenerEmbarque(despachoId)
      .then((e) => {
        setEmbarque(e)
        if (e) setForm(desdeEmbarque(e))
      })
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el embarque.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  function cambiar(campo: CampoTexto, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores((e) => ({ ...e, [campo]: '' }))
  }

  function cambiarContenedor(indice: number, campo: keyof Contenedor, valor: string) {
    setForm((f) => ({
      ...f,
      contenedores: f.contenedores.map((c, i) =>
        i === indice ? { ...c, [campo]: campo === 'numero' ? valor.toUpperCase().replace(/\s/g, '') : valor } : c,
      ),
    }))
    setErrores((e) => ({ ...e, [`contenedores[${indice}].${campo}`]: '', contenedores: '' }))
  }

  function quitarContenedor(indice: number) {
    setForm((f) => ({ ...f, contenedores: f.contenedores.filter((_, i) => i !== indice) }))
    // Los errores por fila dependen del índice: se recalculan al guardar.
    setErrores((e) => Object.fromEntries(Object.entries(e).filter(([k]) => !k.startsWith('contenedores['))))
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const nuevos = validarEmbarque(form)
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setGuardando(true)
    try {
      const guardado = await guardarEmbarque(despachoId, form)
      setEmbarque(guardado)
      setForm(desdeEmbarque(guardado))
      setMensaje(
        guardado.etaModificada
          ? `Información de embarque guardada. La ETA cambió del ${formatoFecha(guardado.etaAnterior)} al ${formatoFecha(guardado.fechaEta)}: el plazo de almacenaje se recalculó.`
          : 'Información de embarque guardada.',
      )
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeDeError(e, 'No se pudo guardar el embarque.'))
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando embarque" />
      </Box>
    )
  }

  const plazo = plazoAlmacenaje(form.fechaEta, embarque)
  const etaCambiada = embarque !== null && form.fechaEta !== '' && form.fechaEta !== embarque.fechaEta

  const texto = (campo: CampoTexto, label: string, extra: { required?: boolean; type?: string } = {}) => (
    <TextField
      label={label}
      value={form[campo]}
      onChange={(e) => cambiar(campo, e.target.value)}
      required={extra.required}
      type={extra.type}
      error={Boolean(errores[campo])}
      helperText={errores[campo]}
    />
  )

  return (
    <Paper component="form" onSubmit={guardar} noValidate sx={{ p: { xs: 2, md: 2.5 } }}>
      <Stack spacing={2} sx={{ mb: mensaje || error ? 2 : 0 }}>
        {mensaje && <Alert severity={embarque?.etaModificada ? 'info' : 'success'}>{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
      </Stack>

      <TituloSeccion>Datos del conocimiento de embarque (BL)</TituloSeccion>
      <Box sx={DOS_COLUMNAS}>
        {texto('numeroBl', 'N° de BL / House BL', { required: true })}
        {texto('naviera', 'Naviera / Agente marítimo', { required: true })}
        {texto('buque', 'Nombre del buque')}
        {texto('numeroViaje', 'N° de viaje')}
        {texto('puertoEmbarque', 'Puerto de embarque', { required: true })}
        {texto('puertoDescarga', 'Puerto de descarga')}
        {texto('fechaEmbarque', 'Fecha ETD (zarpe)', { type: 'date' })}
        {texto('fechaEta', 'Fecha ETA (arribo estimado)', { required: true, type: 'date' })}
        <TextField
          select
          label="Tipo de BL"
          value={form.tipoBl}
          onChange={(e) => cambiar('tipoBl', e.target.value)}
          required
          error={Boolean(errores.tipoBl)}
          helperText={errores.tipoBl}
        >
          <MenuItem value="MASTER">Master BL</MenuItem>
          <MenuItem value="HOUSE">House BL</MenuItem>
        </TextField>
        <TextField
          label="Plazo de almacenaje (fecha límite)"
          value={
            plazo
              ? `${formatoFecha(plazo.fechaLimite)} · ${plazo.diasRestantes < 0 ? `vencido hace ${Math.abs(plazo.diasRestantes)} días` : `${plazo.diasRestantes} días restantes`}`
              : 'Se calcula con la fecha ETA'
          }
          disabled
          slotProps={{ htmlInput: { readOnly: true, 'aria-live': 'polite' } }}
          helperText={etaCambiada ? 'Recalculado con la nueva ETA. Guarde para confirmarlo.' : undefined}
        />
      </Box>

      {plazo?.alerta && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          {plazo.diasRestantes < 0
            ? `El plazo de almacenaje venció hace ${Math.abs(plazo.diasRestantes)} días.`
            : `Quedan ${plazo.diasRestantes} días para el vencimiento del plazo de almacenaje.`}
        </Alert>
      )}

      <Box sx={{ mt: 3 }}>
        <TituloSeccion
          accion={
            <Button
              size="small"
              startIcon={<AddIcon />}
              onClick={() => setForm((f) => ({ ...f, contenedores: [...f.contenedores, { ...FILA_VACIA }] }))}
            >
              Agregar contenedor
            </Button>
          }
        >
          Contenedores asociados
        </TituloSeccion>
        {errores.contenedores && (
          <Alert severity="error" sx={{ mb: 1 }}>
            {errores.contenedores}
          </Alert>
        )}
        <TableContainer>
          <Table size="small" sx={{ minWidth: 640 }}>
            <TableHead>
              <TableRow>
                <TableCell>N° Contenedor</TableCell>
                <TableCell sx={{ width: 130 }}>Tipo</TableCell>
                <TableCell sx={{ width: 150 }}>Peso bruto (kg)</TableCell>
                <TableCell>Precintos</TableCell>
                <TableCell sx={{ width: 48 }} />
              </TableRow>
            </TableHead>
            <TableBody>
              {form.contenedores.map((c, i) => (
                <TableRow key={i} sx={{ verticalAlign: 'top' }}>
                  <TableCell>
                    <TextField
                      value={c.numero}
                      placeholder="CSQU3054383"
                      onChange={(e) => cambiarContenedor(i, 'numero', e.target.value)}
                      error={Boolean(errores[`contenedores[${i}].numero`])}
                      helperText={errores[`contenedores[${i}].numero`]}
                      slotProps={{ htmlInput: { maxLength: 11, 'aria-label': `Número del contenedor ${i + 1}` } }}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      select
                      value={c.tipo}
                      onChange={(e) => cambiarContenedor(i, 'tipo', e.target.value)}
                      slotProps={{ htmlInput: { 'aria-label': `Tipo del contenedor ${i + 1}` } }}
                    >
                      {TIPOS_CONTENEDOR.map((t) => (
                        <MenuItem key={t.valor} value={t.valor}>
                          {t.texto}
                        </MenuItem>
                      ))}
                    </TextField>
                  </TableCell>
                  <TableCell>
                    <TextField
                      value={c.pesoBrutoKg}
                      placeholder="18540"
                      onChange={(e) => cambiarContenedor(i, 'pesoBrutoKg', e.target.value)}
                      error={Boolean(errores[`contenedores[${i}].pesoBrutoKg`])}
                      helperText={errores[`contenedores[${i}].pesoBrutoKg`]}
                      slotProps={{
                        htmlInput: { inputMode: 'decimal', 'aria-label': `Peso bruto del contenedor ${i + 1}` },
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      value={c.precinto}
                      placeholder="SL-88213"
                      onChange={(e) => cambiarContenedor(i, 'precinto', e.target.value)}
                      slotProps={{ htmlInput: { maxLength: 30, 'aria-label': `Precinto del contenedor ${i + 1}` } }}
                    />
                  </TableCell>
                  <TableCell>
                    <Tooltip title="Quitar contenedor">
                      <IconButton aria-label={`Quitar contenedor ${i + 1}`} onClick={() => quitarContenedor(i)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>

      <Box sx={{ mt: 3 }}>
        <Button type="submit" variant="contained" disabled={guardando}>
          {guardando ? 'Guardando…' : 'Guardar información de embarque'}
        </Button>
      </Box>
    </Paper>
  )
}
