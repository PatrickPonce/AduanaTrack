import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'

import * as authApi from '../api/auth'
import { UbicacionActual } from '../test/utils'
import { AuthProvider } from './AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('../api/auth')

function renderizar() {
  return render(
    <MemoryRouter initialEntries={['/clientes']}>
      <AuthProvider>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/clientes" element={<p>Pantalla protegida</p>} />
          </Route>
          <Route path="/login" element={<p>Login</p>} />
        </Routes>
        <UbicacionActual />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('sin sesión redirige al login', async () => {
    vi.mocked(authApi.sesionActual).mockRejectedValue(new Error('401'))
    renderizar()

    expect(await screen.findByText('Login')).toBeInTheDocument()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/login')
  })

  it('con sesión muestra la pantalla', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    renderizar()

    expect(await screen.findByText('Pantalla protegida')).toBeInTheDocument()
  })
})

describe('ProtectedRoute con roles (HU-16)', () => {
  function conRoles(ruta: string) {
    return render(
      <MemoryRouter initialEntries={[ruta]}>
        <AuthProvider>
          <Routes>
            <Route element={<ProtectedRoute roles={['ADMIN', 'AGENTE']} />}>
              <Route path="/clientes" element={<p>Pantalla de agente</p>} />
            </Route>
            <Route element={<ProtectedRoute roles={['CLIENTE']} />}>
              <Route path="/portal-cliente/mis-despachos" element={<p>Mis despachos</p>} />
            </Route>
            <Route path="/dashboard" element={<p>Dashboard</p>} />
            <Route path="/login" element={<p>Login</p>} />
          </Routes>
          <UbicacionActual />
        </AuthProvider>
      </MemoryRouter>,
    )
  }

  it('el CLIENTE que abre a mano una pantalla de agente vuelve a «Mis despachos»', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'cliente', nombre: 'Cliente', rol: 'CLIENTE' })
    conRoles('/clientes')

    expect(await screen.findByText('Mis despachos')).toBeInTheDocument()
    expect(screen.queryByText('Pantalla de agente')).not.toBeInTheDocument()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/portal-cliente/mis-despachos')
  })

  it('el agente que abre el portal del cliente vuelve al Dashboard', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    conRoles('/portal-cliente/mis-despachos')

    expect(await screen.findByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/dashboard')
  })

  it('con el rol correcto muestra la pantalla', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'admin', nombre: 'Admin', rol: 'ADMIN' })
    conRoles('/clientes')

    expect(await screen.findByText('Pantalla de agente')).toBeInTheDocument()
  })
})
