import type { Rol } from '../types'

/**
 * Qué rutas ve cada rol (HU-16). Ocultar menús y redirigir NO es seguridad: el backend filtra los
 * datos por rol y por empresa. Esto solo evita que el usuario llegue a pantallas que no le sirven.
 */

/** Quienes trabajan el despacho: ven el menú del agente. */
export const ROLES_AGENCIA: Rol[] = ['ADMIN', 'AGENTE']

export const INICIO_CLIENTE = '/portal-cliente/mis-despachos'

/** Pantalla de inicio de cada rol: tras el login y al abrir una ruta que no le corresponde. */
export function inicioDe(rol: Rol): string {
  return rol === 'CLIENTE' ? INICIO_CLIENTE : '/dashboard'
}

/** Prefijos que el CLIENTE puede abrir; todo lo demás es pantalla de agente. */
const RUTAS_CLIENTE = ['/portal-cliente', '/notificaciones']

/** true si `ruta` es una pantalla de ese rol (sirve para respetar el «volver a» tras el login). */
export function rutaPermitida(rol: Rol, ruta: string): boolean {
  const esDeCliente = RUTAS_CLIENTE.some((p) => ruta === p || ruta.startsWith(`${p}/`) || ruta.startsWith(`${p}?`))
  if (rol === 'CLIENTE') return esDeCliente
  if (ruta.startsWith('/portal-cliente')) return false
  if (ruta.startsWith('/administracion')) return rol === 'ADMIN'
  return true
}
