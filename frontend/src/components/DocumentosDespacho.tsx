import DownloadIcon from '@mui/icons-material/FileDownloadOutlined'
import HistoryIcon from '@mui/icons-material/History'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import { useCallback, useEffect, useState } from 'react'

import { listarDocumentos, listarVersiones, urlArchivoDocumento } from '../api/documentos'
import { mensajeDeError } from '../api/http'
import type { DocumentoRequerido, DocumentosDespacho as Documentos, VersionDocumento } from '../types'
import { formatoFechaHora, formatoTamano } from '../utils/formato'
import { EtiquetaColor } from './EtiquetaColor'
import { TituloSeccion } from './Seccion'

interface Props {
  despachoId: number
}

/** HU-06 · T-06.4: pestaña «Documentos» del despacho (contrato C1). */
export function DocumentosDespacho({ despachoId }: Props) {
  const [datos, setDatos] = useState<Documentos | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [versionesDe, setVersionesDe] = useState<DocumentoRequerido | null>(null)

  const cargar = useCallback(async () => {
    try {
      setDatos(await listarDocumentos(despachoId))
      setError(null)
    } catch (e) {
      setError(mensajeDeError(e, 'No se pudieron cargar los documentos del despacho.'))
    }
  }, [despachoId])

  useEffect(() => {
    setDatos(null)
    void cargar()
  }, [cargar])

  if (error && !datos) return <Alert severity="error">{error}</Alert>
  if (!datos) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando documentos" />
      </Box>
    )
  }

  const pendientes = datos.documentos.filter((d) => d.obligatorio && d.estado === 'PENDIENTE')

  return (
    <Box>
      <TituloSeccion
        accion={
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center' }}
            aria-label="Estado general de la documentación"
          >
            <Typography variant="body2" color="text.secondary">
              Estado general
            </Typography>
            {datos.estadoGeneral === 'COMPLETO' ? (
              <EtiquetaColor texto="Completo" tono="verde" />
            ) : (
              <EtiquetaColor texto="Pendiente" tono="naranja" />
            )}
          </Stack>
        }
      >
        Documentos requeridos del despacho
      </TituloSeccion>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {pendientes.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {pendientes.length === 1 ? 'Falta 1 documento obligatorio' : `Faltan ${pendientes.length} documentos obligatorios`}
          : {pendientes.map((d) => d.nombre).join(', ')}. No se podrá registrar la clasificación arancelaria hasta
          cargarlos.
        </Alert>
      )}

      <TableContainer>
        <Table size="small" sx={{ minWidth: 720 }}>
          <TableHead>
            <TableRow>
              <TableCell>Documento</TableCell>
              <TableCell>Obligatorio</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell>Versión</TableCell>
              <TableCell>Fecha de carga</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {datos.documentos.map((d) => (
              <TableRow key={d.tipo}>
                <TableCell>
                  {d.nombre}
                  {d.archivo && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      {d.archivo} · {formatoTamano(d.tamanoBytes)}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>{d.obligatorio ? 'Sí' : 'No'}</TableCell>
                <TableCell>
                  {d.estado === 'CARGADO' ? (
                    <EtiquetaColor texto="Cargado" tono="verde" />
                  ) : (
                    <EtiquetaColor texto="Pendiente" tono={d.obligatorio ? 'naranja' : 'gris'} />
                  )}
                </TableCell>
                <TableCell>{d.version ? `v${d.version}` : '—'}</TableCell>
                <TableCell>
                  {formatoFechaHora(d.cargadoEn)}
                  {d.cargadoPor && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      {d.cargadoPor}
                    </Typography>
                  )}
                </TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  {d.version !== null && (
                    <Tooltip title="Ver versiones">
                      <IconButton
                        size="small"
                        aria-label={`Ver versiones de ${d.nombre}`}
                        onClick={() => setVersionesDe(d)}
                      >
                        <HistoryIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                  {d.documentoId !== null && (
                    <Tooltip title="Descargar PDF vigente">
                      <IconButton
                        size="small"
                        component="a"
                        href={urlArchivoDocumento(despachoId, d.documentoId)}
                        aria-label={`Descargar ${d.nombre}`}
                      >
                        <DownloadIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <VersionesDialog despachoId={despachoId} documento={versionesDe} onCerrar={() => setVersionesDe(null)} />
    </Box>
  )
}

/** Historial del tipo: la versión vigente y las anteriores, que se conservan al reemplazar (CA3). */
function VersionesDialog({
  despachoId,
  documento,
  onCerrar,
}: {
  despachoId: number
  documento: DocumentoRequerido | null
  onCerrar: () => void
}) {
  const [versiones, setVersiones] = useState<VersionDocumento[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!documento) return
    let activo = true
    setVersiones(null)
    setError(null)
    listarVersiones(despachoId, documento.tipo)
      .then((v) => activo && setVersiones(v))
      .catch((e) => activo && setError(mensajeDeError(e, 'No se pudieron cargar las versiones.')))
    return () => {
      activo = false
    }
  }, [despachoId, documento])

  return (
    <Dialog open={documento !== null} onClose={onCerrar} maxWidth="md" fullWidth>
      <DialogTitle>Versiones · {documento?.nombre}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error">{error}</Alert>}
        {!error && !versiones && (
          <Box sx={{ display: 'grid', placeItems: 'center', py: 4 }}>
            <CircularProgress aria-label="Cargando versiones" />
          </Box>
        )}
        {versiones && (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Versión</TableCell>
                  <TableCell>Archivo</TableCell>
                  <TableCell>Cargado por</TableCell>
                  <TableCell>Fecha</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell sx={{ width: 56 }} />
                </TableRow>
              </TableHead>
              <TableBody>
                {versiones.map((v) => (
                  <TableRow key={v.documentoId}>
                    <TableCell>v{v.version}</TableCell>
                    <TableCell>
                      {v.archivo}
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                        {formatoTamano(v.tamanoBytes)}
                      </Typography>
                    </TableCell>
                    <TableCell>{v.cargadoPor}</TableCell>
                    <TableCell>{formatoFechaHora(v.cargadoEn)}</TableCell>
                    <TableCell>
                      {v.vigente ? (
                        <EtiquetaColor texto="Vigente" tono="verde" />
                      ) : (
                        <EtiquetaColor texto="Histórica" tono="gris" />
                      )}
                    </TableCell>
                    <TableCell>
                      <Tooltip title="Descargar PDF">
                        <IconButton
                          size="small"
                          component="a"
                          href={urlArchivoDocumento(despachoId, v.documentoId)}
                          aria-label={`Descargar versión ${v.version}`}
                        >
                          <DownloadIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  )
}
