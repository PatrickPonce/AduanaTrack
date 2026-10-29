import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as dashboardApi from '../api/dashboard'
import { renderEnRuta } from '../test/utils'
import type { DespachoTablero, EstadoDespacho } from '../types'
import { ModuloDespachoPage } from './ModuloDespachoPage'

vi.mock('../api/auth')
vi.mock('../api/dashboard')

function fila(id: number, estado: EstadoDespacho): DespachoTablero {
  return {
    id,
    numero: `DESP-2026-000${id}`,
    clienteId: 1,
    cliente: 'Importadora Andina SAC',
    aduanaCodigo: '118',
    aduana: 'Marítima del Callao',
    numeroBl: null,
    contenedor: null,
    totalContenedores: 0,
    fechaEta: null,
    estado,
    canalControl: null,
    diasAlmacenaje: null,
    alertaAlmacenaje: false,
    estadoCartaPoder: 'VIGENTE',
    alertaCritica: false,
  }
}

describe('ModuloDespachoPage (menú de los módulos del Sprint 2)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([
      fila(1, 'APERTURA'),
      fila(2, 'CLASIFICADO'),
      fila(3, 'DAM_NUMERADA'),
    ])
  })

  it('clasifica cada despacho según su avance en el módulo de Tributos (HU-08)', async () => {
    renderEnRuta(<ModuloDespachoPage modulo="tributos" />, { ruta: '/tributos', patron: '/tributos' })

    const situacion = async (numero: string) =>
      within((await screen.findByText(numero)).closest('tr')!).getAllByRole('cell')[4].textContent

    expect(await situacion('DESP-2026-0001')).toBe('En espera')
    expect(await situacion('DESP-2026-0002')).toBe('Por gestionar')
    expect(await situacion('DESP-2026-0003')).toBe('Completado')
    expect(screen.getByLabelText('Por gestionar')).toHaveTextContent('1')
  })

  it('abre el despacho directo en la pestaña del módulo', async () => {
    const user = userEvent.setup()
    renderEnRuta(<ModuloDespachoPage modulo="dam" />, { ruta: '/dam', patron: '/dam' })

    await user.click(await screen.findByText('DESP-2026-0003'))

    const ubicacion = screen.getByTestId('ubicacion')
    expect(ubicacion).toHaveTextContent('/despachos/3')
    expect(JSON.parse(ubicacion.dataset.state!)).toEqual({ pestana: 'dam', volverA: '/dam' })
  })
})
