import type { Despacho, DespachoRequest, Embarque, EmbarqueRequest } from '../types'
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

/** Devuelve null si el despacho aún no tiene embarque (404). */
export async function obtenerEmbarque(despachoId: number): Promise<Embarque | null> {
  try {
    const { data } = await http.get<Embarque>(`/despachos/${despachoId}/embarque`)
    return data
  } catch (error) {
    if ((error as { response?: { status?: number } }).response?.status === 404) return null
    throw error
  }
}

export async function guardarEmbarque(despachoId: number, embarque: EmbarqueRequest): Promise<Embarque> {
  const cuerpo = {
    ...embarque,
    contenedores: embarque.contenedores.map((c) => ({
      ...c,
      pesoBrutoKg: c.pesoBrutoKg.trim() ? Number(c.pesoBrutoKg.replace(',', '.')) : null,
    })),
  }
  const { data } = await http.put<Embarque>(`/despachos/${despachoId}/embarque`, cuerpo)
  return data
}
