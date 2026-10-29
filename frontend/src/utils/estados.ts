import type { EstadoDespacho } from '../types'

/** Orden en que avanza el despacho (Sprints 1 y 2); cada servicio exige el estado anterior. */
const ORDEN: EstadoDespacho[] = [
  'BORRADOR',
  'APERTURA',
  'DOCUMENTACION_COMPLETA',
  'CLASIFICADO',
  'LIQUIDADO',
  'TRIBUTOS_PAGADOS',
  'DAM_NUMERADA',
  'CANAL_ASIGNADO',
]

/**
 * true si el despacho ya está en `minimo` o en un estado posterior: decide qué pestañas se ven
 * (p. ej. «Clasificación» desde DOCUMENTACION_COMPLETA). Un estado que no está en la lista
 * llega en el Sprint 3 (LEVANTE…) y cuenta como posterior.
 */
export function alcanzoEstado(actual: EstadoDespacho, minimo: EstadoDespacho): boolean {
  const posicion = ORDEN.indexOf(actual)
  return posicion === -1 || posicion >= ORDEN.indexOf(minimo)
}
