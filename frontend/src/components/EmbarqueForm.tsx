import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/DeleteOutlined'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
  MenuItem,
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
import { formatoFecha } from '../utils/formato'
import { validarContenedor } from '../utils/validaciones'
import { rejilla, Seccion, soloLectura, Tarjeta } from './Tarjeta'

export const TIPOS_CONTENEDOR = ['20GP', '40GP', '40HC']

const FILA_VACIA: Contenedor = { numero: '', tipo: '', precinto: '' }
const VACIO: EmbarqueRequest = {
  numeroBl: '',
  tipoBl: '',
  naviera: '',
  buque: '',
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
    puertoEmbarque: e.puertoEmbarque,
    puertoDescarga: e.puertoDescarga ?? '',
    fechaEmbarque: e.fechaEmbarque ?? '',
    fechaEta: e.fechaEta,
    contenedores: e.contenedores.map((c) => ({ numero: c.numero, tipo: c.tipo ?? '', precinto: c.precinto ?? '' })),
  }
}

/** Valida el formulario como el backend: obligatorios (CA4), ISO 6346 por fila (CA2) y sin repetidos. */
export function validarEmbarque(form: EmbarqueRequest): Record<string, string> {
  const errores: Record<string, string> = {}
  for (const [campo, mensaje] of OBLIGATORIOS) {
    if (!String(form[campo]).trim()) errores[campo] = mensaje
  }
  if (form.fechaEmbarque && form.fechaEta && form.fechaEmbarque > form.fechaEta) {
    errores.fechaEmbarque = 'La fecha de embarque debe ser anterior o igual a la ETA'
  }
  if (form.contenedores.length === 0) errores.contenedores = 'Registre al menos un contenedor'
  const vistos = new Set<string>()
  form.contenedores.forEach((c, i) => {
    const numero = c.numero.trim().toUpperCase()
    const problema = validarContenedor(numero)
    if (problema) errores[`contenedores[${i}].numero`] = problema
    else if (vistos.has(numero)) errores[`contenedores[${i}].numero`] = 'Contenedor repetido en este embarque'
    vistos.add(numero)
  })
  return errores
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
    <Tarjeta component="form" onSubmit={guardar} noValidate titulo="Datos del conocimiento de embarque (BL)">
      <Stack spacing={2.5}>
        {mensaje && <Alert severity={embarque?.etaModificada ? 'info' : 'success'}>{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {embarque?.alertaAlmacenaje && (
          <Alert severity="warning" variant="filled" sx={{ bgcolor: 'warning.main' }}>
            {embarque.diasRestantes !== null && embarque.diasRestantes < 0
              ? `El plazo de almacenaje venció hace ${Math.abs(embarque.diasRestantes)} días.`
              : `Quedan ${embarque.diasRestantes} días para el vencimiento del plazo de almacenaje.`}
          </Alert>
        )}

        <Box sx={rejilla}>
          {texto('numeroBl', 'N° de BL / House BL', { required: true })}
          <TextField
            select
            label="Tipo de BL"
            value={form.tipoBl}
            onChange={(e) => cambiar('tipoBl', e.target.value)}
            required
            error={Boolean(errores.tipoBl)}
            helperText={errores.tipoBl}
            slotProps={{ select: { displayEmpty: true } }}
          >
            <MenuItem value="" disabled>
              Seleccione el tipo
            </MenuItem>
            <MenuItem value="MASTER">Master BL</MenuItem>
            <MenuItem value="HOUSE">House BL</MenuItem>
          </TextField>
          {texto('naviera', 'Naviera', { required: true })}
          {texto('buque', 'Buque')}
          {texto('puertoEmbarque', 'Puerto de embarque', { required: true })}
          {texto('puertoDescarga', 'Puerto de descarga')}
          {texto('fechaEmbarque', 'Fecha de embarque', { type: 'date' })}
          {texto('fechaEta', 'Fecha ETA (arribo estimado)', { required: true, type: 'date' })}
        </Box>

        <Box sx={{ pt: 1 }}>
          <Seccion titulo="Plazo de almacenaje" />
          <Box sx={rejilla}>
            <TextField
              label="Fecha límite de almacenaje"
              value={embarque ? formatoFecha(embarque.fechaLimiteAlmacenaje) : 'Se calculará al guardar'}
              sx={soloLectura}
              slotProps={{ htmlInput: { readOnly: true } }}
            />
            <TextField
              label="Días restantes"
              value={embarque?.diasRestantes ?? '—'}
              color={embarque?.alertaAlmacenaje ? 'warning' : undefined}
              focused={embarque?.alertaAlmacenaje || undefined}
              sx={soloLectura}
              slotProps={{ htmlInput: { readOnly: true } }}
            />
          </Box>
        </Box>

        <Box sx={{ pt: 1 }}>
          <Seccion
            titulo="Contenedores asociados"
            acciones={
              <Button
                startIcon={<AddIcon />}
                onClick={() => setForm((f) => ({ ...f, contenedores: [...f.contenedores, { ...FILA_VACIA }] }))}
              >
                Agregar contenedor
              </Button>
            }
          />
          {errores.contenedores && (
            <Alert severity="error" sx={{ mb: 1.5 }}>
              {errores.contenedores}
            </Alert>
          )}
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ width: 40 }}>#</TableCell>
                  <TableCell>N° de contenedor</TableCell>
                  <TableCell sx={{ width: 150 }}>Tipo</TableCell>
                  <TableCell>Precinto</TableCell>
                  <TableCell sx={{ width: 56 }} />
                </TableRow>
              </TableHead>
              <TableBody>
                {form.contenedores.map((c, i) => (
                  <TableRow key={i} sx={{ '& td': { verticalAlign: 'top', py: 1.25 } }}>
                    <TableCell sx={{ pt: '18px !important' }}>{i + 1}</TableCell>
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
                        slotProps={{ htmlInput: { 'aria-label': `Tipo del contenedor ${i + 1}` }, select: { displayEmpty: true } }}
                      >
                        <MenuItem value="">
                          <em>Sin tipo</em>
                        </MenuItem>
                        {TIPOS_CONTENEDOR.map((t) => (
                          <MenuItem key={t} value={t}>
                            {t}
                          </MenuItem>
                        ))}
                      </TextField>
                    </TableCell>
                    <TableCell>
                      <TextField
                        value={c.precinto}
                        placeholder="Opcional"
                        onChange={(e) => cambiarContenedor(i, 'precinto', e.target.value)}
                        slotProps={{ htmlInput: { maxLength: 30, 'aria-label': `Precinto del contenedor ${i + 1}` } }}
                      />
                    </TableCell>
                    <TableCell>
                      <Tooltip title="Quitar contenedor">
                        <IconButton aria-label={`Quitar contenedor ${i + 1}`} onClick={() => quitarContenedor(i)}>
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>

        <Box>
          <Button type="submit" variant="contained" size="large" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Guardar información de embarque'}
          </Button>
        </Box>
      </Stack>
    </Tarjeta>
  )
}
