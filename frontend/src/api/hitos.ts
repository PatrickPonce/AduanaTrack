import type { Hito } from '../types'
import { http } from './http'

// HU-10 · T-10.1 (Patrick): línea de tiempo del despacho. Cada hito trae fecha, hora y usuario responsable.

/** Hitos en orden cronológico; el usuario es «Sistema» cuando lo registró el propio sistema. */
export async function listarHitos(despachoId: number): Promise<Hito[]> {
  const { data } = await http.get<Hito[]>(`/despachos/${despachoId}/hitos`)
  return data
}
