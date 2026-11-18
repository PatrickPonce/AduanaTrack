import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as notificacionesApi from '../api/notificaciones'
import { renderEnRuta } from '../test/utils'
import type { NotificacionBandeja } from '../types'
import { NotificacionesPage } from './NotificacionesPage'

vi.mock('../api/auth')
vi.mock('../api/notificaciones')

const NOTIFICACION: NotificacionBandeja = {
  id: 7,
  tipo: 'INSPECCION_CON_OBSERVACIONES',
  severidad: 'ALTA',
  titulo: 'Inspección con observaciones',
  mensaje: 'El despacho DESP-2026-0001 tiene observaciones.',
  despachoId: 1,
  numeroDespacho: 'DESP-2026-0001',
  leida: false,
  creadaEn: '2026-11-16T09:15:00',
  leidaEn: null,
}

describe('NotificacionesPage (HU-14)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    vi.mocked(notificacionesApi.obtenerPreferencia).mockResolvedValue('SISTEMA')
    vi.mocked(notificacionesApi.listarNotificaciones).mockResolvedValue({
      contenido: [NOTIFICACION],
      pagina: 0,
      tamano: 50,
      totalElementos: 1,
      totalPaginas: 1,
    })
  })

  it('lista las notificaciones y enlaza al despacho', async () => {
    renderEnRuta(<NotificacionesPage />, { ruta: '/notificaciones', patron: '/notificaciones' })
    expect(await screen.findByText('Inspección con observaciones')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Ver despacho DESP-2026-0001/ })).toHaveAttribute('href', '/despachos/1')
  })

  it('marca como leída', async () => {
    vi.mocked(notificacionesApi.marcarLeida).mockResolvedValue({ ...NOTIFICACION, leida: true })
    const user = userEvent.setup()
    renderEnRuta(<NotificacionesPage />, { ruta: '/notificaciones', patron: '/notificaciones' })
    await user.click(await screen.findByRole('button', { name: 'Marcar como leída' }))
    await waitFor(() => expect(notificacionesApi.marcarLeida).toHaveBeenCalledWith(7))
    // La campana se entera sin esperar a que el usuario cambie de pantalla.
    expect(notificacionesApi.avisarCambioNotificaciones).toHaveBeenCalled()
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Marcar como leída' })).not.toBeInTheDocument())
  })

  it('muestra el error si la bandeja no carga', async () => {
    vi.mocked(notificacionesApi.listarNotificaciones).mockRejectedValue(new Error('x'))
    renderEnRuta(<NotificacionesPage />, { ruta: '/notificaciones', patron: '/notificaciones' })
    expect(await screen.findByText('No se pudo conectar con el servidor.')).toBeInTheDocument()
  })

  it('permite elegir el canal de aviso y lo guarda', async () => {
    vi.mocked(notificacionesApi.guardarPreferencia).mockResolvedValue('AMBOS')
    const user = userEvent.setup()
    renderEnRuta(<NotificacionesPage />, { ruta: '/notificaciones', patron: '/notificaciones' })

    await user.click(await screen.findByRole('combobox', { name: /cómo quiere recibir los avisos/i }))
    await user.click(await screen.findByRole('option', { name: 'Dentro del sistema y por correo' }))

    await waitFor(() => expect(notificacionesApi.guardarPreferencia).toHaveBeenCalledWith('AMBOS'))
    expect(await screen.findByText('Preferencia guardada.')).toBeInTheDocument()
  })
})
