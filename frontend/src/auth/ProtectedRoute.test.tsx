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
