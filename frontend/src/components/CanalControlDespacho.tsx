import { Alert, Box, Button, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { useEffect, useState, type FormEvent } from 'react'
import { Link as RouterLink } from 'react-router'

import { guardarCanal } from '../api/canal'
import { leerError, mensajeDeError } from '../api/http'
import type { CanalControl, CanalGuardado } from '../types'
import { CANALES_CONTROL, textoOpcion } from '../utils/opciones'
import { CanalChip } from './EtiquetaColor'
import { DOS_COLUMNAS, TituloSeccion } from './Seccion'

function canalValido(canal: string | null | undefined): CanalControl | '' {
  const mayusculas = canal?.toUpperCase()
  return CANALES_CONTROL.find((c) => c.valor === mayusculas)?.valor ?? ''
}

interface Props {
  despachoId: number
  /** Canal que ya tiene el despacho (el de la cabecera); null mientras no se asigna. */
  canal: string | null
  /** ADMIN y AGENTE registran; CLIENTE solo consulta. */
  puedeEditar: boolean
  /** Se llama tras guardar, para actualizar el chip de canal y el estado de la cabecera. */
  onCanalGuardado?: () => void
  /** Lleva a la pestaña «Línea de tiempo», donde queda el hito del cambio. */
  onVerLineaTiempo?: () => void
}

/**
 * HU-10 · T-10.3: pestaña «Canal de control». El canal no llega solo de SUNAT: el agente lo consulta
 * en el sistema externo y lo digita aquí (contrato C5). En rojo se muestra el hito sugerido (CA2) y en
 * verde se habilita el acceso al levante (CA3); cada guardado deja un hito en la línea de tiempo (CA4).
 */
export function CanalControlDespacho({ despachoId, canal, puedeEditar, onCanalGuardado, onVerLineaTiempo }: Props) {
  const [seleccion, setSeleccion] = useState<CanalControl | ''>(canalValido(canal))
  const [resultado, setResultado] = useState<CanalGuardado | null>(null)
  const [problema, setProblema] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => setSeleccion(canalValido(canal)), [canal])

  const actual = resultado?.canal ?? canalValido(canal)
  const guardado = resultado !== null

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    if (!seleccion) {
      setProblema('Seleccione el canal informado por SUNAT')
      return
    }
    setProblema(null)
    setEnviando(true)
    try {
      setResultado(await guardarCanal(despachoId, seleccion))
      onCanalGuardado?.()
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 400 && body?.campos?.canal) setProblema(body.campos.canal)
      else setError(mensajeDeError(e, 'No se pudo guardar el canal de control.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Box>
      <TituloSeccion>Canal de control asignado</TituloSeccion>

      <Stack spacing={2} sx={{ mb: 2 }}>
        <Alert severity="info">
          El canal no se recibe automáticamente: consúltelo en el sistema externo de despacho aduanero o en SUNAT y
          regístrelo aquí.
        </Alert>
        {guardado && (
          <Alert
            severity="success"
            action={
              onVerLineaTiempo && (
                <Button color="inherit" size="small" onClick={onVerLineaTiempo}>
                  Ver línea de tiempo
                </Button>
              )
            }
          >
            Canal {textoOpcion(CANALES_CONTROL, resultado.canal).toLowerCase()} guardado. Se agregó un hito a la línea de
            tiempo del despacho.
          </Alert>
        )}
        {resultado?.hitoSugerido && (
          <Alert severity="warning">Hito sugerido: {resultado.hitoSugerido}.</Alert>
        )}
        {error && <Alert severity="error">{error}</Alert>}
      </Stack>

      <Box sx={{ ...DOS_COLUMNAS, mb: 3 }}>
        <Box>
          <Typography component="div" sx={{ fontSize: 12.5, fontWeight: 600, color: '#334155', mb: 0.5 }}>
            Canal de control
          </Typography>
          <Box aria-label="Canal de control actual">
            <CanalChip canal={actual || null} />
          </Box>
        </Box>
        {actual === 'VERDE' && (
          <Box>
            <Typography component="div" sx={{ fontSize: 12.5, fontWeight: 600, color: '#334155', mb: 0.5 }}>
              Levante
            </Typography>
            {/* El módulo de levante (HU-13) llega en el Sprint 3: hasta entonces esta ruta redirige al panel. */}
            <Button component={RouterLink} to={`/levante/${despachoId}`} variant="contained" size="small">
              Ir al módulo de levante
            </Button>
          </Box>
        )}
      </Box>

      {puedeEditar ? (
        <Paper component="form" onSubmit={guardar} noValidate variant="outlined" sx={{ p: 2 }}>
          <Box sx={DOS_COLUMNAS}>
            <TextField
              select
              label="Canal informado por SUNAT"
              value={seleccion}
              onChange={(e) => {
                setSeleccion(e.target.value as CanalControl)
                setProblema(null)
              }}
              error={Boolean(problema)}
              helperText={problema}
              required
            >
              {CANALES_CONTROL.map((c) => (
                <MenuItem key={c.valor} value={c.valor}>
                  {c.texto}
                </MenuItem>
              ))}
            </TextField>
          </Box>
          <Stack direction="row" sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" disabled={enviando}>
              {enviando ? 'Guardando…' : 'Guardar canal'}
            </Button>
          </Stack>
        </Paper>
      ) : (
        <Alert severity="info">Solo el agente de aduana puede registrar el canal de control.</Alert>
      )}
    </Box>
  )
}
