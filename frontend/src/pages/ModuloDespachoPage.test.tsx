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

  it('«Depósitos / Citas» (HU-11) tiene por gestionar los despachos con el canal asignado', async () => {
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([
      fila(1, 'DAM_NUMERADA'),
      fila(2, 'CANAL_ASIGNADO'),
      fila(3, 'LEVANTE_OTORGADO'),
      fila(4, 'RETIRO_PROGRAMADO'),
    ])
    const user = userEvent.setup()
    renderEnRuta(<ModuloDespachoPage modulo="citas" />, { ruta: '/citas', patron: '/citas' })

    expect(await screen.findByText('Citas en depósito')).toBeInTheDocument()
    expect(screen.getByLabelText('Por gestionar')).toHaveTextContent('2')
    expect(screen.getByLabelText('Completados')).toHaveTextContent('1')
    expect(screen.getByLabelText('En espera')).toHaveTextContent('1')

    await user.click(screen.getByText('DESP-2026-0002'))
    expect(JSON.parse(screen.getByTestId('ubicacion').dataset.state!)).toEqual({ pestana: 'citas', volverA: '/citas' })
  })

  it('abre el despacho directo en la pestaña del módulo', async () => {
    const user = userEvent.setup()
    renderEnRuta(<ModuloDespachoPage modulo="dam" />, { ruta: '/dam', patron: '/dam' })

    await user.click(await screen.findByText('DESP-2026-0003'))

    const ubicacion = screen.getByTestId('ubicacion')
    expect(ubicacion).toHaveTextContent('/despachos/3')
    expect(JSON.parse(ubicacion.dataset.state!)).toEqual({ pestana: 'dam', volverA: '/dam' })
  })

  it('«Inspecciones» (HU-12): por gestionar con cita programada, completados desde la conformidad y en espera sin cita', async () => {
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([
      fila(1, 'CANAL_ASIGNADO'),
      fila(2, 'INSPECCION_PROGRAMADA'),
      fila(3, 'INSPECCION_CONFORME'),
      fila(4, 'LEVANTE_OTORGADO'),
      fila(5, 'RETIRO_PROGRAMADO'),
      fila(6, 'DAM_NUMERADA'),
    ])
    renderEnRuta(<ModuloDespachoPage modulo="inspeccion" />, { ruta: '/inspecciones', patron: '/inspecciones' })

    expect(await screen.findByRole('heading', { name: 'Inspecciones' })).toBeInTheDocument()
    expect(await screen.findByText('DESP-2026-0002')).toBeInTheDocument()
    expect(screen.getByLabelText('Por gestionar')).toHaveTextContent('1')
    expect(screen.getByLabelText('Completados')).toHaveTextContent('3')
    expect(screen.getByLabelText('En espera')).toHaveTextContent('2')
    expect(screen.getByText('Aún no llegan a «Inspección programada»')).toBeInTheDocument()
    // Pantalla de usuario final: sin códigos de historia de usuario.
    expect(screen.queryByText(/HU-\d/)).not.toBeInTheDocument()
  })

  it('«Inspecciones» abre el despacho en la pestaña Inspección y vuelve al módulo', async () => {
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([fila(7, 'INSPECCION_PROGRAMADA')])
    const user = userEvent.setup()
    renderEnRuta(<ModuloDespachoPage modulo="inspeccion" />, { ruta: '/inspecciones', patron: '/inspecciones' })

    await user.click(await screen.findByText('DESP-2026-0007'))

    const ubicacion = screen.getByTestId('ubicacion')
    expect(ubicacion).toHaveTextContent('/despachos/7')
    expect(JSON.parse(ubicacion.dataset.state!)).toEqual({ pestana: 'inspeccion', volverA: '/inspecciones' })
  })
})
