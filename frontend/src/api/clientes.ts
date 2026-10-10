import type { CambioCliente, Cliente, ClienteExiste, ClienteRequest, ClienteResumen, Pagina } from '../types'
import { http } from './http'

export async function buscarClientes(q: string, page = 0, size = 20): Promise<Pagina<ClienteResumen>> {
  const { data } = await http.get<Pagina<ClienteResumen>>('/clientes', { params: { q, page, size } })
  return data
}

export async function existeRuc(ruc: string): Promise<ClienteExiste> {
  const { data } = await http.get<ClienteExiste>('/clientes/existe', { params: { ruc } })
  return data
}

export async function obtenerCliente(id: number): Promise<Cliente> {
  const { data } = await http.get<Cliente>(`/clientes/${id}`)
  return data
}

export async function crearCliente(cliente: ClienteRequest): Promise<Cliente> {
  const { data } = await http.post<Cliente>('/clientes', cliente)
  return data
}

export async function actualizarCliente(id: number, cliente: ClienteRequest): Promise<Cliente> {
  const { data } = await http.put<Cliente>(`/clientes/${id}`, cliente)
  return data
}

export async function historialCliente(id: number): Promise<CambioCliente[]> {
  const { data } = await http.get<CambioCliente[]>(`/clientes/${id}/historial`)
  return data
}

/** Carta poder en PDF (≤ 10 MB) con sus fechas de emisión y vencimiento (T-04.2). */
export async function subirCartaPoder(
  clienteId: number,
  archivo: File,
  fechaEmision: string,
  fechaVencimiento: string,
): Promise<void> {
  const formulario = new FormData()
  formulario.append('archivo', archivo)
  formulario.append('fechaEmision', fechaEmision)
  formulario.append('fechaVencimiento', fechaVencimiento)
  await http.post(`/clientes/${clienteId}/carta-poder`, formulario)
}
