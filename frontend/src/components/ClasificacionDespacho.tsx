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
  TableFooter,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'

import { guardarClasificacion, guardarItems, listarItems } from '../api/clasificacion'
import { leerError, mensajeDeError } from '../api/http'
import type { EstadoDespacho, ItemFactura, ItemFacturaRequest, PartidaResumen } from '../types'
import { alcanzoEstado } from '../utils/estados'
import { formatoFechaHora, formatoMonto } from '../utils/formato'
import { UNIDADES_ITEM } from '../utils/opciones'
import { PartidaAutocomplete } from './PartidaAutocomplete'
import { TituloSeccion } from './Seccion'

/** Fila de la tabla: cantidad y valor FOB se editan como texto y se envían como número. */
export interface FilaItem {
  id?: number
  descripcion: string
  cantidad: string
  unidad: string
  valorFob: string
  partida: PartidaResumen | null
}

const FILA_VACIA: FilaItem = { descripcion: '', cantidad: '', unidad: 'UND', valorFob: '', partida: null }

function desdeItems(items: ItemFactura[]): FilaItem[] {
  return items.map((i) => ({
    id: i.id,
    descripcion: i.descripcion,
    cantidad: String(i.cantidad),
    unidad: i.unidad,
    valorFob: i.valorFob.toFixed(2),
    partida: i.partida,
  }))
}

function numero(valor: string): number {
  return Number(valor.trim().replace(',', '.'))
}

export function aRequest(filas: FilaItem[]): ItemFacturaRequest[] {
  return filas.map((f) => ({
    ...(f.id !== undefined && { id: f.id }),
    descripcion: f.descripcion.trim(),
    cantidad: numero(f.cantidad),
    unidad: f.unidad,
    valorFob: numero(f.valorFob),
    partidaId: f.partida?.id ?? null,
  }))
}

/** Mismas reglas que ItemFacturaRequest (contrato C2), con las claves de error del API: items[i].campo. */
export function validarItems(filas: FilaItem[]): Record<string, string> {
  const errores: Record<string, string> = {}
  filas.forEach((f, i) => {
    if (!f.descripcion.trim()) errores[`items[${i}].descripcion`] = 'Ingrese la descripción del ítem'
    else if (f.descripcion.trim().length > 500) errores[`items[${i}].descripcion`] = 'Máximo 500 caracteres'
    const cantidad = f.cantidad.trim().replace(',', '.')
    if (!/^\d+(\.\d{1,3})?$/.test(cantidad) || Number(cantidad) <= 0) {
      errores[`items[${i}].cantidad`] = 'Cantidad mayor a 0 (máximo 3 decimales)'
    }
    if (!f.unidad.trim()) errores[`items[${i}].unidad`] = 'Seleccione la unidad'
    const fob = f.valorFob.trim().replace(',', '.')
    if (!/^\d+(\.\d{1,2})?$/.test(fob) || Number(fob) <= 0) {
      errores[`items[${i}].valorFob`] = 'Valor FOB mayor a 0 (USD, 2 decimales)'
    }
  })
  return errores
}

interface Props {
  despachoId: number
  estado: EstadoDespacho
  /** ADMIN y AGENTE editan; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Se llama cuando el despacho pasa a CLASIFICADO, para actualizar la cabecera y las pestañas. */
  onClasificado?: () => void
}

