import DownloadIcon from '@mui/icons-material/FileDownloadOutlined'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined'
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined'
import { Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material'
import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'

import { leerError, mensajeDeError } from '../api/http'
import {
  exportarReporte,
  obtenerIndicadoresGestion,
  type FormatoReporte,
  type IndicadoresGestion,
  type RangoFechas,
} from '../api/reportes'
import { useAuth } from '../auth/AuthContext'
import { useEncabezado } from '../components/Encabezado'
import { EnvioProgramadoReporte } from '../components/EnvioProgramadoReporte'
import { EtiquetaColor, type Tono } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import { COLORES, fadeInUp } from '../theme'
import { formatoFecha, hoyIso } from '../utils/formato'

/** Máximo del rango, en meses (regla de HU-15, igual que el backend). */
export const MAX_MESES = 12

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

const CANALES: { canal: string; texto: string; tono: Tono }[] = [
  { canal: 'VERDE', texto: 'Verde', tono: 'verde' },
  { canal: 'NARANJA', texto: 'Naranja', tono: 'naranja' },
  { canal: 'ROJO', texto: 'Rojo', tono: 'rojo' },
]

/** Suma meses a una fecha AAAA-MM-DD como LocalDate.plusMonths: si el día no existe, queda el último del mes. */
export function sumarMeses(iso: string, meses: number): string {
  const [anio, mes, dia] = iso.split('-').map(Number)
  const total = anio! * 12 + (mes! - 1) + meses
  const anioNuevo = Math.floor(total / 12)
  const mesNuevo = (total % 12) + 1
  const ultimoDia = new Date(Date.UTC(anioNuevo, mesNuevo, 0)).getUTCDate()
  const diaNuevo = Math.min(dia!, ultimoDia)
  return `${anioNuevo}-${String(mesNuevo).padStart(2, '0')}-${String(diaNuevo).padStart(2, '0')}`
}

/** Rango con el que abre la página: desde el primer día de hace dos meses hasta hoy. */
export function rangoInicial(hoy: string = hoyIso()): RangoFechas {
  return { desde: `${sumarMeses(hoy, -2).slice(0, 7)}-01`, hasta: hoy }
}

/** Mismas reglas que el backend: las dos fechas, «desde» no mayor que «hasta» y como máximo 12 meses. */
export function validarRango({ desde, hasta }: RangoFechas): Record<string, string> {
  const errores: Record<string, string> = {}
  if (!desde) errores.desde = 'Indique la fecha «Desde»'
  if (!hasta) errores.hasta = 'Indique la fecha «Hasta»'
  if (desde && hasta) {
    if (desde > hasta) errores.desde = '«Desde» no puede ser posterior a «Hasta»'
    else if (hasta > sumarMeses(desde, MAX_MESES)) errores.hasta = `El rango no puede pasar de ${MAX_MESES} meses`
  }
  return errores
}

/** "2026-09" → "sep 2026". */
export function textoMes(mes: string): string {
  const [anio, numero] = mes.split('-')
  return `${MESES[Number(numero) - 1] ?? numero} ${anio}`
}

/** 9.4 → "9,4 días"; sin despachos con levante en el rango, "—". */
export function textoTiempoPromedio(dias: number | null): string {
  if (dias === null) return '—'
  return `${dias.toFixed(1).replace('.', ',')} ${dias === 1 ? 'día' : 'días'}`
}

/**
 * HU-15 · T-15.4: página «Reportes». El jefe de operaciones elige un rango de fechas y ve el total de
 * despachos, los que tienen levante y el tiempo promedio de despacho (CA1), y el gráfico «Despachos por
 * mes» con el resumen por canal, que se actualizan al cambiar el rango (CA2). El reporte se exporta a
 * Excel y PDF (CA3) y se puede programar su envío por correo (CA4). Contrato C10.
 */
export function ReportesPage() {
  useEncabezado('Reportes', 'Reportes e Indicadores de Gestión')
  const { usuario } = useAuth()
  const [form, setForm] = useState<RangoFechas>(() => rangoInicial())
  const [rango, setRango] = useState<RangoFechas>(form)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [indicadores, setIndicadores] = useState<IndicadoresGestion | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [exportando, setExportando] = useState<FormatoReporte | null>(null)
  const [errorExportacion, setErrorExportacion] = useState<string | null>(null)

  const consultar = useCallback(async (actual: RangoFechas) => {
    setCargando(true)
    setError(null)
    try {
      setIndicadores(await obtenerIndicadoresGestion(actual))
    } catch (e) {
      const { status, body } = leerError(e)
      setIndicadores(null)
      if (status === 400 && body?.campos) setErrores(body.campos)
      // Mientras el cálculo de indicadores (T-15.1) no esté publicado, la exportación sí funciona.
      else if (status === 404) setError('Los indicadores todavía no están disponibles en el servidor. Puede exportar el reporte.')
      else setError(mensajeDeError(e, 'No se pudieron cargar los indicadores del rango.'))
    } finally {
      setCargando(false)
    }
  }, [])

  // CA2: cada vez que se aplica un rango se vuelven a pedir los indicadores.
  useEffect(() => {
    if (usuario && usuario.rol !== 'CLIENTE') void consultar(rango)
  }, [consultar, rango, usuario])

  function cambiar(campo: keyof RangoFechas, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrores({})
  }

  function aplicar(evento: FormEvent) {
    evento.preventDefault()
    const nuevos = validarRango(form)
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return
    setRango({ ...form })
  }

  /** CA3: descarga el reporte del rango aplicado (indicadores y tabla de despachos). */
  async function exportar(formato: FormatoReporte) {
    setExportando(formato)
    setErrorExportacion(null)
    try {
      await exportarReporte(formato, rango)
    } catch (e) {
      const { body } = leerError(e)
      const campos = body?.campos ? Object.values(body.campos).join(' ') : null
      const formatoTexto = formato === 'pdf' ? 'PDF' : 'Excel'
      setErrorExportacion(campos || mensajeDeError(e, `No se pudo exportar el reporte a ${formatoTexto}.`))
    } finally {
      setExportando(null)
    }
  }

  if (usuario?.rol === 'CLIENTE') {
    return <Alert severity="info">Los reportes de gestión son para el administrador y el agente de aduana.</Alert>
  }

  const maximo = Math.max(1, ...(indicadores?.despachosPorMes.map((m) => m.cantidad) ?? []))

  return (
    <Stack spacing={2.5}>
      <Paper component="form" onSubmit={aplicar} noValidate sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion
          accion={
            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
              <Button
                variant="outlined"
                size="small"
                startIcon={<DownloadIcon />}
                onClick={() => void exportar('xlsx')}
                disabled={exportando !== null}
              >
                {exportando === 'xlsx' ? 'Exportando…' : 'Exportar a Excel'}
              </Button>
              <Button
                variant="outlined"
                size="small"
                startIcon={<DownloadIcon />}
                onClick={() => void exportar('pdf')}
                disabled={exportando !== null}
              >
                {exportando === 'pdf' ? 'Exportando…' : 'Exportar a PDF'}
              </Button>
            </Stack>
          }
        >
          Rango del reporte
        </TituloSeccion>
        {errorExportacion && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorExportacion}
          </Alert>
        )}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'flex-start' } }}>
          <TextField
            label="Desde"
            type="date"
            value={form.desde}
            onChange={(e) => cambiar('desde', e.target.value)}
            error={Boolean(errores.desde)}
            helperText={errores.desde}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            label="Hasta"
            type="date"
            value={form.hasta}
            onChange={(e) => cambiar('hasta', e.target.value)}
            error={Boolean(errores.hasta)}
            helperText={errores.hasta}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <Button type="submit" variant="contained" sx={{ height: 40, mt: { sm: 0.5 } }} disabled={cargando}>
            Aplicar
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }} role="status">
          Mostrando del {formatoFecha(rango.desde)} al {formatoFecha(rango.hasta)} (máximo {MAX_MESES} meses).
        </Typography>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' } }}>
        <Tarjeta
          titulo="Total de despachos"
          valor={indicadores ? String(indicadores.totalDespachos) : '—'}
          acento={COLORES.azul}
          icono={<LocalShippingOutlinedIcon />}
          orden={0}
        />
        <Tarjeta
          titulo="Con levante"
          valor={indicadores ? String(indicadores.conLevante) : '—'}
          acento="#16a34a"
          icono={<FactCheckOutlinedIcon />}
          orden={1}
        />
        <Tarjeta
          titulo="Tiempo promedio de despacho"
          valor={indicadores ? textoTiempoPromedio(indicadores.tiempoPromedioDias) : '—'}
          ayuda="De la apertura al levante, solo despachos con levante"
          acento="#d97706"
          icono={<TimerOutlinedIcon />}
          orden={2}
        />
      </Box>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion
          accion={
            indicadores && (
              <Stack direction="row" spacing={1} aria-label="Despachos por canal" sx={{ flexWrap: 'wrap', gap: 1 }}>
                {CANALES.map((c) => (
                  <EtiquetaColor key={c.canal} texto={`${c.texto}: ${indicadores.porCanal[c.canal] ?? 0}`} tono={c.tono} />
                ))}
              </Stack>
            )
          }
        >
          Despachos por mes
        </TituloSeccion>
        {cargando && (
          <Box sx={{ display: 'grid', placeItems: 'center', py: 4 }}>
            <CircularProgress aria-label="Cargando indicadores" />
          </Box>
        )}
        {/* Sin indicadores (servidor caído o no publicado) el panel lo dice en vez de quedar en blanco. */}
        {!cargando && !indicadores && (
          <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
            El gráfico se mostrará cuando el servidor entregue los indicadores del rango.
          </Typography>
        )}
        {!cargando && indicadores && indicadores.despachosPorMes.every((m) => m.cantidad === 0) && (
          <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
            No hay despachos abiertos en el rango.
          </Typography>
        )}
        {!cargando && indicadores && indicadores.despachosPorMes.some((m) => m.cantidad > 0) && (
          <Box
            role="list"
            aria-label="Gráfico de despachos por mes"
            sx={{ display: 'flex', alignItems: 'flex-end', gap: { xs: 1, sm: 2 }, height: 220, pt: 3, overflowX: 'auto' }}
          >
            {indicadores.despachosPorMes.map((m) => (
              <Box
                key={m.mes}
                role="listitem"
                aria-label={`${textoMes(m.mes)}: ${m.cantidad} ${m.cantidad === 1 ? 'despacho' : 'despachos'}`}
                sx={{ flex: '1 0 48px', maxWidth: 96, height: '100%', display: 'flex', flexDirection: 'column' }}
              >
                <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
                  <Typography aria-hidden sx={{ fontSize: 12.5, fontWeight: 700, textAlign: 'center', mb: 0.5 }}>
                    {m.cantidad}
                  </Typography>
                  <Box
                    aria-hidden
                    sx={{
                      height: `${(m.cantidad / maximo) * 100}%`,
                      minHeight: m.cantidad > 0 ? 4 : 0,
                      borderRadius: '6px 6px 0 0',
                      background: `linear-gradient(180deg, ${COLORES.azul}, ${COLORES.azul}b3)`,
                      transition: 'height .3s ease',
                    }}
                  />
                </Box>
                <Typography aria-hidden variant="caption" color="text.secondary" sx={{ textAlign: 'center', mt: 0.75 }}>
                  {textoMes(m.mes)}
                </Typography>
              </Box>
            ))}
          </Box>
        )}
      </Paper>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <EnvioProgramadoReporte />
      </Paper>
    </Stack>
  )
}

