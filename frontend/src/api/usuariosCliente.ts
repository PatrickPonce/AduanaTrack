import type { AccesoDenegado, UsuarioCliente } from '../types'
import { http } from './http'

// HU-16 · contrato C11 (docs/sprint-3/contratos/C11-portal-cliente.md), solo ADMIN.
// ⚠️ RUTAS SUPUESTAS mientras el contrato no esté publicado: revisarlas al integrar.

/** GET /admin/usuarios-cliente: usuarios con rol CLIENTE y su empresa (nula = sin asignar). */
export async function listarUsuariosCliente(): Promise<UsuarioCliente[]> {
  const { data } = await http.get<UsuarioCliente[]>('/admin/usuarios-cliente')
  return data
}

/** PUT /admin/usuarios-cliente/{id}/empresa { clienteId }: asigna o cambia la empresa del usuario. */
export async function asignarEmpresa(usuarioId: number, clienteId: number): Promise<UsuarioCliente> {
  const { data } = await http.put<UsuarioCliente>(`/admin/usuarios-cliente/${usuarioId}/empresa`, { clienteId })
  return data
}

/** GET /admin/accesos-denegados: intentos de usuarios CLIENTE de ver datos de otra empresa. */
export async function listarAccesosDenegados(): Promise<AccesoDenegado[]> {
  const { data } = await http.get<AccesoDenegado[]>('/admin/accesos-denegados')
  return data
}
