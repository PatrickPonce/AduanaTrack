import type { AccesoDenegado, EmpresaAsignable, Pagina, UsuarioCliente } from '../types'
import { http } from './http'

// HU-16 · contrato C11 (docs/sprint-3/contratos/C11-portal-cliente.md), solo rol ADMIN (AGENTE y CLIENTE: 403).

/** GET /administracion/usuarios-cliente: lista plana ordenada por usuario. */
export async function listarUsuariosCliente(): Promise<UsuarioCliente[]> {
  const { data } = await http.get<UsuarioCliente[]>('/administracion/usuarios-cliente')
  return data
}

/** GET /administracion/empresas-asignables: empresas activas con RUC válido, por razón social. */
export async function listarEmpresasAsignables(): Promise<EmpresaAsignable[]> {
  const { data } = await http.get<EmpresaAsignable[]>('/administracion/empresas-asignables')
  return data
}

/**
 * PUT /administracion/usuarios-cliente/{usuarioId}/empresa { clienteId }: asigna o cambia la empresa y devuelve
 * la fila actualizada. 400 con `campos.clienteId` o `campos.usuarioId`; 404 si el usuario no existe.
 */
export async function asignarEmpresa(usuarioId: number, clienteId: number): Promise<UsuarioCliente> {
  const { data } = await http.put<UsuarioCliente>(`/administracion/usuarios-cliente/${usuarioId}/empresa`, {
    clienteId,
  })
  return data
}

/** GET /administracion/accesos-denegados?pagina=&tamano=: del más reciente al más antiguo (tamano máximo 50). */
export async function listarAccesosDenegados(pagina = 0, tamano = 20): Promise<Pagina<AccesoDenegado>> {
  const { data } = await http.get<Pagina<AccesoDenegado>>('/administracion/accesos-denegados', {
    params: { pagina, tamano },
  })
  return data
}
