import type { UsuarioSesion } from '../types'
import { http } from './http'

export async function login(usuario: string, password: string, recordarme: boolean): Promise<UsuarioSesion> {
  const { data } = await http.post<UsuarioSesion>('/auth/login', { usuario, password, recordarme })
  return data
}

export async function logout(): Promise<void> {
  await http.post('/auth/logout')
}

export async function sesionActual(): Promise<UsuarioSesion> {
  const { data } = await http.get<UsuarioSesion>('/auth/me')
  return data
}
