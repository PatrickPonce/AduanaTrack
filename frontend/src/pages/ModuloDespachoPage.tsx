import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import TrafficOutlinedIcon from '@mui/icons-material/TrafficOutlined'
import {
  Alert,
  Box,
  Chip,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'

import { listarDespachosTablero } from '../api/dashboard'
import { mensajeDeError } from '../api/http'
import { useEncabezado } from '../components/Encabezado'
import { EtiquetaColor, type Tono } from '../components/EtiquetaColor'
import { textoEstado } from '../components/EstadoDespachoChip'
import { COLORES, fadeInUp } from '../theme'
import type { DespachoTablero, EstadoDespacho } from '../types'
import { alcanzoEstado } from '../utils/estados'
import type { Pestana } from './DespachoDetallePage'

export type Modulo = 'documentos' | 'clasificacion' | 'tributos' | 'dam' | 'canal'

interface ConfigModulo {
  titulo: string
  hu: string
  descripcion: string
  icono: ReactNode
  acento: string
  pestana: Pestana
  /** Estado desde el que el módulo se puede trabajar. */
  requiere: EstadoDespacho
  /** Estado que el despacho alcanza cuando el módulo queda completo. */
  completa: EstadoDespacho
}

const MODULOS: Record<Modulo, ConfigModulo> = {
  documentos: {
    titulo: 'Documentos del despacho',
    hu: 'HU-06',
    descripcion: 'Carga y versiones de la factura, el BL, la lista de empaque y la póliza de seguro.',
    icono: <DescriptionOutlinedIcon />,
    acento: '#1a6fdc',
    pestana: 'documentos',
    requiere: 'APERTURA',
    completa: 'DOCUMENTACION_COMPLETA',
  },
  clasificacion: {
    titulo: 'Clasificación arancelaria',
    hu: 'HU-07',
    descripcion: 'Asignación de la partida arancelaria a cada ítem de la factura comercial.',
    icono: <CategoryOutlinedIcon />,
    acento: '#7c3aed',
    pestana: 'clasificacion',
    requiere: 'DOCUMENTACION_COMPLETA',
    completa: 'CLASIFICADO',
  },
  tributos: {
    titulo: 'Tributos y liquidación',
    hu: 'HU-08',
    descripcion: 'Cálculo referencial de CIF, Ad Valorem, IGV, IPM y percepción, y registro del pago.',
    icono: <PaymentsOutlinedIcon />,
    acento: '#d97706',
    pestana: 'liquidacion',
    requiere: 'CLASIFICADO',
    completa: 'TRIBUTOS_PAGADOS',
  },
  dam: {
    titulo: 'Numeración de la DAM',
    hu: 'HU-09',
    descripcion: 'Registro del N° de DAM, su fecha de numeración y el cargo en PDF.',
    icono: <ReceiptLongOutlinedIcon />,
    acento: '#0891b2',
    pestana: 'dam',
    requiere: 'TRIBUTOS_PAGADOS',
    completa: 'DAM_NUMERADA',
  },
  canal: {
    titulo: 'Canal de control',
    hu: 'HU-10',
    descripcion: 'Registro del canal asignado (verde, naranja o rojo) y seguimiento en la línea de tiempo.',
    icono: <TrafficOutlinedIcon />,
    acento: COLORES.verde,
    pestana: 'canal',
    requiere: 'DAM_NUMERADA',
    completa: 'CANAL_ASIGNADO',
  },
}

type Situacion = 'pendiente' | 'completado' | 'espera'
type Filtro = Situacion | 'todos'

const SITUACIONES: Record<Situacion, { texto: string; tono: Tono; orden: number }> = {
  pendiente: { texto: 'Por gestionar', tono: 'naranja', orden: 0 },
  completado: { texto: 'Completado', tono: 'verde', orden: 1 },
  espera: { texto: 'En espera', tono: 'gris', orden: 2 },
}

function situacionDe(d: DespachoTablero, config: ConfigModulo): Situacion {
  if (alcanzoEstado(d.estado, config.completa)) return 'completado'
  if (alcanzoEstado(d.estado, config.requiere)) return 'pendiente'
  return 'espera'
}

/** Sprint 2 (HU-06 a HU-10): lista los despachos según su avance en el módulo y abre la pestaña que corresponde. */
export function ModuloDespachoPage({ modulo }: { modulo: Modulo }) {
  const config = MODULOS[modulo]
  useEncabezado(`Operación aduanera / ${config.hu}`, config.titulo)
  const navegar = useNavigate()
  const ruta = useLocation().pathname
  const [despachos, setDespachos] = useState<DespachoTablero[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [filtro, setFiltro] = useState<Filtro>('todos')

  useEffect(() => {
    listarDespachosTablero({})
      .then(setDespachos)
      .catch((e) => {
        setDespachos([])
        setError(mensajeDeError(e, 'No se pudo cargar la lista de despachos.'))
      })
  }, [])

  const filas = useMemo(
    () =>
      (despachos ?? [])
        .map((d) => ({ d, situacion: situacionDe(d, config) }))
        .sort((a, b) => SITUACIONES[a.situacion].orden - SITUACIONES[b.situacion].orden),
    [despachos, config],
  )
  const conteo = (s: Situacion) => filas.filter((f) => f.situacion === s).length
  const visibles = filtro === 'todos' ? filas : filas.filter((f) => f.situacion === filtro)

  function abrir(id: number) {
    navegar(`/despachos/${id}`, { state: { pestana: config.pestana, volverA: ruta } })
  }

  const resumen: { clave: Situacion; titulo: string; ayuda: string }[] = [
    { clave: 'pendiente', titulo: 'Por gestionar', ayuda: 'Listos para trabajar en este módulo' },
    { clave: 'completado', titulo: 'Completados', ayuda: 'Ya pasaron esta etapa' },
    { clave: 'espera', titulo: 'En espera', ayuda: `Aún no llegan a «${textoEstado(config.requiere)}»` },
  ]

  return (
    <Stack spacing={2.5}>
      <Paper
        sx={{
          p: { xs: 2.5, md: 3 },
          position: 'relative',
          overflow: 'hidden',
          animation: `${fadeInUp} .4s ease`,
          background: `linear-gradient(135deg, #fff 55%, ${config.acento}14 100%)`,
        }}
      >
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Box
            aria-hidden
            sx={{
              width: 52,
              height: 52,
              flexShrink: 0,
              borderRadius: 3,
              display: 'grid',
              placeItems: 'center',
              color: '#fff',
              background: `linear-gradient(135deg, ${config.acento}, ${config.acento}b3)`,
              boxShadow: `0 8px 20px ${config.acento}40`,
              '& svg': { fontSize: 28 },
            }}
          >
            {config.icono}
          </Box>
          <Box sx={{ minWidth: 0 }}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
              <Typography component="h2" sx={{ fontSize: 20, fontWeight: 700 }}>
                {config.titulo}
              </Typography>
              <Chip label={config.hu} size="small" sx={{ bgcolor: `${config.acento}1f`, color: config.acento }} />
            </Stack>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {config.descripcion}
            </Typography>
          </Box>
        </Stack>
      </Paper>

      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' } }}>
        {resumen.map((r, i) => (
          <Paper
            key={r.clave}
            sx={{
              px: 2.5,
              py: 2,
              borderTop: `3px solid ${r.clave === 'pendiente' ? config.acento : r.clave === 'completado' ? '#16a34a' : '#94a3b8'}`,
              animation: `${fadeInUp} .4s ease`,
              animationDelay: `${0.05 + i * 0.06}s`,
              animationFillMode: 'backwards',
            }}
          >
            <Typography sx={{ fontSize: 28, fontWeight: 700, lineHeight: 1.15 }} aria-label={r.titulo}>
              {despachos ? conteo(r.clave) : '—'}
            </Typography>
            <Typography sx={{ fontSize: 13.5, fontWeight: 600 }}>{r.titulo}</Typography>
            <Typography variant="caption" color="text.secondary">
              {r.ayuda}
            </Typography>
          </Paper>
        ))}
      </Box>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <Stack
          direction="row"
          sx={{ alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1.5, mb: 2 }}
        >
          <Typography component="h3" sx={{ fontSize: 15, fontWeight: 700 }}>
            Despachos
          </Typography>
          <ToggleButtonGroup
            size="small"
            exclusive
            value={filtro}
            onChange={(_, v: Filtro | null) => v && setFiltro(v)}
            aria-label="Filtrar por situación"
          >
            <ToggleButton value="todos">Todos</ToggleButton>
            <ToggleButton value="pendiente">Por gestionar</ToggleButton>
            <ToggleButton value="completado">Completados</ToggleButton>
            <ToggleButton value="espera">En espera</ToggleButton>
          </ToggleButtonGroup>
        </Stack>

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
                <TableCell>Aduana</TableCell>
                <TableCell>Estado actual</TableCell>
                <TableCell>Situación en el módulo</TableCell>
                <TableCell aria-label="Abrir" />
              </TableRow>
            </TableHead>
            <TableBody>
              {visibles.map(({ d, situacion }) => (
                <TableRow
                  key={d.id}
                  hover
                  tabIndex={0}
                  onClick={() => abrir(d.id)}
                  onKeyDown={(e) => e.key === 'Enter' && abrir(d.id)}
                  sx={{
                    cursor: 'pointer',
                    '&:focus-visible': { outline: 2, outlineColor: 'primary.main' },
                    '&:hover .abrir': { transform: 'translateX(3px)', color: config.acento },
                  }}
                >
                  <TableCell sx={{ whiteSpace: 'nowrap', fontWeight: 600 }}>{d.numero}</TableCell>
                  <TableCell>{d.cliente}</TableCell>
                  <TableCell>{d.aduana ?? '—'}</TableCell>
                  <TableCell>{textoEstado(d.estado)}</TableCell>
                  <TableCell>
                    <EtiquetaColor texto={SITUACIONES[situacion].texto} tono={SITUACIONES[situacion].tono} />
                  </TableCell>
                  <TableCell align="right" sx={{ width: 40 }}>
                    <ChevronRightIcon
                      className="abrir"
                      fontSize="small"
                      sx={{ color: 'text.secondary', transition: 'transform .18s ease, color .18s ease' }}
                    />
                  </TableCell>
                </TableRow>
              ))}
              {despachos !== null && visibles.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No hay despachos en esta situación.
                  </TableCell>
                </TableRow>
              )}
              {despachos === null && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Cargando…
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Stack>
  )
}
