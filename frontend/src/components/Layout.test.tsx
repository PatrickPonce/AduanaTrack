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

describe('Layout · menú por rol (HU-16)', () => {
  const MENU_AGENTE = [
    'Dashboard',
    'Despachos',
    'Clientes',
    'Embarques',
    'Documentos',
    'Clasificación',
    'Tributos',
    'DAM (Registro)',
    'Canal de Control',
    'Depósitos / Citas',
    'Inspecciones',
    'Levante',
    'Reportes',
    'Notificaciones',
  ]

  const enlaces = () =>
    within(menuPrincipal())
      .getAllByRole('link')
      .map((a) => a.textContent)

  it('el CLIENTE solo ve «Mis despachos» y «Notificaciones» bajo «Portal del cliente»', async () => {
    renderizar('CLIENTE', '/portal-cliente/mis-despachos')
    await screen.findByText('Lucía Ramos')

    expect(enlaces()).toEqual(['Mis despachos', 'Notificaciones'])
    expect(within(menuPrincipal()).getByRole('heading', { name: 'Portal del cliente' })).toBeInTheDocument()
    for (const texto of ['Clientes', 'Embarques', 'Documentos', 'Clasificación', 'Tributos', 'Levante']) {
      expect(within(menuPrincipal()).queryByText(texto)).not.toBeInTheDocument()
    }
    expect(within(menuPrincipal()).getByRole('link', { name: 'Mis despachos' })).toHaveAttribute(
      'href',
      '/portal-cliente/mis-despachos',
    )
  })

  it('el AGENTE conserva su menú y no ve el portal ni la administración', async () => {
    renderizar('AGENTE')
    await screen.findByText('Lucía Ramos')

    expect(enlaces()).toEqual(MENU_AGENTE)
  })

  it('el ADMIN ve el menú del agente y suma «Administración → Usuarios cliente»', async () => {
    renderizar('ADMIN')
    await screen.findByText('Lucía Ramos')

    expect(enlaces()).toEqual([...MENU_AGENTE, 'Usuarios cliente'])
    expect(within(menuPrincipal()).getByRole('heading', { name: 'Administración' })).toBeInTheDocument()
    expect(within(menuPrincipal()).getByRole('link', { name: 'Usuarios cliente' })).toHaveAttribute(
      'href',
      '/administracion/usuarios-cliente',
    )
  })

  it('muestra al CLIENTE su empresa y RUC en el pie de la barra lateral', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({
      usuario: 'cliente',
      nombre: 'Lucía Ramos',
      rol: 'CLIENTE',
      empresa: { id: 1, ruc: '20100047218', razonSocial: 'Importadora Andina SAC' },
    })
    render(
      <MemoryRouter initialEntries={['/portal-cliente/mis-despachos']}>
        <AuthProvider>
          <Routes>
            <Route element={<Layout />}>
              <Route path="*" element={<p>Contenido</p>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    )

    const empresa = await screen.findByRole('group', { name: 'Empresa' })
    expect(empresa).toHaveTextContent('Importadora Andina SAC')
    expect(empresa).toHaveTextContent('RUC 20100047218')
  })

  it('el agente no ve el bloque de empresa', async () => {
    renderizar('AGENTE')
    await screen.findByText('Lucía Ramos')

    expect(screen.queryByRole('group', { name: 'Empresa' })).not.toBeInTheDocument()
  })
})
