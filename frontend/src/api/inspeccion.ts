import type { EstadoDespacho } from '../types'
import { http, leerError } from './http'

// HU-12 · contrato C7 (docs/sprint-3/contratos/C7-inspeccion.md). Implementa Fabio (T-12.1 y T-12.2);
// esta pantalla (T-12.3) consume el contrato.

export type ResultadoInspeccion = 'CONFORME' | 'CON_OBSERVACIONES'

/** Un ítem del checklist: la observación va en el ítem que no es conforme. */
export interface ItemInspeccion {
  descripcion: string
  conforme: boolean
  observacion: string | null
}

export interface InspeccionSolicitud {
  items: ItemInspeccion[]
  observaciones: string | null
  /** PDF de hasta 10 MB, opcional. */
  acta: File | null
}

/**
 * Respuesta del POST y del GET. El contrato garantiza `resultado`, `estadoDespacho`, `levanteHabilitado` y
 * `actaNombre`; el resto se muestra solo si el API lo devuelve.
 */
export interface InspeccionRegistrada {
  resultado: ResultadoInspeccion
  estadoDespacho: EstadoDespacho
  levanteHabilitado: boolean
  actaNombre: string | null
  observaciones?: string | null
  items?: ItemInspeccion[]
  registradaPor?: string
  registradaEn?: string
}

/** Ítems que el agente verifica, en el orden del contrato C7 (catálogo fijo que confirma el PO). */
export async function listarItemsInspeccion(): Promise<string[]> {
  const { data } = await http.get<string[]>('/catalogos/items-inspeccion')
  return data
}

/** Última inspección registrada, o `null` si el despacho todavía no tiene ninguna (404 o cuerpo vacío). */
export async function obtenerUltimaInspeccion(despachoId: number): Promise<InspeccionRegistrada | null> {
  try {
    const { data } = await http.get<InspeccionRegistrada | null>(`/despachos/${despachoId}/inspeccion`)
    return data && data.resultado ? data : null
  } catch (error) {
    if (leerError(error).status === 404) return null
    throw error
  }
}

/**
 * Registra el resultado: el sistema calcula si es CONFORME o CON_OBSERVACIONES. Viaja como multipart con
 * `datos` (JSON) y `acta` (PDF, opcional). 409 si el despacho no está en INSPECCION_PROGRAMADA; 413 y 415
 * para un acta de más de 10 MB o que no es PDF.
 */
export async function registrarInspeccion(
  despachoId: number,
  solicitud: InspeccionSolicitud,
): Promise<InspeccionRegistrada> {
  const datos = new FormData()
  const json = JSON.stringify({ items: solicitud.items, observaciones: solicitud.observaciones })
  datos.append('datos', new Blob([json], { type: 'application/json' }))
  if (solicitud.acta) datos.append('acta', solicitud.acta)
  const { data } = await http.post<InspeccionRegistrada>(`/despachos/${despachoId}/inspeccion`, datos)
  return data
}

/** Enlace de descarga del acta; la cookie de sesión viaja sola por ser del mismo origen. */
export function urlActaInspeccion(despachoId: number): string {
  return `${http.defaults.baseURL}/despachos/${despachoId}/inspeccion/acta`
}
