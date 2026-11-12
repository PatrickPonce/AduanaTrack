import { render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import { AuthProvider } from '../auth/AuthContext'
import { UbicacionActual } from '../test/utils'
import type { Rol } from '../types'
import { Layout } from './Layout'

vi.mock('../api/auth')

function renderizar(rol: Rol = 'AGENTE', ruta = '/dashboard') {
  vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'usuario', nombre: 'Lucía Ramos', rol })
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path="*" element={<p>Contenido</p>} />
          </Route>
        </Routes>
        <UbicacionActual />
      </AuthProvider>
    </MemoryRouter>,
  )
}

/** El menú se dibuja dos veces (fijo en escritorio y en el Drawer móvil): basta con el primero. */
function menuPrincipal() {
  return screen.getAllByRole('navigation', { name: 'Menú principal' })[0]!
}

describe('Layout (menú lateral y barra superior)', () => {
  it('ya no muestra la sección «Próximamente» ni la marca «Pronto»', async () => {
    renderizar()
    await screen.findByText('Lucía Ramos')

    expect(screen.queryByText(/próximamente/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/pronto/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/sprint 2/i)).not.toBeInTheDocument()
  })

  it('«Inspecciones» y «Notificaciones» son enlaces reales con su ruta', async () => {
    renderizar()
    await screen.findByText('Lucía Ramos')
    const nav = within(menuPrincipal())

    expect(nav.getByRole('link', { name: 'Inspecciones' })).toHaveAttribute('href', '/inspecciones')
    expect(nav.getByRole('link', { name: 'Notificaciones' })).toHaveAttribute('href', '/notificaciones')
  })

  it('«Inspecciones» va entre «Depósitos / Citas» y «Levante», en el orden del despacho', async () => {
    renderizar()
    await screen.findByText('Lucía Ramos')
    const textos = within(menuPrincipal())
      .getAllByRole('link')
      .map((a) => a.textContent)

    expect(textos.indexOf('Inspecciones')).toBe(textos.indexOf('Depósitos / Citas') + 1)
    expect(textos.indexOf('Levante')).toBe(textos.indexOf('Inspecciones') + 1)
  })

  it('marca como actual el ítem de la ruta abierta', async () => {
    renderizar('AGENTE', '/inspecciones')
    await screen.findByText('Lucía Ramos')

    expect(within(menuPrincipal()).getByRole('link', { name: 'Inspecciones' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('pone la campana de notificaciones en la barra superior, junto al usuario', async () => {
    renderizar()
    await screen.findByText('Lucía Ramos')
    const barra = within(screen.getByRole('banner'))

    expect(barra.getByRole('link', { name: 'Notificaciones' })).toHaveAttribute('href', '/notificaciones')
    expect(barra.getByRole('button', { name: 'Cuenta de usuario' })).toBeInTheDocument()
  })

  it('muestra en el pie la versión de package.json y el Sprint 3', async () => {
    renderizar()
    await screen.findByText('Lucía Ramos')

    expect(screen.getAllByText(`v${__APP_VERSION__} · Sprint 3 · Grupo 2 USMP`)[0]).toBeInTheDocument()
    expect(__APP_VERSION__).toBe('0.3.0')
  })
})