function Tarjeta({
  titulo,
  valor,
  ayuda,
  acento,
  icono,
  orden,
}: {
  titulo: string
  valor: string
  ayuda?: string
  acento: string
  icono: ReactNode
  orden: number
}) {
  return (
    <Paper
      sx={{
        px: 2.5,
        py: 2.25,
        position: 'relative',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 1.5,
        animation: `${fadeInUp} .4s ease`,
        animationDelay: `${orden * 0.06}s`,
        animationFillMode: 'backwards',
        '&::before': { content: '""', position: 'absolute', left: 0, top: 0, bottom: 0, width: 4, bgcolor: acento },
      }}
    >
      <Box>
        <Typography component="p" sx={{ fontSize: 28, fontWeight: 800, lineHeight: 1.15 }} aria-label={titulo}>
          {valor}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ fontSize: 13 }}>
          {titulo}
        </Typography>
        {ayuda && (
          <Typography variant="caption" color="text.secondary">
            {ayuda}
          </Typography>
        )}
      </Box>
      <Box
        aria-hidden
        sx={{
          width: 44,
          height: 44,
          flexShrink: 0,
          borderRadius: 2.5,
          display: 'grid',
          placeItems: 'center',
          color: acento,
          bgcolor: `${acento}14`,
        }}
      >
        {icono}
      </Box>
    </Paper>
  )
}
