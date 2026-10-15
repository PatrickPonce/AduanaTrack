import { Alert, Box, Button, CircularProgress, Link, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router'

import { obtenerDespacho } from '../api/despachos'
import { mensajeDeError } from '../api/http'
import { EmbarqueForm } from '../components/EmbarqueForm'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip } from '../components/EstadoDespachoChip'
import type { Despacho } from '../types'

/** HU-05: "Información del Embarque Marítimo" de un despacho (/embarques/:despachoId). */
export function EmbarquePage() {
  const { despachoId } = useParams()
  const [despacho, setDespacho] = useState<Despacho | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEncabezado(`Embarques / ${despacho?.numero ?? ''}`, 'Información del Embarque Marítimo')

  useEffect(() => {
    setDespacho(null)
    setError(null)
    obtenerDespacho(Number(despachoId))
      .then(setDespacho)
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el despacho.')))
  }, [despachoId])

  if (error) return <Alert severity="error">{error}</Alert>
  if (!despacho) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando despacho" />
      </Box>
    )
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <Typography variant="body2">
          Despacho{' '}
          <Link component={RouterLink} to={`/despachos/${despacho.id}`} sx={{ fontWeight: 600 }}>
            {despacho.numero ?? 'en borrador'}
          </Link>{' '}
          · {despacho.cliente.razonSocial}
        </Typography>
        <EstadoDespachoChip estado={despacho.estado} />
      </Stack>
      {despacho.estado === 'BORRADOR' ? (
        <Alert
          severity="info"
          action={
            <Button component={RouterLink} to={`/despachos/${despacho.id}/editar`} color="inherit" size="small">
              Completar despacho
            </Button>
          }
        >
          Confirme el despacho con «Guardar y continuar» antes de registrar la información de embarque.
        </Alert>
      ) : (
        <EmbarqueForm despachoId={despacho.id} />
      )}
    </Stack>
  )
}
