import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as clientesApi from '../api/clientes'
import * as usuariosApi from '../api/usuariosCliente'
import { renderEnRuta } from '../test/utils'
import type { ClienteResumen, UsuarioCliente } from '../types'
import { UsuariosClientePage } from './UsuariosClientePage'

vi.mock('../api/auth')
vi.mock('../api/clientes')
vi.mock('../api/usuariosCliente')

const RUTA = '/administracion/usuarios-cliente'

const ANDINA: ClienteResumen = {
  id: 1,
  ruc: '20100047218',
  razonSocial: 'Importadora Andina SAC',
  estadoCartaPoder: 'VIGENTE',
}
const PACIFICO: ClienteResumen = {
  id: 2,
  ruc: '20601574315',
  razonSocial: 'Comercial Pacífico EIRL',
  estadoCartaPoder: 'VIGENTE',
}
/** RUC con dígito verificador incorrecto (el correcto es 20123456786). */
const RUC_MALO: ClienteResumen = { id: 3, ruc: '20123456780', razonSocial: 'Datos Antiguos SA', estadoCartaPoder: 'VIGENTE' }

const USUARIOS: UsuarioCliente[] = [
  { id: 10, usuario: 'rquispe', nombre: 'Rosa Quispe', empresa: { id: 1, ruc: ANDINA.ruc, razonSocial: ANDINA.razonSocial } },
  { id: 11, usuario: 'jtorres', nombre: 'Jorge Torres', empresa: null },
]

async function elegirEmpresa(user: ReturnType<typeof userEvent.setup>, empresa: ClienteResumen) {
  vi.mocked(clientesApi.buscarClientes).mockResolvedValue({
    contenido: [empresa],
    pagina: 0,
    tamano: 20,
    totalElementos: 1,
    totalPaginas: 1,
  })
  await user.type(screen.getByRole('combobox', { name: /Empresa/ }), empresa.razonSocial.slice(0, 5))
  await user.click(await screen.findByRole('option', { name: new RegExp(empresa.razonSocial) }))
}

describe('UsuariosClientePage (HU-16 · Administración → Usuarios cliente)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'admin', nombre: 'Admin', rol: 'ADMIN' })
    vi.mocked(clientesApi.buscarClientes).mockResolvedValue({
      contenido: [],
      pagina: 0,
      tamano: 20,
      totalElementos: 0,
      totalPaginas: 0,
    })
    vi.mocked(usuariosApi.listarUsuariosCliente).mockResolvedValue(USUARIOS)
    vi.mocked(usuariosApi.listarAccesosDenegados).mockResolvedValue([
      {
        id: 1,
        fecha: '2026-11-09T16:45:00',
        usuario: 'rquispe',
        empresa: 'Importadora Andina SAC',
        recurso: '/api/v1/portal-cliente/despachos/12',
        motivo: 'El despacho pertenece a otra empresa',
      },
    ])
    vi.mocked(usuariosApi.asignarEmpresa).mockImplementation(async (id, clienteId) => ({
      ...USUARIOS.find((u) => u.id === id)!,
      empresa: { id: clienteId, ruc: PACIFICO.ruc, razonSocial: PACIFICO.razonSocial },
    }))
  })

  it('muestra cada usuario con su estado «Asignado» o «Sin asignar» y la acción que corresponde', async () => {
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    const tabla = await screen.findByRole('table', { name: 'Usuarios cliente' })
    const rosa = within(within(tabla).getByText('Rosa Quispe').closest('tr')!)
    expect(rosa.getByText('Importadora Andina SAC')).toBeInTheDocument()
    expect(rosa.getByText('Asignado')).toBeInTheDocument()
    expect(rosa.getByRole('button', { name: 'Cambiar empresa de Rosa Quispe' })).toHaveTextContent('Cambiar')

    const jorge = within(within(tabla).getByText('Jorge Torres').closest('tr')!)
    expect(jorge.getByText('Sin asignar')).toBeInTheDocument()
    expect(jorge.getByRole('button', { name: 'Asignar empresa de Jorge Torres' })).toHaveTextContent('Asignar')
  })

  it('asigna una empresa con RUC válido y recarga la tabla', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Jorge Torres')

    await user.click(screen.getByRole('button', { name: 'Asignar empresa de Jorge Torres' }))
    expect(screen.getByRole('combobox', { name: /Usuario cliente/ })).toHaveTextContent('Jorge Torres (jtorres)')
    expect(screen.getByRole('combobox', { name: /Usuario cliente/ })).toHaveFocus()
    await elegirEmpresa(user, PACIFICO)
    await user.click(screen.getByRole('button', { name: 'Asignar empresa' }))

    expect(usuariosApi.asignarEmpresa).toHaveBeenCalledWith(11, 2)
    expect(await screen.findByText('Se asignó Comercial Pacífico EIRL a Jorge Torres.')).toBeInTheDocument()
    await waitFor(() => expect(usuariosApi.listarUsuariosCliente).toHaveBeenCalledTimes(2))
  })

  it('no envía sin usuario ni empresa', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Jorge Torres')

    await user.click(screen.getByRole('button', { name: 'Asignar empresa' }))

    expect(screen.getByText('Seleccione el usuario cliente')).toBeInTheDocument()
    expect(screen.getByText('Seleccione la empresa')).toBeInTheDocument()
    expect(usuariosApi.asignarEmpresa).not.toHaveBeenCalled()
  })

  it('rechaza una empresa con RUC no válido', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Jorge Torres')

    await user.click(screen.getByRole('button', { name: 'Asignar empresa de Jorge Torres' }))
    await elegirEmpresa(user, RUC_MALO)
    await user.click(screen.getByRole('button', { name: 'Asignar empresa' }))

    expect(screen.getByText(/La empresa tiene un RUC no válido \(20123456780\)/)).toBeInTheDocument()
    expect(usuariosApi.asignarEmpresa).not.toHaveBeenCalled()
  })

  it('lista los intentos de acceso denegados', async () => {
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    const tabla = await screen.findByRole('table', { name: 'Intentos de acceso denegados' })
    const fila = await within(tabla).findByText('/api/v1/portal-cliente/despachos/12')
    expect(within(fila.closest('tr')!).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      '09/11/2026 16:45',
      'rquispe',
      'Importadora Andina SAC',
      '/api/v1/portal-cliente/despachos/12',
      'El despacho pertenece a otra empresa',
    ])
  })

  it('si falla el registro de accesos lo avisa sin ocultar la tabla de usuarios', async () => {
    vi.mocked(usuariosApi.listarAccesosDenegados).mockRejectedValue(new Error('red'))
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByText('No se pudo conectar con el servidor.')).toBeInTheDocument()
    expect(screen.getByText('Rosa Quispe')).toBeInTheDocument()
  })
})
