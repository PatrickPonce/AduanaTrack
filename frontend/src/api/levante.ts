import type { EstadoDespacho } from '../types'
import { http } from './http'

// HU-13 · contrato C8 (docs/sprint-3/contratos/C8-levante.md). Implementa Patrick (T-13.1 y T-13.2);
// esta sección (T-13.3) consume el contrato.

/**
 * Levante del despacho. `numeroLevante`, `fechaLevante` y `constanciaNombre` vienen nulos mientras no se
 * registra; `retiroHabilitado` es true desde LEVANTE_OTORGADO (ya se puede registrar la cita de retiro).
 */
export interface Levante {
  numeroLevante: string | null
  fechaLevante: string | null
  constanciaNombre: string | null
  conAdvertencia: boolean
  retiroHabilitado: boolean
  estadoDespacho: EstadoDespacho
}

export interface LevanteSolicitud {
  numeroLevante: string
  /** AAAA-MM-DD; no puede ser futura. */
  fechaLevante: string
  /** true cuando el agente acepta registrar el levante pese a los requisitos pendientes (CA2). */
  confirmarSinRequisitos: boolean
  /** PDF de hasta 10 MB, opcional. */
  constancia: File | null
}

/** Siempre 200: los datos del levante vienen nulos mientras el despacho no lo tiene. */
export async function obtenerLevante(despachoId: number): Promise<Levante> {
  const { data } = await http.get<Levante>(`/despachos/${despachoId}/levante`)
  return data
}

/**
 * Registra el levante y pasa el despacho a LEVANTE_OTORGADO (CA1). Viaja como multipart con `datos` (JSON)
 * y `constancia` (PDF, opcional). 409 LEVANTE_REQUISITOS_PENDIENTES con `pendientes` si falta el pago o la
 * inspección y no llega la confirmación (CA2); 409 LEVANTE_DUPLICADO si el número ya está en otro despacho;
 * 413 y 415 para una constancia de más de 10 MB o que no es PDF.
 */
export async function registrarLevante(despachoId: number, solicitud: LevanteSolicitud): Promise<Levante> {
  const datos = new FormData()
  const json = JSON.stringify({
    numeroLevante: solicitud.numeroLevante,
    fechaLevante: solicitud.fechaLevante,
    confirmarSinRequisitos: solicitud.confirmarSinRequisitos,
  })
  datos.append('datos', new Blob([json], { type: 'application/json' }))
  if (solicitud.constancia) datos.append('constancia', solicitud.constancia)
  const { data } = await http.post<Levante>(`/despachos/${despachoId}/levante`, datos)
  return data
}

/** Enlace de descarga de la constancia (CA3); la cookie de sesión viaja sola por ser del mismo origen. */
export function urlConstanciaLevante(despachoId: number): string {
  return `${http.defaults.baseURL}/despachos/${despachoId}/levante/constancia`
}
