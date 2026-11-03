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

/** 400 con `campos.desde` o `campos.hasta` si falta una fecha, «desde» es mayor que «hasta» o el rango pasa de 12 meses. */
export async function obtenerIndicadoresGestion(rango: RangoFechas): Promise<IndicadoresGestion> {
  const { data } = await http.get<IndicadoresGestion>('/reportes/indicadores', { params: rango })
  return data
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
