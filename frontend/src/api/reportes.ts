import axios from 'axios'

import { http } from './http'

// HU-15 · contrato C10 (Guía de programación del Sprint 3, sección 4). Implementan Fabio (T-15.1 indicadores
// y T-15.3 envío programado) y Patrick (T-15.2 exportación); esta página (T-15.4) consume el contrato.

/** Rango de fechas del reporte, ambas en AAAA-MM-DD e incluidas. */
export interface RangoFechas {
  desde: string
  hasta: string
}

export interface DespachosPorMes {
  /** AAAA-MM */
  mes: string
  cantidad: number
}

/**
 * Indicadores del rango. `tiempoPromedioDias` cuenta de la apertura al levante, solo de los despachos con
 * levante (CA1), y es nulo si ninguno lo tiene. `porCanal` trae VERDE, NARANJA y ROJO (CA2).
 */
export interface IndicadoresGestion {
  totalDespachos: number
  conLevante: number
  tiempoPromedioDias: number | null
  despachosPorMes: DespachosPorMes[]
  porCanal: Record<string, number>
}

export type FormatoReporte = 'xlsx' | 'pdf'

export type Periodicidad = 'SEMANAL' | 'MENSUAL'

export interface EnvioProgramadoRequest {
  periodicidad: Periodicidad
  destinatarios: string[]
}

/** Envío programado del reporte (CA4); los datos del último envío llegan si el API los devuelve. */
export interface EnvioProgramado extends EnvioProgramadoRequest {
  id: number
  activo?: boolean
  ultimoEnvio?: string | null
}

/** Respuesta tal como puede llegar del servidor, antes de normalizarla. */
interface IndicadoresCrudos {
  totalDespachos?: number | null
  conLevante?: number | null
  tiempoPromedioDias?: number | string | null
  /** Lista del C10, u objeto { "AAAA-MM": n } si el backend serializa un Map<YearMonth, Long>. */
  despachosPorMes?: DespachosPorMes[] | Record<string, number> | null
  porCanal?: Record<string, number> | null
}

/** Meses AAAA-MM de `desde` a `hasta`, ambos incluidos. */
function mesesDelRango({ desde, hasta }: RangoFechas): string[] {
  const meses: string[] = []
  let [anio, mes] = desde.slice(0, 7).split('-').map(Number) as [number, number]
  const fin = hasta.slice(0, 7)
  // Tope de 24 por seguridad: el rango válido es de 12 meses como máximo.
  for (let i = 0; i < 24; i++) {
    const actual = `${anio}-${String(mes).padStart(2, '0')}`
    if (actual > fin) break
    meses.push(actual)
    mes++
    if (mes > 12) {
      mes = 1
      anio++
    }
  }
  return meses
}

/**
 * Deja los indicadores como los espera la página: `despachosPorMes` como lista ordenada con todos los meses
 * del rango (0 en los meses sin despachos, que el backend puede omitir) y los números siempre numéricos.
 */
export function normalizarIndicadores(crudos: IndicadoresCrudos, rango: RangoFechas): IndicadoresGestion {
  const porMes = new Map<string, number>()
  const fuente = crudos.despachosPorMes
  if (Array.isArray(fuente)) {
    for (const m of fuente) porMes.set(String(m.mes).slice(0, 7), Number(m.cantidad) || 0)
  } else if (fuente && typeof fuente === 'object') {
    for (const [mes, cantidad] of Object.entries(fuente)) porMes.set(mes.slice(0, 7), Number(cantidad) || 0)
  }
  const meses = [...new Set([...mesesDelRango(rango), ...porMes.keys()])].sort()
  const promedio = crudos.tiempoPromedioDias
  return {
    totalDespachos: Number(crudos.totalDespachos) || 0,
    conLevante: Number(crudos.conLevante) || 0,
    tiempoPromedioDias: promedio === null || promedio === undefined || promedio === '' ? null : Number(promedio),
    despachosPorMes: meses.map((mes) => ({ mes, cantidad: porMes.get(mes) ?? 0 })),
    porCanal: crudos.porCanal ?? {},
  }
}

/** 400 con `campos.desde` o `campos.hasta` si falta una fecha, «desde» es mayor que «hasta» o el rango pasa de 12 meses. */
export async function obtenerIndicadoresGestion(rango: RangoFechas): Promise<IndicadoresGestion> {
  const { data } = await http.get<IndicadoresCrudos>('/reportes/indicadores', { params: rango })
  return normalizarIndicadores(data, rango)
}

/**
 * Descarga el reporte del rango en Excel o PDF (CA3) con el nombre que envía el servidor
 * (reporte-gestion_AAAA-MM-DD). Como la respuesta es un Blob, el cuerpo JSON de un error se vuelve a leer
 * para que la pantalla vea el código y los campos marcados.
 */
export async function exportarReporte(formato: FormatoReporte, rango: RangoFechas): Promise<void> {
  try {
    const respuesta = await http.get<Blob>('/reportes/exportar', {
      params: { formato, ...rango },
      responseType: 'blob',
    })
    const disposicion = String(respuesta.headers['content-disposition'] ?? '')
    const nombre = /filename="?([^";]+)"?/.exec(disposicion)?.[1] ?? `reporte-gestion.${formato}`
    const url = URL.createObjectURL(respuesta.data)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = nombre
    document.body.appendChild(enlace)
    enlace.click()
    enlace.remove()
    URL.revokeObjectURL(url)
  } catch (error) {
    const respuesta = axios.isAxiosError(error) ? error.response : undefined
    if (respuesta?.data instanceof Blob && respuesta.data.type.includes('json')) {
      try {
        respuesta.data = JSON.parse(await respuesta.data.text())
      } catch {
        // Si el cuerpo no es JSON válido, la pantalla muestra el mensaje genérico.
      }
    }
    throw error
  }
}

export async function listarEnviosProgramados(): Promise<EnvioProgramado[]> {
  const { data } = await http.get<EnvioProgramado[]>('/reportes/programados')
  return data
}

export async function programarEnvio(envio: EnvioProgramadoRequest): Promise<EnvioProgramado> {
  const { data } = await http.post<EnvioProgramado>('/reportes/programados', envio)
  return data
}

export async function eliminarEnvioProgramado(id: number): Promise<void> {
  await http.delete(`/reportes/programados/${id}`)
}
