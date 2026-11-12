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

describe('DashboardPage · Flujo del despacho', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'agente', nombre: 'Agente', rol: 'AGENTE' })
    vi.mocked(catalogosApi.listarAduanas).mockResolvedValue([])
    vi.mocked(dashboardApi.obtenerIndicadores).mockResolvedValue({
      despachosActivos: 2,
      enCanalNaranjaRojo: 1,
      conLevante: 2,
      conAlertasCriticas: 0,
    })
    vi.mocked(dashboardApi.listarDespachosTablero).mockResolvedValue([
      { ...fila(1, 'DESP-2026-0001', '2026-10-15'), estado: 'CANAL_ASIGNADO', canalControl: 'VERDE' },
      { ...fila(2, 'DESP-2026-0002', '2026-10-16'), estado: 'INSPECCION_PROGRAMADA', canalControl: 'ROJO' },
      { ...fila(3, 'DESP-2026-0003', '2026-10-17'), estado: 'LEVANTE_OTORGADO', canalControl: 'VERDE' },
      { ...fila(4, 'DESP-2026-0004', '2026-10-18'), estado: 'RETIRO_PROGRAMADO', canalControl: 'NARANJA' },
    ])
  })

  it('se titula «Flujo del despacho» sin mencionar sprints ni HU', async () => {
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })

    expect(await screen.findByText('Flujo del despacho')).toBeInTheDocument()
    expect(screen.queryByText(/sprint/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/HU-\d/)).not.toBeInTheDocument()
  })

  it('cubre las 11 etapas en el orden real, de Apertura a Retiro programado', async () => {
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })
    await screen.findByText('DESP-2026-0001')

    const etapas = within(screen.getByRole('list', { name: 'Etapas del despacho' })).getAllByRole('button')
    expect(etapas.map((b) => b.getAttribute('aria-label'))).toEqual([
      'Paso 1, Apertura: 0',
      'Paso 2, Documentación completa: 0',
      'Paso 3, Clasificado: 0',
      'Paso 4, Liquidado: 0',
      'Paso 5, Tributos pagados: 0',
      'Paso 6, DAM numerada: 0',
      'Paso 7, Canal asignado: 1',
      'Paso 8, Inspección programada: 1',
      'Paso 9, Inspección conforme: 0',
      'Paso 10, Levante otorgado: 1',
      'Paso 11, Retiro programado: 1',
    ])
  })

  it('cada etapa nueva lleva al módulo donde se trabaja el paso siguiente', async () => {
    const user = userEvent.setup()
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })
    await screen.findByText('DESP-2026-0001')

    await user.click(screen.getByRole('button', { name: /^Paso 8, Inspección programada/ }))
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/inspecciones')
  })

  it('«Inspección conforme» lleva a Levante, el paso que sigue', async () => {
    const user = userEvent.setup()
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })
    await screen.findByText('DESP-2026-0001')

    await user.click(screen.getByRole('button', { name: /^Paso 9, Inspección conforme/ }))
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/levante')
  })

  it('la tabla muestra los estados nuevos con su texto (un retiro programado ya tiene levante)', async () => {
    renderEnRuta(<DashboardPage />, { ruta: '/dashboard', patron: '/dashboard' })

    const estado = async (numero: string) =>
      within((await screen.findByText(numero)).closest('tr')!).getAllByRole('cell')[5].textContent
    expect(await estado('DESP-2026-0002')).toBe('Inspección programada')
    expect(await estado('DESP-2026-0004')).toBe('Retiro programado')
    expect(screen.getByLabelText('Con levante registrado')).toHaveTextContent('2')
    expect(screen.getByLabelText('Despachos activos')).toHaveTextContent('2')
  })
})
