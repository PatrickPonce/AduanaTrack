import type { EstadoDespacho } from '../types'

/** Orden en que avanza el despacho (Sprints 1 a 3); cada servicio exige el estado anterior. */
const ORDEN: EstadoDespacho[] = [
  'BORRADOR',
  'APERTURA',
  'DOCUMENTACION_COMPLETA',
  'CLASIFICADO',
  'LIQUIDADO',
  'TRIBUTOS_PAGADOS',
  'DAM_NUMERADA',
  'CANAL_ASIGNADO',
  'INSPECCION_PROGRAMADA',
  'INSPECCION_CONFORME',
  'LEVANTE_OTORGADO',
  'RETIRO_PROGRAMADO',
]

/** Estados que ve quien consulta despachos ya abiertos (sin BORRADOR), en su orden: filtros por estado. */
export const ESTADOS_VISIBLES: readonly EstadoDespacho[] = ORDEN.filter((e) => e !== 'BORRADOR')

/**
 * true si el despacho ya está en `minimo` o en un estado posterior: decide qué pestañas se ven
 * (p. ej. «Clasificación» desde DOCUMENTACION_COMPLETA). Un estado que no está en la lista
 * (uno que el backend agregue después) cuenta como posterior.
 */
export function alcanzoEstado(actual: EstadoDespacho, minimo: EstadoDespacho): boolean {
  const posicion = ORDEN.indexOf(actual)
  return posicion === -1 || posicion >= ORDEN.indexOf(minimo)
}