/** HU-07 · T-07.4: pestaña «Clasificación» con los ítems de la factura comercial (contrato C2). */
export function ClasificacionDespacho({ despachoId, estado, puedeEditar, onClasificado }: Props) {
  const [filas, setFilas] = useState<FilaItem[]>([])
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState<'items' | 'clasificacion' | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCargando(true)
    listarItems(despachoId)
      .then((items) => setFilas(desdeItems(items)))
      .catch((e) => setError(mensajeDeError(e, 'No se pudieron cargar los ítems de la factura.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  // Desde la liquidación (HU-08) los montos dependen de estos ítems: ya no se editan.
  const editable = puedeEditar && !alcanzoEstado(estado, 'LIQUIDADO')
  const sinPartida = filas.filter((f) => !f.partida).length
  const totalFob = filas.reduce((total, f) => total + (Number.isFinite(numero(f.valorFob)) ? numero(f.valorFob) : 0), 0)

  function cambiar(indice: number, cambio: Partial<FilaItem>) {
    setFilas((actuales) => actuales.map((f, i) => (i === indice ? { ...f, ...cambio } : f)))
    setErrores((e) => {
      const restantes = { ...e }
      for (const campo of Object.keys(cambio)) delete restantes[`items[${indice}].${campo}`]
      return restantes
    })
    setMensaje(null)
  }

  function quitar(indice: number) {
    setFilas((actuales) => actuales.filter((_, i) => i !== indice))
    // Los errores por fila dependen del índice: se recalculan al guardar.
    setErrores({})
    setMensaje(null)
  }

  /** PUT de la tabla completa; devuelve false si hay errores de validación. */
  async function enviarItems(): Promise<boolean> {
    const nuevos = validarItems(filas)
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return false
    try {
      setFilas(desdeItems(await guardarItems(despachoId, aRequest(filas))))
      return true
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeDeError(e, 'No se pudieron guardar los ítems.'))
      return false
    }
  }

  async function guardarSoloItems() {
    setMensaje(null)
    setError(null)
    setGuardando('items')
    if (await enviarItems()) setMensaje('Ítems de la factura guardados.')
    setGuardando(null)
  }

  /** CA3 y CA4: guarda los ítems con su partida y registra la clasificación (usuario y fecha). */
  async function clasificar() {
    setMensaje(null)
    setError(null)
    setGuardando('clasificacion')
    try {
      if (!(await enviarItems())) return
      const resultado = await guardarClasificacion(despachoId)
      setMensaje(
        `Clasificación arancelaria registrada por ${resultado.clasificadoPor} el ${formatoFechaHora(resultado.clasificadoEn)}.`,
      )
      onClasificado?.()
    } catch (e) {
      setError(mensajeClasificacion(e))
    } finally {
      setGuardando(null)
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando ítems" />
      </Box>
    )
  }

  return (
    <Box>
      <TituloSeccion
        accion={
          editable && (
            <Button size="small" startIcon={<AddIcon />} onClick={() => setFilas((f) => [...f, { ...FILA_VACIA }])}>
              Agregar ítem
            </Button>
          )
        }
      >
        Ítems de la factura comercial
      </TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        {mensaje && <Alert severity="success">{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {estado === 'CLASIFICADO' && !mensaje && (
          <Alert severity="info">La clasificación arancelaria está registrada. El siguiente paso es la liquidación.</Alert>
        )}
      </Stack>

      <TableContainer>
        <Table size="small" sx={{ minWidth: 960 }}>
          <TableHead>
            <TableRow>
              <TableCell sx={{ width: 48 }}>N°</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell sx={{ width: 120 }}>Cantidad</TableCell>
              <TableCell sx={{ width: 150 }}>Unidad</TableCell>
              <TableCell sx={{ width: 150 }}>Valor FOB (USD)</TableCell>
              <TableCell sx={{ width: 300 }}>Partida arancelaria</TableCell>
              {editable && <TableCell sx={{ width: 48 }} />}
            </TableRow>
          </TableHead>
          <TableBody>
            {filas.map((f, i) => (
              <TableRow key={f.id ?? `nueva-${i}`} sx={{ verticalAlign: 'top' }}>
                <TableCell sx={{ pt: editable ? 2 : undefined }}>{i + 1}</TableCell>
                {editable ? (
                  <>
                    <TableCell>
                      <TextField
                        value={f.descripcion}
                        onChange={(e) => cambiar(i, { descripcion: e.target.value })}
                        error={Boolean(errores[`items[${i}].descripcion`])}
                        helperText={errores[`items[${i}].descripcion`]}
                        fullWidth
                        size="small"
                        slotProps={{ htmlInput: { maxLength: 500, 'aria-label': `Descripción del ítem ${i + 1}` } }}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        value={f.cantidad}
                        onChange={(e) => cambiar(i, { cantidad: e.target.value })}
                        error={Boolean(errores[`items[${i}].cantidad`])}
                        helperText={errores[`items[${i}].cantidad`]}
                        size="small"
                        slotProps={{ htmlInput: { inputMode: 'decimal', 'aria-label': `Cantidad del ítem ${i + 1}` } }}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        select
                        value={f.unidad}
                        onChange={(e) => cambiar(i, { unidad: e.target.value })}
                        error={Boolean(errores[`items[${i}].unidad`])}
                        helperText={errores[`items[${i}].unidad`]}
                        size="small"
                        fullWidth
                        slotProps={{ htmlInput: { 'aria-label': `Unidad del ítem ${i + 1}` } }}
                      >
                        {UNIDADES_ITEM.map((u) => (
                          <MenuItem key={u.valor} value={u.valor}>
                            {u.texto}
                          </MenuItem>
                        ))}
                      </TextField>
                    </TableCell>
                    <TableCell>
                      <TextField
                        value={f.valorFob}
                        onChange={(e) => cambiar(i, { valorFob: e.target.value })}
                        error={Boolean(errores[`items[${i}].valorFob`])}
                        helperText={errores[`items[${i}].valorFob`]}
                        size="small"
                        slotProps={{ htmlInput: { inputMode: 'decimal', 'aria-label': `Valor FOB del ítem ${i + 1}` } }}
                      />
                    </TableCell>
                    <TableCell>
                      <PartidaAutocomplete
                        etiqueta={`Partida arancelaria del ítem ${i + 1}`}
                        value={f.partida}
                        onChange={(partida) => cambiar(i, { partida })}
                        error={errores[`items[${i}].partidaId`]}
                      />
                    </TableCell>
                    <TableCell>
                      <Tooltip title="Quitar ítem">
                        <IconButton aria-label={`Quitar ítem ${i + 1}`} onClick={() => quitar(i)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </>
                ) : (
                  <>
                    <TableCell>{f.descripcion}</TableCell>
                    <TableCell>{f.cantidad}</TableCell>
                    <TableCell>{f.unidad}</TableCell>
                    <TableCell>{formatoMonto(numero(f.valorFob))}</TableCell>
                    <TableCell>
                      {f.partida ? `${f.partida.codigoFormateado} · ${f.partida.descripcion}` : 'Sin partida'}
                    </TableCell>
                  </>
                )}
              </TableRow>
            ))}
            {filas.length === 0 && (
              <TableRow>
                <TableCell colSpan={editable ? 7 : 6} sx={{ color: 'text.secondary', py: 2 }}>
                  Sin ítems registrados. {editable && 'Agregue los ítems de la factura comercial.'}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
          {filas.length > 0 && (
            <TableFooter>
              <TableRow>
                <TableCell colSpan={4} align="right" sx={{ fontWeight: 700 }}>
                  Valor FOB total
                </TableCell>
                <TableCell sx={{ fontWeight: 700 }} aria-label="Valor FOB total">
                  {formatoMonto(totalFob)}
                </TableCell>
                <TableCell colSpan={editable ? 2 : 1} />
              </TableRow>
            </TableFooter>
          )}
        </Table>
      </TableContainer>

      {editable && (
        <Stack direction="row" spacing={1.5} sx={{ mt: 3, alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
          <Button variant="outlined" onClick={guardarSoloItems} disabled={guardando !== null}>
            {guardando === 'items' ? 'Guardando…' : 'Guardar ítems'}
          </Button>
          <Button
            variant="contained"
            onClick={clasificar}
            disabled={guardando !== null || filas.length === 0 || sinPartida > 0}
          >
            {guardando === 'clasificacion' ? 'Guardando…' : 'Guardar clasificación arancelaria'}
          </Button>
          {sinPartida > 0 && (
            <Typography variant="body2" color="warning.main" role="status">
              {sinPartida === 1 ? 'Falta asignar la partida a 1 ítem.' : `Falta asignar la partida a ${sinPartida} ítems.`}
            </Typography>
          )}
        </Stack>
      )}
    </Box>
  )
}

/** 409 del contrato C2: ítems sin partida (números) o documentos obligatorios pendientes (HU-06). */
function mensajeClasificacion(error: unknown): string {
  const { status, body } = leerError(error)
  if (status === 409 && body?.codigo === 'ITEMS_SIN_PARTIDA' && Array.isArray(body.items)) {
    return `Los ítems ${body.items.join(', ')} no tienen partida arancelaria asignada.`
  }
  if (status === 409 && body?.codigo === 'DOCUMENTACION_INCOMPLETA') {
    const pendientes = Array.isArray(body.pendientes) ? ` (${body.pendientes.join(', ')})` : ''
    return `Faltan documentos obligatorios${pendientes}. Cárguelos en la pestaña «Documentos».`
  }
  return mensajeDeError(error, 'No se pudo guardar la clasificación arancelaria.')
}
