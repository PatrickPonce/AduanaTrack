import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { Alert, Box, Button, CircularProgress, Paper, Stack, Tab, Tabs, Typography } from '@mui/material'
import { useEffect, useState, type ReactNode } from 'react'
import { Link as RouterLink, useLocation, useParams } from 'react-router'

import { obtenerDespacho } from '../api/despachos'
import { mensajeDeError } from '../api/http'
import { useAuth } from '../auth/AuthContext'
import { CartaPoderChip } from '../components/CartaPoderChip'
import { DocumentosDespacho } from '../components/DocumentosDespacho'
import { useEncabezado } from '../components/Encabezado'
import { EstadoDespachoChip } from '../components/EstadoDespachoChip'
import { CanalChip } from '../components/EtiquetaColor'
import { DOS_COLUMNAS, TituloSeccion } from '../components/Seccion'
import type { Despacho } from '../types'
import { formatoFecha } from '../utils/formato'
import { MODALIDADES_DESPACHO, TIPOS_OPERACION, textoOpcion } from '../utils/opciones'

interface EstadoNavegacion {
  mensaje?: string
  /** Filtros del Dashboard (?aduanaId=&clienteId=) para volver con ellos intactos (T-02.4). */
  volverA?: string
}

type Pestana = 'generales' | 'documentos'

/** HU-03 · T-03.5 y HU-02 · T-02.4: detalle completo del despacho con su estado; HU-06 · T-06.4: pestaña «Documentos». */
export function DespachoDetallePage() {
  const { id } = useParams()
  const ubicacion = useLocation()
  const { usuario } = useAuth()
  const navegacion = (ubicacion.state as EstadoNavegacion | null) ?? {}
  const [despacho, setDespacho] = useState<Despacho | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pestana, setPestana] = useState<Pestana>('generales')

  useEncabezado(`Despachos / ${despacho?.numero ?? 'Borrador'}`, 'Detalle del Despacho de Importación')

  useEffect(() => {
    setDespacho(null)
    setError(null)
    setPestana('generales')
    obtenerDespacho(Number(id))
      .then(setDespacho)
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el despacho.')))
  }, [id])

  /** Tras un cambio de estado (p. ej. DOCUMENTACION_COMPLETA) se actualiza la cabecera sin perder la pestaña. */
  function recargarDespacho() {
    obtenerDespacho(Number(id))
      .then(setDespacho)
      .catch(() => {
        // La cabecera conserva el estado anterior; se actualiza al volver a abrir el despacho.
      })
  }

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
  const registra = usuario?.rol !== 'CLIENTE'
  // Un borrador todavía no tiene expediente: el API responde 409 DESPACHO_EN_BORRADOR.
  const visible = esBorrador ? 'generales' : pestana

  return (
    <Stack spacing={2}>
      <Box>{volver}</Box>
      {navegacion.mensaje && <Alert severity="success">{navegacion.mensaje}</Alert>}

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1, mb: 2 }}>
          <Typography component="h2" sx={{ fontSize: 20, fontWeight: 700 }}>
            {despacho.numero ?? 'Despacho en borrador'}
          </Typography>
          <EstadoDespachoChip estado={despacho.estado} size="medium" />
          <Box sx={{ flexGrow: 1 }} />
          {esBorrador && registra && (
            <Button component={RouterLink} to={`/despachos/${despacho.id}/editar`} variant="contained">
              Completar despacho
            </Button>
          )}
          {!esBorrador && (
            <Button component={RouterLink} to={`/embarques/${despacho.id}`} variant="contained">
              Información de embarque (BL)
            </Button>
          )}
        </Stack>

        {!esBorrador && (
          <Tabs
            value={visible}
            onChange={(_, v: Pestana) => setPestana(v)}
            sx={{ mb: 2.5, borderBottom: 1, borderColor: 'divider' }}
          >
            <Tab value="generales" label="Datos generales" />
            <Tab value="documentos" label="Documentos" />
          </Tabs>
        )}

        {visible === 'documentos' && (
          <DocumentosDespacho
            despachoId={despacho.id}
            puedeCargar={registra}
            onDocumentacionCompleta={recargarDespacho}
          />
        )}

        {visible === 'generales' && (
          <>
            <TituloSeccion>Datos generales del despacho</TituloSeccion>
            <Box sx={DOS_COLUMNAS}>
              <Dato titulo="Cliente / Importador">
                {despacho.cliente.razonSocial}
                <Typography variant="caption" sx={{ display: 'block' }} color="text.secondary">
                  RUC {despacho.cliente.ruc}
                </Typography>
              </Dato>
              <Dato titulo="Carta poder del cliente">
                <CartaPoderChip estado={despacho.cliente.estadoCartaPoder} />
              </Dato>
              <Dato titulo="Tipo de operación">{textoOpcion(TIPOS_OPERACION, despacho.tipoOperacion)}</Dato>
              <Dato titulo="Régimen aduanero (referencial)">
                {despacho.regimen ? `${despacho.regimen.codigo} - ${despacho.regimen.descripcion}` : '—'}
              </Dato>
              <Dato titulo="Aduana de destino (referencial)">
                {despacho.aduana ? `${despacho.aduana.codigo} - ${despacho.aduana.descripcion}` : '—'}
              </Dato>
              <Dato titulo={esBorrador ? 'Fecha de registro' : 'Fecha de apertura'}>{formatoFecha(despacho.creadoEn)}</Dato>
              <Dato titulo="Modalidad de despacho">{textoOpcion(MODALIDADES_DESPACHO, despacho.modalidadDespacho)}</Dato>
              <Dato titulo="Agente responsable">{despacho.agente}</Dato>
              <Dato titulo="Referencia del cliente">{despacho.referenciaCliente ?? '—'}</Dato>
              <Dato titulo="Canal de control">
                <CanalChip canal={despacho.canalControl} />
              </Dato>
              <Box sx={{ gridColumn: '1 / -1' }}>
                <Dato titulo="Descripción general de la mercancía">{despacho.descripcionMercancia ?? '—'}</Dato>
              </Box>
            </Box>
          </>
        )}
      </Paper>

      {esBorrador && (
        <Alert severity="info">
          Confirme el despacho con «Guardar y continuar» antes de registrar la información de embarque.
        </Alert>
      )}
    </Stack>
  )
}

function Dato({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <Box>
      <Typography component="div" sx={{ fontSize: 12.5, fontWeight: 600, color: '#334155', mb: 0.5 }}>
        {titulo}
      </Typography>
      <Typography component="div">{children}</Typography>
    </Box>
  )
}
