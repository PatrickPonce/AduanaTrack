import { Alert, Box, CircularProgress, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from '@mui/material'
import { useEffect, useState } from 'react'

import { listarHitos } from '../api/hitos'
import { mensajeDeError } from '../api/http'
import type { Hito } from '../types'
import { formatoFechaHora } from '../utils/formato'
import { EtiquetaColor } from './EtiquetaColor'
import { TituloSeccion } from './Seccion'

/**
 * HU-10 · T-10.3: tabla «Línea de tiempo del despacho». Cada cambio de estado o de canal agrega un
 * hito con fecha, hora y usuario responsable (CA4); el sistema puede proponer uno («Sugerido»).
 */
export function LineaTiempoDespacho({ despachoId }: { despachoId: number }) {
  const [hitos, setHitos] = useState<Hito[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setCargando(true)
    setError(null)
    listarHitos(despachoId)
      .then(setHitos)
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar la línea de tiempo.')))
      .finally(() => setCargando(false))
  }, [despachoId])

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
        <CircularProgress aria-label="Cargando línea de tiempo" />
      </Box>
    )
  }

  return (
    <Box>
      <TituloSeccion>Línea de tiempo del despacho</TituloSeccion>
      {error && <Alert severity="error">{error}</Alert>}
      {!error && hitos.length === 0 && <Alert severity="info">Todavía no hay hitos registrados.</Alert>}
      {hitos.length > 0 && (
        <TableContainer>
          <Table size="small" aria-label="Línea de tiempo del despacho">
            <TableHead>
              <TableRow>
                <TableCell sx={{ width: 170 }}>Fecha y hora</TableCell>
                <TableCell>Hito</TableCell>
                <TableCell sx={{ width: 200 }}>Usuario</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {hitos.map((h, i) => (
                <TableRow key={`${h.fecha}-${i}`}>
                  <TableCell>{formatoFechaHora(h.fecha)}</TableCell>
                  <TableCell>
                    {h.descripcion} {h.sugerido && <EtiquetaColor texto="Sugerido" tono="naranja" />}
                  </TableCell>
                  <TableCell>{h.usuario}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
