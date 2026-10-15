import {
  Alert,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

import { listarDespachosTablero } from '../api/dashboard'
import { mensajeDeError } from '../api/http'
import { useEncabezado } from '../components/Encabezado'
import { EtiquetaColor } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import type { DespachoTablero } from '../types'
import { formatoFecha } from '../utils/formato'

/** Despachos abiertos y su embarque/BL: punto de entrada al formulario de HU-05. */
export function EmbarquesPage() {
  useEncabezado('Embarques', 'Embarques Marítimos')
  const navegar = useNavigate()
  const [despachos, setDespachos] = useState<DespachoTablero[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listarDespachosTablero({})
      .then(setDespachos)
      .catch((e) => {
        setDespachos([])
        setError(mensajeDeError(e, 'No se pudo cargar la lista de despachos.'))
      })
  }, [])

  const abrir = (id: number) => navegar(`/embarques/${id}`)

  return (
    <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
      <TituloSeccion>Despachos y su conocimiento de embarque</TituloSeccion>
      {error && (
        <Alert severity="error" sx={{ mb: 1.5 }}>
          {error}
        </Alert>
      )}
      <TableContainer>
        <Table size="small" sx={{ '& td, & th': { py: 1.25 } }}>
          <TableHead>
            <TableRow>
              <TableCell>N° Despacho</TableCell>
              <TableCell>Cliente</TableCell>
              <TableCell>N° de BL</TableCell>
              <TableCell>Contenedores</TableCell>
              <TableCell>ETA</TableCell>
              <TableCell>Embarque</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {despachos?.map((d) => (
              <TableRow
                key={d.id}
                hover
                tabIndex={0}
                onClick={() => abrir(d.id)}
                onKeyDown={(e) => e.key === 'Enter' && abrir(d.id)}
                sx={{ cursor: 'pointer' }}
              >
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{d.numero}</TableCell>
                <TableCell>{d.cliente}</TableCell>
                <TableCell>{d.numeroBl ?? '—'}</TableCell>
                <TableCell>{d.totalContenedores || '—'}</TableCell>
                <TableCell>{formatoFecha(d.fechaEta)}</TableCell>
                <TableCell>
                  {d.numeroBl ? (
                    <EtiquetaColor texto="Registrado" tono="verde" />
                  ) : (
                    <EtiquetaColor texto="Pendiente" tono="naranja" />
                  )}
                </TableCell>
              </TableRow>
            ))}
            {despachos?.length === 0 && !error && (
              <TableRow>
                <TableCell colSpan={6}>
                  <Typography color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                    Aún no hay despachos abiertos.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  )
}
