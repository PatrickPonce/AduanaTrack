import DeleteIcon from '@mui/icons-material/DeleteOutlined'
import {
  Alert,
  Box,
  Button,
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
  Typography,
} from '@mui/material'
import { useEffect, useState, type FormEvent } from 'react'

import { leerError, mensajeDeError } from '../api/http'
import {
  eliminarEnvioProgramado,
  listarEnviosProgramados,
  programarEnvio,
  type EnvioProgramado,
  type Periodicidad,
} from '../api/reportes'
import { formatoFechaHora } from '../utils/formato'
import { EtiquetaColor } from './EtiquetaColor'
import { TituloSeccion } from './Seccion'

export const PERIODICIDADES: Record<Periodicidad, string> = {
  SEMANAL: 'Semanal',
  MENSUAL: 'Mensual',
}

const MAX_DESTINATARIOS = 10
const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** "a@x.pe, b@x.pe; c@x.pe" → ["a@x.pe", "b@x.pe", "c@x.pe"], sin repetidos. */
export function separarDestinatarios(texto: string): string[] {
  const correos = texto
    .split(/[\s,;]+/)
    .map((c) => c.trim().toLowerCase())
    .filter(Boolean)
  return [...new Set(correos)]
}

/** Al menos un correo, todos válidos y como máximo 10. */
export function validarDestinatarios(texto: string): string | null {
  const correos = separarDestinatarios(texto)
  if (correos.length === 0) return 'Ingrese al menos un correo'
  const invalido = correos.find((c) => !CORREO.test(c))
  if (invalido) return `Correo no válido: ${invalido}`
  if (correos.length > MAX_DESTINATARIOS) return `Máximo ${MAX_DESTINATARIOS} destinatarios`
  return null
}

/**
 * HU-15 · T-15.4: «Envío programado del reporte» (CA4). El reporte en Excel se envía por correo cada semana
 * o cada mes a los destinatarios; la tarea diaria del backend (T-15.3) decide cuándo toca. Contrato C10.
 */
export function EnvioProgramadoReporte() {
  const [envios, setEnvios] = useState<EnvioProgramado[]>([])
  const [disponible, setDisponible] = useState(true)
  const [periodicidad, setPeriodicidad] = useState<Periodicidad>('SEMANAL')
  const [destinatarios, setDestinatarios] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listarEnviosProgramados()
      .then(setEnvios)
      .catch((e) => {
        // Mientras el envío programado no esté publicado (404), la sección lo explica en lugar de fallar.
        if (leerError(e).status === 404) setDisponible(false)
        else setError(mensajeDeError(e, 'No se pudieron cargar los envíos programados.'))
      })
  }, [])

  async function programar(evento: FormEvent) {
    evento.preventDefault()
    setMensaje(null)
    setError(null)
    const problema = validarDestinatarios(destinatarios)
    setErrores(problema ? { destinatarios: problema } : {})
    if (problema) return
    setEnviando(true)
    try {
      const nuevo = await programarEnvio({ periodicidad, destinatarios: separarDestinatarios(destinatarios) })
      setEnvios((actuales) => [...actuales, nuevo])
      setDestinatarios('')
      setMensaje(`Envío ${PERIODICIDADES[nuevo.periodicidad].toLowerCase()} programado para ${nuevo.destinatarios.join(', ')}.`)
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setError(mensajeDeError(e, 'No se pudo programar el envío.'))
    } finally {
      setEnviando(false)
    }
  }

  async function eliminar(envio: EnvioProgramado) {
    setMensaje(null)
    setError(null)
    try {
      await eliminarEnvioProgramado(envio.id)
      setEnvios((actuales) => actuales.filter((e) => e.id !== envio.id))
      setMensaje('Envío programado eliminado.')
    } catch (e) {
      setError(mensajeDeError(e, 'No se pudo eliminar el envío programado.'))
    }
  }

  return (
    <Box>
      <TituloSeccion>Envío programado por correo</TituloSeccion>
      <Stack spacing={2} sx={{ mb: 2 }}>
        {!disponible && (
          <Alert severity="info">El envío programado del reporte todavía no está disponible en el servidor.</Alert>
        )}
        {mensaje && <Alert severity="success">{mensaje}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
      </Stack>

      {disponible && (
        <>
          <Box
            component="form"
            onSubmit={programar}
            noValidate
            sx={{ display: 'grid', gap: 2, alignItems: 'start', gridTemplateColumns: { xs: '1fr', md: '180px 1fr auto' } }}
          >
            <TextField
              select
              label="Periodicidad"
              value={periodicidad}
              onChange={(e) => setPeriodicidad(e.target.value as Periodicidad)}
              error={Boolean(errores.periodicidad)}
              helperText={errores.periodicidad}
            >
              {(Object.keys(PERIODICIDADES) as Periodicidad[]).map((p) => (
                <MenuItem key={p} value={p}>
                  {PERIODICIDADES[p]}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Destinatarios"
              value={destinatarios}
              onChange={(e) => {
                setDestinatarios(e.target.value)
                setErrores({})
              }}
              error={Boolean(errores.destinatarios)}
              helperText={errores.destinatarios || 'Correos separados por coma'}
              placeholder="jefe.operaciones@ejemplo.pe"
            />
            <Button type="submit" variant="contained" sx={{ height: 40, mt: 0.5 }} disabled={enviando}>
              {enviando ? 'Guardando…' : 'Programar envío'}
            </Button>
          </Box>

          {envios.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              No hay envíos programados.
            </Typography>
          ) : (
            <TableContainer sx={{ mt: 2 }}>
              <Table size="small" aria-label="Envíos programados">
                <TableHead>
                  <TableRow>
                    <TableCell>Periodicidad</TableCell>
                    <TableCell>Destinatarios</TableCell>
                    <TableCell>Último envío</TableCell>
                    <TableCell>Estado</TableCell>
                    <TableCell sx={{ width: 56 }} />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {envios.map((e) => (
                    <TableRow key={e.id}>
                      <TableCell>{PERIODICIDADES[e.periodicidad] ?? e.periodicidad}</TableCell>
                      <TableCell>{e.destinatarios.join(', ')}</TableCell>
                      <TableCell>{e.ultimoEnvio ? formatoFechaHora(e.ultimoEnvio) : 'Todavía no se envía'}</TableCell>
                      <TableCell>
                        {e.activo === false ? (
                          <EtiquetaColor texto="Inactivo" tono="gris" />
                        ) : (
                          <EtiquetaColor texto="Activo" tono="verde" />
                        )}
                      </TableCell>
                      <TableCell>
                        <Tooltip title="Eliminar envío programado">
                          <IconButton
                            size="small"
                            aria-label={`Eliminar envío ${PERIODICIDADES[e.periodicidad]?.toLowerCase() ?? ''} a ${e.destinatarios.join(', ')}`}
                            onClick={() => void eliminar(e)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
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
