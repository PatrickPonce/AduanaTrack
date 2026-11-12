import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from './api/auth'
import * as dashboardApi from './api/dashboard'
import * as catalogosApi from './api/catalogos'
import * as portalApi from './api/portalCliente'
import * as usuariosApi from './api/usuariosCliente'
import { App } from './App'
import { AuthProvider } from './auth/AuthContext'
import { UbicacionActual } from './test/utils'
import type { UsuarioSesion } from './types'

vi.mock('./api/auth')
vi.mock('./api/catalogos')
vi.mock('./api/dashboard')
vi.mock('./api/portalCliente')
vi.mock('./api/usuariosCliente')

const CLIENTE: UsuarioSesion = {
  usuario: 'rquispe',
  nombre: 'Rosa Quispe',
  rol: 'CLIENTE',
  empresa: { id: 1, ruc: '20100047218', razonSocial: 'Importadora Andina SAC' },
}

function abrir(ruta: string) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AuthProvider>
        <App />
        <UbicacionActual />
      </AuthProvider>
    </MemoryRouter>,
  )
}

/** Rutas de App.tsx con la sesión de cada rol (HU-16): la UI redirige; el backend igual filtra. */
describe('App · rutas por rol (HU-16)', () => {
  beforeEach(() => {
    vi.mocked(portalApi.listarMisDespachos).mockResolvedValue([])
    vi.mocked(usuariosApi.listarUsuariosCliente).mockResolvedValue([])
    vi.mocked(usuariosApi.listarAccesosDenegados).mockResolvedValue([])
    vi.mocked(catalogosApi.listarAduanas).mockResolvedValue([])
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([])
    vi.mocked(dashboardApi.obtenerIndicadores).mockResolvedValue({
      despachosActivos: 0,
      enCanalNaranjaRojo: 0,
      conLevante: 0,
      conAlertasCriticas: 0,
    })
  })

  it.each(['/dashboard', '/clientes', '/embarques', '/citas', '/despachos/5', '/administracion/usuarios-cliente'])(
    'el CLIENTE que abre %s termina en «Mis despachos»',
    async (ruta) => {
      vi.mocked(authApi.sesionActual).mockResolvedValue(CLIENTE)
      abrir(ruta)

      await waitFor(() =>
        expect(screen.getByTestId('ubicacion')).toHaveTextContent('/portal-cliente/mis-despachos'),
      )
      expect(await screen.findByText(/Solo se muestran los despachos de/)).toBeInTheDocument()
    },
  )

  it('una ruta desconocida lleva al CLIENTE a su portal', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue(CLIENTE)
    abrir('/no-existe')

    await waitFor(() =>
      expect(screen.getByTestId('ubicacion')).toHaveTextContent('/portal-cliente/mis-despachos'),
    )
  })

  it('el AGENTE no entra a la administración de usuarios cliente', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    abrir('/administracion/usuarios-cliente')

    await waitFor(() => expect(screen.getByTestId('ubicacion')).toHaveTextContent('/dashboard'))
  })

  it('el ADMIN entra a «Usuarios cliente»', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'admin', nombre: 'Admin', rol: 'ADMIN' })
    abrir('/administracion/usuarios-cliente')

    expect(await screen.findByRole('table', { name: 'Intentos de acceso denegados' })).toBeInTheDocument()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/administracion/usuarios-cliente')
  })
})
