import { Alert, Box, CircularProgress, Link, Paper, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import { Link as RouterLink, useNavigate, useParams } from 'react-router'

import { obtenerDespacho } from '../api/despachos'
import { mensajeDeError } from '../api/http'
import { useAuth } from '../auth/AuthContext'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip } from '../components/EstadoDespachoChip'
import { CanalChip } from '../components/EtiquetaColor'
import { LevanteDespacho } from '../components/LevanteDespacho'
import type { Despacho } from '../types'
import type { EstadoNavegacion } from './DespachoDetallePage'

/**
 * HU-13 · T-13.3: sección «Levante» de un despacho (/levante/:despachoId), adonde llevan «Ir a levante» de
 * la inspección conforme y «Ir al módulo de levante» del canal verde. «Registrar cita de retiro» abre el
 * despacho en la pestaña «Citas» con el tipo Retiro elegido.
 */
export function LevantePage() {
  const { despachoId } = useParams()
  const navegar = useNavigate()
  const { usuario } = useAuth()
  const [despacho, setDespacho] = useState<Despacho | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEncabezado(`Levante / ${despacho?.numero ?? ''}`, 'Registro del Levante de la Mercancía')

  useEffect(() => {
    setDespacho(null)
    setError(null)
    obtenerDespacho(Number(despachoId))
      .then(setDespacho)
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el despacho.')))
  }, [despachoId])

  function recargarDespacho() {
    obtenerDespacho(Number(despachoId))
      .then(setDespacho)
      .catch(() => {
        // La cabecera conserva el estado anterior; se actualiza al volver a abrir el despacho.
      })
  }

  if (error) return <Alert severity="error">{error}</Alert>
  if (!despacho) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando despacho" />
      </Box>
    )
  }

  const irACitaDeRetiro = () => {
    const estado: EstadoNavegacion = { pestana: 'citas', tipoCita: 'RETIRO', volverA: `/levante/${despacho.id}` }
    navegar(`/despachos/${despacho.id}`, { state: estado })
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
        <Typography variant="body2">
          Despacho{' '}
          <Link component={RouterLink} to={`/despachos/${despacho.id}`} sx={{ fontWeight: 600 }}>
            {despacho.numero ?? 'en borrador'}
          </Link>{' '}
          · {despacho.cliente.razonSocial}
        </Typography>
        <EstadoDespachoChip estado={despacho.estado} />
        <CanalChip canal={despacho.canalControl} />
      </Stack>
      {despacho.estado === 'BORRADOR' ? (
        <Alert severity="info">Confirme el despacho antes de registrar el levante.</Alert>
      ) : (
        <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
          <LevanteDespacho
            despachoId={despacho.id}
            estado={despacho.estado}
            puedeEditar={usuario?.rol !== 'CLIENTE'}
            onLevanteRegistrado={recargarDespacho}
            onRegistrarRetiro={irACitaDeRetiro}
          />
        </Paper>
      )}
    </Stack>
  )
}
