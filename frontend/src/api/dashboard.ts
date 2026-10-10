import type { DespachoTablero, FiltrosTablero, Indicadores } from '../types'
import { http } from './http'

export async function obtenerIndicadores(filtros: FiltrosTablero): Promise<Indicadores> {
  const { data } = await http.get<Indicadores>('/dashboard/indicadores', { params: filtros })
  return data
}

export async function listarDespachosTablero(filtros: FiltrosTablero): Promise<DespachoTablero[]> {
  const { data } = await http.get<DespachoTablero[]>('/dashboard/despachos', { params: filtros })
  return data
}

/** Descarga el Excel con los filtros activos y lo guarda con el nombre que envía el servidor. */
export async function exportarExcel(filtros: FiltrosTablero): Promise<void> {
  const respuesta = await http.get<Blob>('/dashboard/despachos/excel', { params: filtros, responseType: 'blob' })
  const disposicion = String(respuesta.headers['content-disposition'] ?? '')
  const nombre = /filename="?([^";]+)"?/.exec(disposicion)?.[1] ?? 'despachos.xlsx'
  const url = URL.createObjectURL(respuesta.data)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}
