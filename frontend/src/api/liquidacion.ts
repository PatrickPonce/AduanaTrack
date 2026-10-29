import type { Liquidacion, LiquidacionRequest, PagoRegistrado, PagoRequest } from '../types'
import { http } from './http'

// HU-08 · contrato C3 (docs/sprint-2/contratos/C3-liquidacion-pago.md).

/**
 * Calcula CIF, Ad Valorem, IGV, IPM y percepción con las tasas configuradas y pasa el despacho a
 * LIQUIDADO. Volver a generarla reemplaza la anterior mientras no haya pago.
 * 409 DESPACHO_NO_CLASIFICADO si el despacho todavía no está clasificado.
 */
export async function generarLiquidacion(despachoId: number, solicitud: LiquidacionRequest): Promise<Liquidacion> {
  const { data } = await http.post<Liquidacion>(`/despachos/${despachoId}/liquidacion`, solicitud)
  return data
}

/** Pago hecho fuera del sistema; pasa el despacho a TRIBUTOS_PAGADOS. 409 LIQUIDACION_PENDIENTE sin liquidación. */
export async function registrarPago(despachoId: number, solicitud: PagoRequest): Promise<PagoRegistrado> {
  const { data } = await http.post<PagoRegistrado>(`/despachos/${despachoId}/pago`, solicitud)
  return data
}
