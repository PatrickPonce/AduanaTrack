import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { Alert, Box, Button, CircularProgress, Stack, Tab, Tabs, Typography } from '@mui/material'
import { useEffect, useState, type ReactNode } from 'react'
import { Link as RouterLink, useLocation, useParams } from 'react-router'

import { obtenerDespacho } from '../api/despachos'
import { mensajeDeError } from '../api/http'
import { CartaPoderChip } from '../components/CartaPoderChip'
import { EmbarqueForm } from '../components/EmbarqueForm'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip } from '../components/EstadoDespachoChip'
import { Tarjeta } from '../components/Tarjeta'
import type { Despacho } from '../types'
import { formatoFecha } from '../utils/formato'

interface EstadoNavegacion {
  mensaje?: string
  /** Filtros del Dashboard (?aduanaId=&clienteId=) para volver con ellos intactos (T-02.4). */
  volverA?: string
}

/** HU-03 · T-03.5: detalle del despacho con su estado y las pestañas de datos y embarque. */
export function DespachoDetallePage() {
  const { id } = useParams()
  const ubicacion = useLocation()
  const navegacion = (ubicacion.state as EstadoNavegacion | null) ?? {}
  const [despacho, setDespacho] = useState<Despacho | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pestana, setPestana] = useState(0)
  // La pestaña «Embarque / BL» corresponde a HU-05; los datos generales, a HU-03.
  const numero = despacho ? (despacho.numero ?? 'Borrador') : '…'
  useEncabezado(pestana === 1 ? 'HU-05' : 'HU-03', pestana === 1 ? 'Embarques' : 'Despachos', numero)

  useEffect(() => {
    setDespacho(null)
    setError(null)
    obtenerDespacho(Number(id))
      .then(setDespacho)
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el despacho.')))
  }, [id])

  const volver = (
    <Button component={RouterLink} to={navegacion.volverA ?? '/dashboard'} startIcon={<ArrowBackIcon />}>
      Volver al panel
    </Button>
  )

  if (error) {
    return (
      <Stack spacing={2}>
        <Alert severity="error">{error}</Alert>
        <Box>{volver}</Box>
      </Stack>
    )
  }
  if (!despacho) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando despacho" />
      </Box>
    )
  }

  const esBorrador = despacho.estado === 'BORRADOR'

  return (
    <Stack spacing={2.5}>
      <Box>{volver}</Box>
      {navegacion.mensaje && <Alert severity="success">{navegacion.mensaje}</Alert>}

      <Tarjeta>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap', rowGap: 1, mb: 2.5 }}>
          <Typography variant="h5" component="h2">
            {despacho.numero ?? 'Despacho en borrador'}
          </Typography>
          <EstadoDespachoChip estado={despacho.estado} size="medium" />
          {esBorrador && (
            <Button component={RouterLink} to={`/despachos/${despacho.id}/editar`} variant="contained" size="small">
              Completar despacho
            </Button>
          )}
        </Stack>
        <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' } }}>
          <Dato titulo="Cliente / Importador">
            {despacho.cliente.razonSocial}
            <Typography variant="caption" sx={{ display: 'block' }} color="text.secondary">
              RUC {despacho.cliente.ruc}
            </Typography>
          </Dato>
          <Dato titulo="Régimen aduanero">
            {despacho.regimen ? `${despacho.regimen.codigo} - ${despacho.regimen.descripcion}` : '—'}
          </Dato>
          <Dato titulo="Aduana de destino">
            {despacho.aduana ? `${despacho.aduana.codigo} - ${despacho.aduana.descripcion}` : '—'}
          </Dato>
          <Dato titulo={esBorrador ? 'Fecha de registro' : 'Fecha de apertura'}>{formatoFecha(despacho.creadoEn)}</Dato>
        </Box>
      </Tarjeta>

      <Tabs value={pestana} onChange={(_, v: number) => setPestana(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Datos generales" />
        <Tab label="Embarque / BL" />
      </Tabs>

      {pestana === 0 && (
        <Tarjeta titulo="Datos generales del despacho">
          <Box sx={{ display: 'grid', gap: 2.5, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
            <Dato titulo="Referencia del cliente">{despacho.referenciaCliente ?? '—'}</Dato>
            <Dato titulo="Agente responsable">{despacho.agente}</Dato>
            <Dato titulo="Carta poder del cliente">
              <CartaPoderChip estado={despacho.cliente.estadoCartaPoder} />
            </Dato>
            <Dato titulo="Canal de control">{despacho.canalControl ?? 'Pendiente'}</Dato>
            <Box sx={{ gridColumn: '1 / -1' }}>
              <Dato titulo="Descripción general de la mercancía">{despacho.descripcionMercancia ?? '—'}</Dato>
            </Box>
          </Box>
        </Tarjeta>
      )}

      {pestana === 1 &&
        (esBorrador ? (
          <Alert severity="info">
            Confirme el despacho con «Guardar y continuar» antes de registrar la información de embarque.
          </Alert>
        ) : (
          <EmbarqueForm despachoId={despacho.id} />
        ))}
    </Stack>
  )
}

function Dato({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <Box>
      <Typography variant="caption" component="div" sx={{ fontWeight: 600, color: '#475569', mb: 0.25 }}>
        {titulo}
      </Typography>
      <Typography component="div">{children}</Typography>
    </Box>
  )
}
