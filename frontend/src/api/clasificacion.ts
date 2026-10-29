import type { ClasificacionGuardada, ItemFactura, ItemFacturaRequest, PartidaArancelaria } from '../types'
import { http } from './http'

// HU-07 · contrato C2: buscador de partidas (Fabio, T-07.1), ítems (Patrick, T-07.2,
// docs/sprint-2/contratos/C2-items-factura.md) y guardar clasificación (Fabio, T-07.3).

/** Búsqueda por código o palabras, sin distinguir mayúsculas ni tildes; máximo 20 resultados. */
export async function buscarPartidas(q: string): Promise<PartidaArancelaria[]> {
  const { data } = await http.get<PartidaArancelaria[]>('/catalogos/partidas', { params: { q } })
  return data
}

/** Ítems ordenados por número; lista vacía si todavía no se registraron. */
export async function listarItems(despachoId: number): Promise<ItemFactura[]> {
  const { data } = await http.get<ItemFactura[]>(`/despachos/${despachoId}/items`)
  return data
}

/** Se envía siempre la tabla completa, en el orden en que se muestra. */
export async function guardarItems(despachoId: number, items: ItemFacturaRequest[]): Promise<ItemFactura[]> {
  const { data } = await http.put<ItemFactura[]>(`/despachos/${despachoId}/items`, items)
  return data
}

/** 409 DOCUMENTACION_INCOMPLETA o ITEMS_SIN_PARTIDA si falta algo; si no, el despacho pasa a CLASIFICADO. */
export async function guardarClasificacion(despachoId: number): Promise<ClasificacionGuardada> {
  const { data } = await http.post<ClasificacionGuardada>(`/despachos/${despachoId}/clasificacion`)
  return data
}
