import type { EstadoDespacho } from '../types'
import { http } from './http'

// HU-11 · contrato C6 (docs/sprint-3/contratos/C6-citas-deposito.md). Implementa Patrick (T-11.1 y T-11.2);
// esta pantalla (T-11.3) consume el contrato.

export type TipoDeposito = 'DEPOSITO_TEMPORAL' | 'ALMACEN'

export interface Deposito {
  id: number
  codigo: string
  nombre: string
  tipo: TipoDeposito
}

/** INSPECCION en canal naranja o rojo (CA2); RETIRO solo después del levante (CA3). */
export type TipoCita = 'INSPECCION' | 'RETIRO'

export type EstadoCita = 'PROGRAMADA' | 'REALIZADA' | 'CANCELADA'

/** Quien retira la mercancía: obligatorio solo en las citas de retiro. */
export interface Transportista {
  ruc: string
  razonSocial: string
  placa: string
  conductor: string
}

export interface CitaRequest {
  depositoId: number
  numeroCita: string
  tipo: TipoCita
  /** AAAA-MM-DDTHH:mm:ss, hora local del depósito. */
  fechaHoraConfirmada: string
  transportista: Transportista | null
}

/** Respuesta del POST y fila del GET; `estadoDespacho` es el estado del despacho al responder. */
export interface Cita {
  id: number
  depositoId: number
  deposito: string
  numeroCita: string
  tipo: TipoCita
  fechaHoraConfirmada: string
  estado: EstadoCita
  transportista: Transportista | null
  registradaPor: string
  registradaEn: string
  estadoDespacho: EstadoDespacho
}

/** Depósitos temporales y almacenes activos, por código. */
export async function listarDepositos(): Promise<Deposito[]> {
  const { data } = await http.get<Deposito[]>('/catalogos/depositos')
  return data
}

/** Citas del despacho ordenadas por fecha y hora confirmada. */
export async function listarCitas(despachoId: number): Promise<Cita[]> {
  const { data } = await http.get<Cita[]>(`/despachos/${despachoId}/citas`)
  return data
}

/**
 * Registra la cita y, según el tipo, pasa el despacho a INSPECCION_PROGRAMADA o RETIRO_PROGRAMADO. 400 con
 * `campos` si falta un dato (CA4); 409 CITA_INSPECCION_NO_APLICA o CITA_RETIRO_SIN_LEVANTE si el despacho
 * no está en el estado que el tipo exige.
 */
export async function registrarCita(despachoId: number, cita: CitaRequest): Promise<Cita> {
  const { data } = await http.post<Cita>(`/despachos/${despachoId}/citas`, cita)
  return data
}
