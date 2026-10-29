import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as canalApi from '../api/canal'
import * as clasificacionApi from '../api/clasificacion'
import * as despachosApi from '../api/despachos'
import * as hitosApi from '../api/hitos'
import { renderEnRuta } from '../test/utils'
import type { Despacho, EstadoDespacho, ItemFactura, Rol } from '../types'
import { DespachoDetallePage } from './DespachoDetallePage'

vi.mock('../api/auth')
vi.mock('../api/canal')
vi.mock('../api/clasificacion')
vi.mock('../api/despachos')
vi.mock('../api/dam')
vi.mock('../api/documentos')
vi.mock('../api/hitos')
vi.mock('../api/liquidacion')

const RODAMIENTOS = { id: 1, codigoFormateado: '8482.10.00.00', descripcion: 'Rodamientos de bolas' }

/** Todos los ítems con partida: la clasificación se puede guardar. */
const ITEMS_CLASIFICADOS: ItemFactura[] = [
  { id: 10, numero: 1, descripcion: 'Rodamiento 6205', cantidad: 200, unidad: 'UND', valorFob: 4200, partidaId: 1, partida: RODAMIENTOS },
  { id: 11, numero: 2, descripcion: 'Rodamiento 6206', cantidad: 150, unidad: 'UND', valorFob: 5800, partidaId: 1, partida: RODAMIENTOS },
]

function despacho(estado: EstadoDespacho, canalControl: string | null = null): Despacho {
  return {
    id: 7,
    numero: 'DESP-2026-0001',
    estado,
    cliente: { id: 2, ruc: '20230972151', razonSocial: 'Comercial Lima SA', estadoCartaPoder: 'VIGENTE' },
    regimen: null,
    aduana: null,
    referenciaCliente: null,
    descripcionMercancia: null,
    tipoOperacion: 'IMPORTACION_DEFINITIVA',
    modalidadDespacho: 'ANTICIPADO',
    canalControl,
    fechaLevante: null,
    agente: 'Agente Demo',
    creadoEn: '2026-10-20T09:00:00',
  }
}

function mostrar(rol: Rol = 'AGENTE') {
  vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'usuario', nombre: 'Usuario', rol })
  return renderEnRuta(<DespachoDetallePage />, { ruta: '/despachos/7', patron: '/despachos/:id' })
}

const nombresDePestanas = () => screen.getAllByRole('tab').map((t) => t.textContent)

describe('DespachoDetallePage · pestañas del Sprint 2', () => {
  beforeEach(() => {
    vi.mocked(hitosApi.listarHitos).mockResolvedValue([])
  })

  it.each<[EstadoDespacho, string[]]>([
    ['APERTURA', ['Datos generales', 'Documentos', 'Línea de tiempo']],
    ['DOCUMENTACION_COMPLETA', ['Datos generales', 'Documentos', 'Clasificación', 'Línea de tiempo']],
    ['CLASIFICADO', ['Datos generales', 'Documentos', 'Clasificación', 'Liquidación', 'Línea de tiempo']],
    ['LIQUIDADO', ['Datos generales', 'Documentos', 'Clasificación', 'Liquidación', 'Línea de tiempo']],
    ['TRIBUTOS_PAGADOS', ['Datos generales', 'Documentos', 'Clasificación', 'Liquidación', 'DAM', 'Línea de tiempo']],
    [
      'DAM_NUMERADA',
      ['Datos generales', 'Documentos', 'Clasificación', 'Liquidación', 'DAM', 'Canal de control', 'Línea de tiempo'],
    ],
    [
      'CANAL_ASIGNADO',
      ['Datos generales', 'Documentos', 'Clasificación', 'Liquidación', 'DAM', 'Canal de control', 'Línea de tiempo'],
    ],
  ])('con el despacho en %s muestra las pestañas que le corresponden', async (estado, esperadas) => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho(estado))
    mostrar()

    await screen.findByRole('tab', { name: 'Datos generales' })

    expect(nombresDePestanas()).toEqual(esperadas)
  })

  it('un borrador todavía no tiene pestañas: primero se confirma el despacho', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho('BORRADOR'))
    mostrar()

    expect(await screen.findByText(/Confirme el despacho con «Guardar y continuar»/)).toBeInTheDocument()
    expect(screen.queryAllByRole('tab')).toHaveLength(0)
  })

  it('el chip de la cabecera muestra el estado en texto legible', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho('TRIBUTOS_PAGADOS'))
    mostrar()

    expect(await screen.findByText('Tributos pagados')).toBeInTheDocument()
  })

  it('«Línea de tiempo» lista los hitos del despacho', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho('APERTURA'))
    vi.mocked(hitosApi.listarHitos).mockResolvedValue([
      { tipo: 'APERTURA', descripcion: 'Despacho abierto (DESP-2026-0001)', sugerido: false, usuario: 'Agente Demo', fecha: '2026-10-20T09:00:00' },
    ])
    const user = userEvent.setup()
    mostrar()

    await user.click(await screen.findByRole('tab', { name: 'Línea de tiempo' }))

    const tabla = await screen.findByRole('table', { name: 'Línea de tiempo del despacho' })
    expect(within(tabla).getByText('Despacho abierto (DESP-2026-0001)')).toBeInTheDocument()
  })

  it('el perfil Cliente ve las pestañas pero solo en consulta', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho('CLASIFICADO'))
    vi.mocked(clasificacionApi.listarItems).mockResolvedValue(ITEMS_CLASIFICADOS)
    const user = userEvent.setup()
    mostrar('CLIENTE')

    await user.click(await screen.findByRole('tab', { name: 'Clasificación' }))
    expect(await screen.findByText('Rodamiento 6205')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar clasificación arancelaria' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'Liquidación' }))
    expect(await screen.findByText(/Solo el agente de aduana puede generar la liquidación/)).toBeInTheDocument()
  })
})

