import axios, { AxiosError } from 'axios'

import type { ApiErrorBody } from '../types'

/**
 * Cliente HTTP único. La sesión vive en la cookie HttpOnly AT_SESSION (nunca en
 * LocalStorage); Axios reenvía la cookie XSRF-TOKEN en la cabecera X-XSRF-TOKEN.
 */
export const http = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

/** Rutas cuyo 401 se maneja en la propia pantalla (login y comprobación de sesión). */
const SIN_REDIRECCION = ['/auth/login', '/auth/me']

http.interceptors.response.use(
  (respuesta) => respuesta,
  (error: AxiosError) => {
    const url = error.config?.url ?? ''
    if (
      error.response?.status === 401 &&
      !SIN_REDIRECCION.some((ruta) => url.endsWith(ruta)) &&
      window.location.pathname !== '/login'
    ) {
      window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)

export interface ErrorApi {
  status: number | null
  body: ApiErrorBody | null
}

/** Extrae el estado HTTP y el cuerpo {codigo, mensaje, campos} de un error de Axios. */
export function leerError(error: unknown): ErrorApi {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data
    const body = data && typeof data === 'object' && 'codigo' in data ? (data as ApiErrorBody) : null
    return { status: error.response?.status ?? null, body }
  }
  return { status: null, body: null }
}

export function mensajeDeError(error: unknown, porDefecto = 'No se pudo completar la operación.'): string {
  const { status, body } = leerError(error)
  if (body?.mensaje) return body.mensaje
  if (status === null) return 'No se pudo conectar con el servidor.'
  return porDefecto
}
