import type { CanalNotificacion, NotificacionBandeja, Pagina } from '../types'
import { http } from './http'

/** Bandeja del usuario en sesión (contrato C9). */
export async function listarNotificaciones(estado: 'TODAS' | 'NO_LEIDAS', pagina = 0, tamano = 20) {
  const { data } = await http.get<Pagina<NotificacionBandeja>>('/notificaciones', {
    params: { estado: estado === 'NO_LEIDAS' ? 'NO_LEIDAS' : undefined, pagina, tamano },
  })
  return data
}

export async function cantidadNoLeidas(): Promise<number> {
  const { data } = await http.get<{ cantidad: number }>('/notificaciones/no-leidas/cantidad')
  return data.cantidad
}

export async function marcarLeida(id: number): Promise<NotificacionBandeja> {
  const { data } = await http.patch<NotificacionBandeja>(`/notificaciones/${id}/leida`)
  return data
}

/** Evento que avisa a la campana de que cambió la cantidad de no leídas (p. ej. al marcar una como leída). */
export const EVENTO_NOTIFICACIONES = 'aduanatrack:notificaciones'

export function avisarCambioNotificaciones(): void {
  window.dispatchEvent(new Event(EVENTO_NOTIFICACIONES))
}

/** Canal por el que el usuario en sesión recibe sus avisos (contrato C9). */
export async function obtenerPreferencia(): Promise<CanalNotificacion> {
  const { data } = await http.get<{ canalNotificacion: CanalNotificacion }>('/usuarios/me/preferencias')
  return data.canalNotificacion
}

export async function guardarPreferencia(canal: CanalNotificacion): Promise<CanalNotificacion> {
  const { data } = await http.put<{ canalNotificacion: CanalNotificacion }>('/usuarios/me/preferencias', {
    canalNotificacion: canal,
  })
  return data.canalNotificacion
}
