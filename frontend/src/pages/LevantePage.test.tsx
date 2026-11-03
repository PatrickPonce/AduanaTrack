import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as despachosApi from '../api/despachos'
import * as levanteApi from '../api/levante'
import { renderEnRuta } from '../test/utils'
import type { Despacho } from '../types'
import { LevantePage } from './LevantePage'

vi.mock('../api/auth')
vi.mock('../api/despachos')
vi.mock('../api/levante')

const DESPACHO: Despacho = {
  id: 7,
  numero: 'DESP-2026-0001',
  estado: 'LEVANTE_OTORGADO',
  cliente: { id: 2, ruc: '20230972151', razonSocial: 'Comercial Lima SA', estadoCartaPoder: 'VIGENTE' },
  regimen: null,
  aduana: null,
  referenciaCliente: null,
  descripcionMercancia: null,
  tipoOperacion: 'IMPORTACION_DEFINITIVA',
  modalidadDespacho: 'ANTICIPADO',
  canalControl: 'VERDE',
  fechaLevante: '2026-10-30',
  agente: 'Agente Demo',
  creadoEn: '2026-10-20T09:00:00',
}

describe('LevantePage (HU-13 · /levante/:despachoId)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(DESPACHO)
    vi.mocked(levanteApi.obtenerLevante).mockResolvedValue({
      numeroLevante: 'LEV-2026-000318',
      fechaLevante: '2026-10-30',
      constanciaNombre: null,
      conAdvertencia: false,
      retiroHabilitado: true,
      estadoDespacho: 'LEVANTE_OTORGADO',
    })
  })

  it('muestra el despacho con su levante y lleva a «Citas» con el retiro elegido', async () => {
    const user = userEvent.setup()
    renderEnRuta(<LevantePage />, { ruta: '/levante/7', patron: '/levante/:despachoId' })

    expect(await screen.findByRole('link', { name: 'DESP-2026-0001' })).toHaveAttribute('href', '/despachos/7')
    await user.click(await screen.findByRole('button', { name: 'Registrar cita de retiro' }))

    const ubicacion = screen.getByTestId('ubicacion')
    expect(ubicacion).toHaveTextContent('/despachos/7')
    expect(JSON.parse(ubicacion.dataset.state!)).toEqual({ pestana: 'citas', tipoCita: 'RETIRO', volverA: '/levante/7' })
  })

  it('si el despacho no existe muestra el error', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockRejectedValue(new Error('404'))
    renderEnRuta(<LevantePage />, { ruta: '/levante/99', patron: '/levante/:despachoId' })

    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })
})
