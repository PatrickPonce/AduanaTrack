import type { Dam, DamRectificacionRequest, DamRequest, RectificacionDam } from '../types'
import { http } from './http'

// HU-09 · contrato C4 (docs/sprint-2/contratos/C4-numeracion-dam.md).

/** Siempre 200: numeroDam, fechaNumeracion y nombreCargo vienen nulos mientras no se registra la DAM. */
export async function obtenerDam(despachoId: number): Promise<Dam> {
  const { data } = await http.get<Dam>(`/despachos/${despachoId}/dam`)
  return data
}

function formulario(solicitud: DamRequest): FormData {
  const datos = new FormData()
  datos.append('numeroDam', solicitud.numeroDam)
  if (solicitud.fechaNumeracion) datos.append('fechaNumeracion', solicitud.fechaNumeracion)
  if (solicitud.cargo) datos.append('cargo', solicitud.cargo)
  return datos
}

/**
 * Guarda el N° de DAM con su fecha (y el cargo en PDF, opcional) y pasa el despacho a DAM_NUMERADA.
 * 409 TRIBUTOS_PENDIENTES si no se pagaron los tributos o la DAM ya está registrada; 409 DAM_DUPLICADA
 * si el número ya está en otro despacho.
 */
export async function registrarDam(despachoId: number, solicitud: DamRequest): Promise<Dam> {
  const { data } = await http.post<Dam>(`/despachos/${despachoId}/dam`, formulario(solicitud))
  return data
}

/** Corrige el número o la fecha con un motivo; el estado no cambia y queda en el historial. 409 DAM_NO_REGISTRADA sin DAM. */
export async function rectificarDam(despachoId: number, solicitud: DamRectificacionRequest): Promise<Dam> {
  const datos = formulario(solicitud)
  datos.append('motivo', solicitud.motivo)
  const { data } = await http.post<Dam>(`/despachos/${despachoId}/dam/rectificacion`, datos)
  return data
}

export async function listarRectificacionesDam(despachoId: number): Promise<RectificacionDam[]> {
  const { data } = await http.get<RectificacionDam[]>(`/despachos/${despachoId}/dam/historial`)
  return data
}

/** Enlace de descarga del cargo; la cookie de sesión viaja sola por ser del mismo origen. */
export function urlCargoDam(despachoId: number): string {
  return `${http.defaults.baseURL}/despachos/${despachoId}/dam/cargo`
}
