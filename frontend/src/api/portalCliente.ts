import type { DespachoPortal, DetalleDespachoPortal } from '../types'
import { http } from './http'

// HU-16 · contrato C11 (docs/sprint-3/contratos/C11-portal-cliente.md), lo publica Patrick.
// ⚠️ RUTAS SUPUESTAS mientras el contrato no esté publicado: revisarlas al integrar.
// El backend decide qué despachos ve el usuario según la empresa de su sesión; la pantalla solo
// muestra lo que recibe. Un despacho de otra empresa responde 404 (no se revela que existe).

/** GET /portal-cliente/despachos: todos los despachos de la empresa del usuario CLIENTE. */
export async function listarMisDespachos(): Promise<DespachoPortal[]> {
  const { data } = await http.get<DespachoPortal[]>('/portal-cliente/despachos')
  return data
}

/** GET /portal-cliente/despachos/{id}: detalle con hitos y documentos; 404 si no es de su empresa. */
export async function obtenerMiDespacho(id: number): Promise<DetalleDespachoPortal> {
  const { data } = await http.get<DetalleDespachoPortal>(`/portal-cliente/despachos/${id}`)
  return data
}

/** GET /portal-cliente/despachos/{id}/documentos/{documentoId}/archivo: descarga con la cookie de sesión. */
export function urlDocumentoPortal(despachoId: number, documentoId: number): string {
  return `${http.defaults.baseURL}/portal-cliente/despachos/${despachoId}/documentos/${documentoId}/archivo`
}
