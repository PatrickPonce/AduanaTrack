import type { CanalControl, CanalGuardado } from '../types'
import { http } from './http'

// HU-10 · contrato C5 (docs/sprint-2/contratos/C5-canal-control.md).

/**
 * Guarda el canal que SUNAT informó y pasa el despacho a CANAL_ASIGNADO; en canal rojo el sistema
 * agrega un hito sugerido y en verde habilita el levante. Guardar el mismo canal no repite el hito.
 * 400 VALIDACION con campos.canal; 409 DAM_NO_NUMERADA si el despacho todavía no tiene DAM.
 */
export async function guardarCanal(despachoId: number, canal: CanalControl): Promise<CanalGuardado> {
  const { data } = await http.put<CanalGuardado>(`/despachos/${despachoId}/canal`, { canal })
  return data
}
