import type { NotificacionBandeja, Pagina } from '../types'
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
