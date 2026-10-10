import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import { renderEnRuta } from '../test/utils'
import { LoginPage } from './LoginPage'

vi.mock('../api/auth')

function errorHttp(status: number) {
  return new AxiosError('error', String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { codigo: 'X', mensaje: 'X' },
  })
}

async function ingresar(usuario: string, password: string, recordarme = false) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText(/Usuario \/ Correo corporativo/), usuario)
  await user.type(screen.getByLabelText(/Contraseña/), password)
  if (recordarme) await user.click(screen.getByLabelText('Recordarme'))
  await user.click(screen.getByRole('button', { name: 'Ingresar al sistema' }))
}

describe('LoginPage (HU-01)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockRejectedValue(errorHttp(401))
  })

  it('CA1: con credenciales correctas va al panel de seguimiento', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ usuario: 'agente', nombre: 'Agente Demo', rol: 'AGENTE' })
    renderEnRuta(<LoginPage />, { ruta: '/login', patron: '/login' })

    await ingresar('agente', 'Demo#2026', true)

    await waitFor(() => expect(screen.getByTestId('ubicacion')).toHaveTextContent('/dashboard'))
    expect(authApi.login).toHaveBeenCalledWith('agente', 'Demo#2026', true)
  })

  it('CA2: un 401 muestra el mensaje genérico sin indicar qué dato falló', async () => {
    vi.mocked(authApi.login).mockRejectedValue(errorHttp(401))
    renderEnRuta(<LoginPage />, { ruta: '/login', patron: '/login' })

    await ingresar('agente', 'otra')

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario o contraseña incorrectos')
  })

  it('CA3: un 423 avisa que la cuenta está bloqueada 15 minutos', async () => {
    vi.mocked(authApi.login).mockRejectedValue(errorHttp(423))
    renderEnRuta(<LoginPage />, { ruta: '/login', patron: '/login' })

    await ingresar('agente', 'Demo#2026')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Cuenta bloqueada temporalmente. Intente nuevamente en 15 minutos.',
    )
  })

  it('no envía el formulario con campos vacíos', async () => {
    renderEnRuta(<LoginPage />, { ruta: '/login', patron: '/login' })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Ingresar al sistema' }))

    expect(authApi.login).not.toHaveBeenCalled()
    expect(screen.getByText('Ingrese su contraseña')).toBeInTheDocument()
  })
})
