import AddIcon from '@mui/icons-material/Add'
import SearchIcon from '@mui/icons-material/Search'
import {
  Alert,
  Button,
  InputAdornment,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router'

import { buscarClientes } from '../api/clientes'
import { mensajeDeError } from '../api/http'
import { CartaPoderChip } from '../components/CartaPoderChip'
import type { ClienteResumen, Pagina } from '../types'

const TAMANO = 10

/** HU-04 · T-04.4: listado de clientes con búsqueda y estado de la carta poder. */
export function ClientesPage() {
  const navegar = useNavigate()
  const [busqueda, setBusqueda] = useState('')
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState<Pagina<ClienteResumen> | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let activo = true
    const espera = setTimeout(() => {
      buscarClientes(busqueda.trim(), pagina, TAMANO)
        .then((d) => {
          if (activo) {
            setDatos(d)
            setError(null)
          }
        })
        .catch((e) => activo && setError(mensajeDeError(e, 'No se pudo cargar la lista de clientes.')))
    }, 250)
    return () => {
      activo = false
      clearTimeout(espera)
    }
  }, [busqueda, pagina])

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
        <Typography variant="h5" component="h1">
          Clientes / Importadores
        </Typography>
        <Button component={RouterLink} to="/clientes/nuevo" variant="contained" startIcon={<AddIcon />}>
          Nuevo cliente
        </Button>
      </Stack>

      <TextField
        label="Buscar por RUC o razón social"
        value={busqueda}
        onChange={(e) => {
          setBusqueda(e.target.value)
          setPagina(0)
        }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          },
        }}
      />

      {error && <Alert severity="error">{error}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>RUC</TableCell>
              <TableCell>Razón social</TableCell>
              <TableCell>Carta poder</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {datos?.contenido.map((c) => (
              <TableRow
                key={c.id}
                hover
                tabIndex={0}
                sx={{ cursor: 'pointer' }}
                onClick={() => navegar(`/clientes/${c.id}`)}
                onKeyDown={(e) => e.key === 'Enter' && navegar(`/clientes/${c.id}`)}
              >
                <TableCell>{c.ruc}</TableCell>
                <TableCell>{c.razonSocial}</TableCell>
                <TableCell>
                  <CartaPoderChip estado={c.estadoCartaPoder} />
                </TableCell>
              </TableRow>
            ))}
            {datos && datos.contenido.length === 0 && (
              <TableRow>
                <TableCell colSpan={3} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  No se encontraron clientes.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={datos?.totalElementos ?? 0}
          page={pagina}
          rowsPerPage={TAMANO}
          rowsPerPageOptions={[TAMANO]}
          onPageChange={(_, p) => setPagina(p)}
        />
      </TableContainer>
    </Stack>
  )
}
