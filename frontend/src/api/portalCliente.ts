import type { DespachoPortal, FiltrosPortal, MiEmpresa } from '../types'
import { http } from './http'

// HU-16 · contrato C11 (docs/sprint-3/contratos/C11-portal-cliente.md), solo rol CLIENTE (ADMIN y AGENTE: 403).
// El backend filtra por la empresa de la sesión. El detalle, la línea de tiempo y los documentos usan las
// rutas de siempre (/despachos/{id}, /hitos, /documentos) y la cabecera sale de /portal/despachos/{id}: un
// despacho ajeno responde 404.

/** GET /portal/mi-empresa: { clienteId, ruc, razonSocial }, todo nulo si el usuario aún no tiene empresa. */
export async function obtenerMiEmpresa(): Promise<MiEmpresa> {
  const { data } = await http.get<MiEmpresa>('/portal/mi-empresa')
  return data
}

/**
 * GET /portal/despachos con los filtros que tengan valor. `numero` es «contiene» sin distinguir mayúsculas;
 * las fechas van como AAAA-MM-DD. 400 con `campos.estado`, `campos.etaDesde` o `campos.etaHasta`.
 */
export async function listarMisDespachos(filtros: FiltrosPortal = {}): Promise<DespachoPortal[]> {
  const params = Object.fromEntries(Object.entries(filtros).filter(([, v]) => typeof v === 'string' && v.trim()))
  const { data } = await http.get<DespachoPortal[]>('/portal/despachos', { params })
  return data
}

/**
 * GET /portal/despachos/{id}: la misma fila de la lista para un solo despacho (BL, ETA y última
 * actualización de la cabecera del detalle). 404 «Despacho no encontrado» si no es de su empresa.
 */
export async function obtenerMiDespacho(id: number): Promise<DespachoPortal> {
  const { data } = await http.get<DespachoPortal>(`/portal/despachos/${id}`)
  return data
}
