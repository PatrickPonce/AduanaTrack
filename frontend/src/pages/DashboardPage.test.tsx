import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as catalogosApi from '../api/catalogos'
import * as clientesApi from '../api/clientes'
import * as dashboardApi from '../api/dashboard'
import { renderEnRuta } from '../test/utils'
import type { DespachoTablero } from '../types'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/auth')
vi.mock('../api/catalogos')
vi.mock('../api/clientes')
vi.mock('../api/dashboard')

function fila(id: number, numero: string, eta: string): DespachoTablero {
  return {
    id,
    numero,
    clienteId: 1,
    cliente: 'Importadora Andina SAC',
    aduanaCodigo: '118',
    aduana: 'Marítima del Callao',
    numeroBl: `HBL-${id}`,
    contenedor: null,
    totalContenedores: 0,
    fechaEta: eta,
    estado: 'APERTURA',
    canalControl: null,
    diasAlmacenaje: 30,
    alertaAlmacenaje: false,
    estadoCartaPoder: 'VIGENTE',
    alertaCritica: false,
  }
}

describe('DashboardPage (HU-02)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    vi.mocked(catalogosApi.listarAduanas).mockResolvedValue([{ id: 1, codigo: '118', nombre: 'Marítima del Callao' }])
    vi.mocked(clientesApi.buscarClientes).mockResolvedValue({
      contenido: [],
      pagina: 0,
      tamano: 20,
      totalElementos: 0,
      totalPaginas: 0,
    })
    vi.mocked(dashboardApi.obtenerIndicadores).mockResolvedValue({
      despachosActivos: 3,
      enCanalNaranjaRojo: 0,
      conLevante: 0,
      conAlertasCriticas: 1,
    })
    // El API ya entrega la tabla ordenada por ETA ascendente.
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([
      fila(2, 'DESP-2026-0002', '2026-10-15'),
      fila(3, 'DESP-2026-0003', '2026-10-20'),
      fila(1, 'DESP-2026-0001', '2026-10-25'),
    ])
  })

  it('CA1: muestra las 4 tarjetas con sus valores', async () => {
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })

    expect(await screen.findByLabelText('Despachos activos')).toHaveTextContent('3')
    expect(screen.getByLabelText('En canal naranja/rojo')).toHaveTextContent('0')
    expect(screen.getByLabelText('Con levante registrado')).toHaveTextContent('0')
    expect(screen.getByLabelText('Con alertas críticas')).toHaveTextContent('1')
  })

  it('CA2: la tabla «Despachos recientes» respeta el orden por ETA', async () => {
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })

    await screen.findByText('DESP-2026-0002')
    const filas = screen.getAllByRole('row').slice(1)
    expect(filas.map((f) => within(f).getAllByRole('cell')[6].textContent)).toEqual([
      '15/10/2026',
      '20/10/2026',
      '25/10/2026',
    ])
  })

  it('muestra el contenedor del BL y el canal de control como en el prototipo', async () => {
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([
      { ...fila(4, 'DESP-2026-0004', '2026-10-15'), contenedor: 'MSCU7841205', totalContenedores: 2, canalControl: 'ROJO' },
    ])
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })

    const celdas = within((await screen.findByText('DESP-2026-0004')).closest('tr')!).getAllByRole('cell')
    expect(celdas[2]).toHaveTextContent('MSCU7841205 (+1)BL HBL-4')
    expect(celdas[4]).toHaveTextContent('Rojo')
    expect(celdas[5]).toHaveTextContent('Apertura')
  })

  it('CA3: el clic en una fila abre el detalle y guarda los filtros para volver', async () => {
    const user = userEvent.setup()
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard?aduanaId=1', patron: '/dashboard' })

    await user.click(await screen.findByText('DESP-2026-0003'))

    const ubicacion = screen.getByTestId('ubicacion')
    expect(ubicacion).toHaveTextContent('/despachos/3')
    expect(JSON.parse(ubicacion.dataset.state!)).toEqual({ volverA: '/dashboard?aduanaId=1' })
  })

  it('CA4: «Aplicar filtro» guarda la aduana en la URL y recarga con el filtro', async () => {
    const user = userEvent.setup()
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })
    await screen.findByText('DESP-2026-0002')

    await user.click(screen.getByRole('combobox', { name: 'Aduana' }))
    await user.click(await screen.findByRole('option', { name: /Marítima del Callao/ }))
    await user.click(screen.getByRole('button', { name: 'Aplicar filtro' }))

    await waitFor(() => expect(screen.getByTestId('ubicacion')).toHaveTextContent('/dashboard?aduanaId=1'))
    expect(dashboardApi.listarDespachosTablero).toHaveBeenLastCalledWith({ aduanaId: 1 })
    expect(dashboardApi.obtenerIndicadores).toHaveBeenLastCalledWith({ aduanaId: 1 })
  })
})