describe('DespachoDetallePage · HU-07 · T-07.5 (QA de los criterios de aceptación)', () => {
  beforeEach(() => {
    vi.mocked(clasificacionApi.listarItems).mockResolvedValue(ITEMS_CLASIFICADOS)
    vi.mocked(clasificacionApi.buscarPartidas).mockResolvedValue([])
    vi.mocked(clasificacionApi.guardarItems).mockResolvedValue(ITEMS_CLASIFICADOS)
    vi.mocked(clasificacionApi.guardarClasificacion).mockResolvedValue({
      estado: 'CLASIFICADO',
      clasificadoPor: 'Agente Demo',
      clasificadoEn: '2026-10-28T11:30:00',
    })
  })

  it('CA4: al guardar la clasificación de todos los ítems se habilita el módulo de cálculo de tributos', async () => {
    vi.mocked(despachosApi.obtenerDespacho)
      .mockResolvedValueOnce(despacho('DOCUMENTACION_COMPLETA'))
      .mockResolvedValue(despacho('CLASIFICADO'))
    const user = userEvent.setup()
    mostrar()

    await user.click(await screen.findByRole('tab', { name: 'Clasificación' }))
    // Antes de clasificar no hay cálculo de tributos.
    expect(screen.queryByRole('tab', { name: 'Liquidación' })).not.toBeInTheDocument()

    await user.click(await screen.findByRole('button', { name: 'Guardar clasificación arancelaria' }))

    expect(await screen.findByText(/registrada por Agente Demo el 28\/10\/2026 11:30/)).toBeInTheDocument()
    const pestanaLiquidacion = await screen.findByRole('tab', { name: 'Liquidación' })
    await user.click(pestanaLiquidacion)
    expect(await screen.findByRole('button', { name: 'Generar liquidación' })).toBeInTheDocument()
    expect(screen.getByText('Clasificado')).toBeInTheDocument()
  })

  it('CA3: con un ítem sin partida el guardado sigue bloqueado y no se abre el cálculo de tributos', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho('DOCUMENTACION_COMPLETA'))
    vi.mocked(clasificacionApi.listarItems).mockResolvedValue([
      ITEMS_CLASIFICADOS[0]!,
      { ...ITEMS_CLASIFICADOS[1]!, partidaId: null, partida: null },
    ])
    const user = userEvent.setup()
    mostrar()

    await user.click(await screen.findByRole('tab', { name: 'Clasificación' }))

    expect(await screen.findByRole('button', { name: 'Guardar clasificación arancelaria' })).toBeDisabled()
    expect(screen.getByRole('status')).toHaveTextContent('Falta asignar la partida a 1 ítem.')
    expect(screen.queryByRole('tab', { name: 'Liquidación' })).not.toBeInTheDocument()
  })

  it('CA2: la partida asignada llega al API en el campo partidaId de cada ítem', async () => {
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(despacho('DOCUMENTACION_COMPLETA'))
    const user = userEvent.setup()
    mostrar()

    await user.click(await screen.findByRole('tab', { name: 'Clasificación' }))
    await user.click(await screen.findByRole('button', { name: 'Guardar clasificación arancelaria' }))

    await waitFor(() => expect(clasificacionApi.guardarClasificacion).toHaveBeenCalledWith(7))
    expect(clasificacionApi.guardarItems).toHaveBeenCalledWith(7, [
      { id: 10, descripcion: 'Rodamiento 6205', cantidad: 200, unidad: 'UND', valorFob: 4200, partidaId: 1 },
      { id: 11, descripcion: 'Rodamiento 6206', cantidad: 150, unidad: 'UND', valorFob: 5800, partidaId: 1 },
    ])
  })
})

describe('DespachoDetallePage · HU-10 · canal de control', () => {
  it('guardar el canal actualiza la cabecera y lleva a la línea de tiempo con el hito nuevo', async () => {
    vi.mocked(despachosApi.obtenerDespacho)
      .mockResolvedValueOnce(despacho('DAM_NUMERADA'))
      .mockResolvedValue(despacho('CANAL_ASIGNADO', 'ROJO'))
    vi.mocked(canalApi.guardarCanal).mockResolvedValue({
      canal: 'ROJO',
      estado: 'CANAL_ASIGNADO',
      levanteHabilitado: false,
      hitoSugerido: 'Programar inspección física',
    })
    vi.mocked(hitosApi.listarHitos).mockResolvedValue([
      { tipo: 'CANAL_ASIGNADO', descripcion: 'Canal de control asignado: ROJO', sugerido: false, usuario: 'Agente Demo', fecha: '2026-10-28T10:00:00' },
      { tipo: 'HITO_SUGERIDO', descripcion: 'Programar inspección física', sugerido: true, usuario: 'Sistema', fecha: '2026-10-28T10:00:00' },
    ])
    const user = userEvent.setup()
    mostrar()

    await user.click(await screen.findByRole('tab', { name: 'Canal de control' }))
    await user.click(screen.getByRole('combobox', { name: /Canal informado por SUNAT/ }))
    await user.click(await screen.findByRole('option', { name: 'Rojo' }))
    await user.click(screen.getByRole('button', { name: 'Guardar canal' }))

    expect(await screen.findByText('Hito sugerido: Programar inspección física.')).toBeInTheDocument()
    expect(await screen.findByText('Canal asignado')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ver línea de tiempo' }))

    const tabla = await screen.findByRole('table', { name: 'Línea de tiempo del despacho' })
    expect(within(tabla).getByText('Canal de control asignado: ROJO')).toBeInTheDocument()
    expect(within(tabla).getByText('Sugerido')).toBeInTheDocument()
  })
})
