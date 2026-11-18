import { act, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as notificacionesApi from '../api/notificaciones'
import { renderEnRuta } from '../test/utils'
import { CampanaNotificaciones } from './CampanaNotificaciones'

vi.mock('../api/auth')
vi.mock('../api/notificaciones', async (original) => {
  const real = await original<typeof notificacionesApi>()
  return { ...real, cantidadNoLeidas: vi.fn() }
})

describe('CampanaNotificaciones (HU-14)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
  })

  it('muestra el contador y lo vuelve a pedir cuando otra pantalla avisa de un cambio', async () => {
    vi.mocked(notificacionesApi.cantidadNoLeidas).mockResolvedValueOnce(3).mockResolvedValue(2)
    renderEnRuta(<CampanaNotificaciones />, { ruta: '/dashboard', patron: '/dashboard' })

    expect(await screen.findByRole('link', { name: 'Notificaciones (3 sin leer)' })).toBeInTheDocument()

    act(() => notificacionesApi.avisarCambioNotificaciones())

    await waitFor(() => expect(screen.getByRole('link', { name: 'Notificaciones (2 sin leer)' })).toBeInTheDocument())
  })

  it('sin no leídas no muestra número', async () => {
    vi.mocked(notificacionesApi.cantidadNoLeidas).mockResolvedValue(0)
    renderEnRuta(<CampanaNotificaciones />, { ruta: '/dashboard', patron: '/dashboard' })

    expect(await screen.findByRole('link', { name: 'Notificaciones' })).toBeInTheDocument()
  })
})
