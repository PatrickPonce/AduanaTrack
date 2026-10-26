import type { DocumentoCargado, DocumentosDespacho, VersionDocumento } from '../types'
import { http } from './http'

// HU-06 · contrato C1 (docs/sprint-2/contratos/C1-documentos-despacho.md).

export async function listarDocumentos(despachoId: number): Promise<DocumentosDespacho> {
  const { data } = await http.get<DocumentosDespacho>(`/despachos/${despachoId}/documentos`)
  return data
}

/** Primera carga o reemplazo de la versión vigente; la anterior se conserva en el historial (CA3). */
export async function cargarDocumento(despachoId: number, tipo: string, archivo: File): Promise<DocumentoCargado> {
  const formulario = new FormData()
  formulario.append('archivo', archivo)
  const { data } = await http.post<DocumentoCargado>(`/despachos/${despachoId}/documentos/${tipo}`, formulario)
  return data
}

/** Versiones del tipo, de la más reciente a la más antigua. */
export async function listarVersiones(despachoId: number, tipo: string): Promise<VersionDocumento[]> {
  const { data } = await http.get<VersionDocumento[]>(`/despachos/${despachoId}/documentos/${tipo}/versiones`)
  return data
}

/** Enlace de descarga de cualquier versión; la cookie de sesión viaja sola por ser del mismo origen. */
export function urlArchivoDocumento(despachoId: number, documentoId: number): string {
  return `${http.defaults.baseURL}/despachos/${despachoId}/documentos/${documentoId}/archivo`
}
