import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as catalogosApi from '../api/catalogos'
import * as clientesApi from '../api/clientes'
import * as despachosApi from '../api/despachos'
import { renderEnRuta } from '../test/utils'
import type { Despacho } from '../types'
import { DespachoFormPage } from './DespachoFormPage'

vi.mock('../api/auth')
vi.mock('../api/catalogos')
vi.mock('../api/clientes')
vi.mock('../api/despachos')

const CLIENTE = { id: 2, ruc: '20230972151', razonSocial: 'Comercial Lima SA', estadoCartaPoder: 'SIN_CARTA' as const }

function renderizar() {
  return renderEnRuta(<DespachoFormPage />, { ruta: '/despachos/nuevo', patron: '/despachos/nuevo' })
}

async function elegirCliente(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByLabelText(/Cliente \/ Importador/))
  await user.click(await screen.findByText(/Comercial Lima SA/))
}

async function elegirOpcion(user: ReturnType<typeof userEvent.setup>, etiqueta: RegExp, opcion: RegExp) {
  await user.click(screen.getByRole('combobox', { name: etiqueta }))
  await user.click(await screen.findByRole('option', { name: opcion }))
}

describe('DespachoFormPage (HU-03)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    vi.mocked(catalogosApi.listarRegimenes).mockResolvedValue([
      { id: 1, codigo: '10', descripcion: 'Importación para el consumo' },
    ])
    vi.mocked(catalogosApi.listarAduanas).mockResolvedValue([{ id: 1, codigo: '118', nombre: 'Marítima del Callao' }])
    vi.mocked(clientesApi.buscarClientes).mockResolvedValue({
      contenido: [CLIENTE],
      pagina: 0,
      tamano: 20,
      totalElementos: 1,
      totalPaginas: 1,
    })
  })

  it('muestra el N° de Despacho en solo lectura hasta guardar', () => {
    renderizar()
    expect(screen.getByLabelText(/N° de Despacho/)).toHaveValue('Se generará al guardar')
  })

  it('CA3: con obligatorios vacíos resalta los campos e impide continuar', async () => {
    const user = userEvent.setup()
    renderizar()

    await user.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    expect(await screen.findAllByText('Campo obligatorio')).toHaveLength(4)
    expect(despachosApi.crearDespacho).not.toHaveBeenCalled()
  })

  it('«Guardar como borrador» solo exige el cliente', async () => {
    vi.mocked(despachosApi.crearDespacho).mockResolvedValue({ id: 5, numero: null } as Despacho)
    const user = userEvent.setup()
    renderizar()

    await elegirCliente(user)
    await user.click(screen.getByRole('button', { name: 'Guardar como borrador' }))

    await waitFor(() => expect(screen.getByTestId('ubicacion')).toHaveTextContent('/despachos/5'))
    expect(despachosApi.crearDespacho).toHaveBeenCalledWith(expect.objectContaining({ clienteId: 2, borrador: true }))
  })

  it('CA2: sin carta poder vigente muestra la alerta de restricción', async () => {
    vi.mocked(despachosApi.crearDespacho).mockRejectedValue(
      new AxiosError('422', '422', undefined, undefined, {
        status: 422,
        statusText: '',
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: {
          codigo: 'CARTA_PODER_NO_VIGENTE',
          mensaje: 'El cliente no cuenta con carta poder vigente. Actualice la carta poder antes de abrir el despacho.',
        },
      }),
    )
    const user = userEvent.setup()
    renderizar()

    await elegirCliente(user)
    await elegirOpcion(user, /Régimen aduanero/, /Importación para el consumo/)
    await elegirOpcion(user, /Aduana de destino/, /Marítima del Callao/)
    await user.type(screen.getByLabelText(/Descripción general de la mercancía/), 'Repuestos')
    await user.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('no cuenta con carta poder vigente')
    expect(screen.getByRole('link', { name: 'Ir al cliente' })).toHaveAttribute('href', '/clientes/2')
  })
})
