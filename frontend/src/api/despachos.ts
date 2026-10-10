import type { Despacho, DespachoRequest } from '../types'
import { http } from './http'

export async function crearDespacho(despacho: DespachoRequest): Promise<Despacho> {
  const { data } = await http.post<Despacho>('/despachos', despacho)
  return data
}

export async function actualizarDespacho(id: number, despacho: DespachoRequest): Promise<Despacho> {
  const { data } = await http.put<Despacho>(`/despachos/${id}`, despacho)
  return data
}

export async function obtenerDespacho(id: number): Promise<Despacho> {
  const { data } = await http.get<Despacho>(`/despachos/${id}`)
  return data
}
