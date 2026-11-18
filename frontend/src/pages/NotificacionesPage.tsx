import { Alert, Box, Button, Chip, CircularProgress, Link, Paper, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'
import { useCallback, useEffect, useState } from 'react'
import { Link as RouterLink } from 'react-router'

import { mensajeDeError } from '../api/http'
import { listarNotificaciones, marcarLeida } from '../api/notificaciones'
import { useAuth } from '../auth/AuthContext'
import { useEncabezado } from '../components/Encabezado'
import type { NotificacionBandeja, SeveridadNotificacion } from '../types'
import { formatoFechaHora } from '../utils/formato'

const SEVERIDAD: Record<SeveridadNotificacion, { texto: string; color: 'error' | 'warning' | 'info' }> = {
  CRITICA: { texto: 'Crítica', color: 'error' },
  ALTA: { texto: 'Alta', color: 'warning' },
  INFO: { texto: 'Informativa', color: 'info' },
}

/** HU-14 · T-14.3: centro de notificaciones del usuario en sesión (contrato C9). */
export function NotificacionesPage() {
  useEncabezado('Gestión', 'Notificaciones')
  const { usuario } = useAuth()
  const esCliente = usuario?.rol === 'CLIENTE'
  const [estado, setEstado] = useState<'TODAS' | 'NO_LEIDAS'>('TODAS')
  const [filas, setFilas] = useState<NotificacionBandeja[] | null>(null)
  const [total, setTotal] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(async (filtro: 'TODAS' | 'NO_LEIDAS') => {
    setError(null)
    try {
      const pagina = await listarNotificaciones(filtro, 0, 50)
      setFilas(pagina.contenido)
      setTotal(pagina.totalElementos)
    } catch (e) {
      setFilas([])
      setError(mensajeDeError(e, 'No se pudieron cargar las notificaciones.'))
    }
  }, [])

  useEffect(() => {
    setFilas(null)
    void cargar(estado)
  }, [estado, cargar])

  async function leer(n: NotificacionBandeja) {
    if (n.leida) return
    try {
      const actualizada = await marcarLeida(n.id)
      setFilas((actual) => actual?.map((f) => (f.id === n.id ? actualizada : f)) ?? null)
    } catch (e) {
      setError(mensajeDeError(e, 'No se pudo marcar la notificación como leída.'))
    }
  }

  return (
    <Stack spacing={2}>
      {error && <Alert severity="error">{error}</Alert>}
      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1, mb: 2 }}>
          <Typography component="h2" sx={{ fontSize: 18, fontWeight: 700 }}>
            Notificaciones {filas && `(${total})`}
          </Typography>
          <ToggleButtonGroup
            exclusive
            size="small"
            value={estado}
            onChange={(_, v: 'TODAS' | 'NO_LEIDAS' | null) => v && setEstado(v)}
            aria-label="Filtrar notificaciones"
          >
            <ToggleButton value="TODAS">Todas</ToggleButton>
            <ToggleButton value="NO_LEIDAS">No leídas</ToggleButton>
          </ToggleButtonGroup>
        </Stack>

        {filas === null && (
          <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
            <CircularProgress aria-label="Cargando notificaciones" />
          </Box>
        )}
        {filas?.length === 0 && !error && (
          <Typography variant="body2" color="text.secondary">
            {estado === 'NO_LEIDAS' ? 'No tienes notificaciones sin leer.' : 'No tienes notificaciones por ahora.'}
          </Typography>
        )}
        <Stack spacing={1.5}>
          {filas?.map((n) => {
            const sev = SEVERIDAD[n.severidad] ?? SEVERIDAD.INFO
            return (
              <Box
                key={n.id}
                sx={{
                  p: 1.5,
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 1,
                  bgcolor: n.leida ? 'transparent' : 'action.hover',
                }}
              >
                <Stack direction="row" sx={{ alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                  <Chip size="small" color={sev.color} label={sev.texto} />
                  <Typography sx={{ fontWeight: n.leida ? 500 : 700, flex: 1 }}>{n.titulo}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {formatoFechaHora(n.creadaEn)}
                  </Typography>
                </Stack>
                <Typography variant="body2" sx={{ mt: 0.75 }}>
                  {n.mensaje}
                </Typography>
                <Stack direction="row" spacing={2} sx={{ mt: 1, alignItems: 'center' }}>
                  {n.despachoId !== null && (
                    <Link
                      component={RouterLink}
                      to={esCliente ? `/portal-cliente/mis-despachos?despacho=${n.despachoId}` : `/despachos/${n.despachoId}`}
                      onClick={() => void leer(n)}
                    >
                      Ver despacho {n.numeroDespacho}
                    </Link>
                  )}
                  {!n.leida && (
                    <Button size="small" onClick={() => void leer(n)}>
                      Marcar como leída
                    </Button>
                  )}
                </Stack>
              </Box>
            )
          })}
        </Stack>
      </Paper>
    </Stack>
  )
}
