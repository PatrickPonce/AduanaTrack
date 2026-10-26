import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as clasificacionApi from '../api/clasificacion'
import type { ItemFactura } from '../types'
import { ClasificacionDespacho, validarItems, type FilaItem } from './ClasificacionDespacho'

vi.mock('../api/clasificacion')

const RODAMIENTOS = { id: 1, codigoFormateado: '8482.10.00.00', descripcion: 'Rodamientos de bolas' }

/** Ejemplo del contrato C2: el ítem 2 todavía no tiene partida. */
const ITEMS: ItemFactura[] = [
  {
    id: 10,
    numero: 1,
    descripcion: 'Rodamiento 6205',
    cantidad: 200,
    unidad: 'UND',
    valorFob: 4200,
    partidaId: 1,
    partida: RODAMIENTOS,
  },
  {
    id: 11,
    numero: 2,
    descripcion: 'Filtro de aceite',
    cantidad: 150,
    unidad: 'UND',
    valorFob: 5800,
    partidaId: null,
    partida: null,
  },
]

const FILTROS = {
  id: 2,
  codigo: '8421230000',
  codigoFormateado: '8421.23.00.00',
  descripcion: 'Filtros de aceite o combustible',
  tasaAdValorem: 6,
}

function error409(data: Record<string, unknown>) {
  return new AxiosError('409', '409', undefined, undefined, {
    status: 409,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  })
}

async function asignarFiltros(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Partida arancelaria del ítem 2'), 'filtro')
  await user.click(await screen.findByRole('option', { name: /8421\.23\.00\.00/ }))
}

describe('ClasificacionDespacho (HU-07)', () => {
  beforeEach(() => {
    vi.mocked(clasificacionApi.listarItems).mockResolvedValue(ITEMS)
    vi.mocked(clasificacionApi.buscarPartidas).mockResolvedValue([FILTROS])
  })

  it('muestra los ítems con su partida y el valor FOB total', async () => {
    render(<ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar />)

    expect(await screen.findByLabelText('Descripción del ítem 1')).toHaveValue('Rodamiento 6205')
    expect(screen.getByLabelText('Partida arancelaria del ítem 1')).toHaveValue('8482.10.00.00 · Rodamientos de bolas')
    expect(screen.getByLabelText('Partida arancelaria del ítem 2')).toHaveValue('')
    expect(screen.getByLabelText('Valor FOB total')).toHaveTextContent('10,000.00')
  })

  it('CA1: el buscador consulta el catálogo con lo que se escribe y muestra la tasa Ad Valorem', async () => {
    const user = userEvent.setup()
    render(<ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar />)
    await screen.findByLabelText('Partida arancelaria del ítem 2')

    await user.type(screen.getByLabelText('Partida arancelaria del ítem 2'), 'FILTRO')

    expect(await screen.findByRole('option', { name: /Filtros de aceite o combustible · Ad Valorem 6 %/ })).toBeVisible()
    expect(clasificacionApi.buscarPartidas).toHaveBeenLastCalledWith('FILTRO')
  })

  it('CA3: con un ítem sin partida el botón está deshabilitado y avisa cuántos faltan', async () => {
    render(<ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar />)

    expect(await screen.findByRole('button', { name: 'Guardar clasificación arancelaria' })).toBeDisabled()
    expect(screen.getByRole('status')).toHaveTextContent('Falta asignar la partida a 1 ítem.')
  })

  it('CA2 y CA4: asigna la partida, guarda los ítems y registra la clasificación', async () => {
    vi.mocked(clasificacionApi.guardarItems).mockImplementation(async (_, items) =>
      items.map((item, i) => ({
        ...ITEMS[i]!,
        partidaId: item.partidaId,
        partida: item.partidaId === 2 ? FILTROS : RODAMIENTOS,
      })),
    )
    vi.mocked(clasificacionApi.guardarClasificacion).mockResolvedValue({
      estado: 'CLASIFICADO',
      clasificadoPor: 'Agente Demo',
      clasificadoEn: '2026-10-26T11:30:00',
    })
    const alClasificar = vi.fn()
    const user = userEvent.setup()
    render(
      <ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar onClasificado={alClasificar} />,
    )
    await screen.findByLabelText('Partida arancelaria del ítem 2')

    await asignarFiltros(user)
    await user.click(screen.getByRole('button', { name: 'Guardar clasificación arancelaria' }))

    expect(await screen.findByText(/registrada por Agente Demo el 26\/10\/2026 11:30/)).toBeInTheDocument()
    expect(clasificacionApi.guardarItems).toHaveBeenCalledWith(7, [
      { id: 10, descripcion: 'Rodamiento 6205', cantidad: 200, unidad: 'UND', valorFob: 4200, partidaId: 1 },
      { id: 11, descripcion: 'Filtro de aceite', cantidad: 150, unidad: 'UND', valorFob: 5800, partidaId: 2 },
    ])
    expect(clasificacionApi.guardarClasificacion).toHaveBeenCalledWith(7)
    expect(alClasificar).toHaveBeenCalledTimes(1)
  })

  it('CA3: si el backend responde 409 ITEMS_SIN_PARTIDA nombra los ítems pendientes', async () => {
    vi.mocked(clasificacionApi.guardarItems).mockResolvedValue(ITEMS)
    vi.mocked(clasificacionApi.guardarClasificacion).mockRejectedValue(
      error409({ codigo: 'ITEMS_SIN_PARTIDA', items: [2] }),
    )
    const user = userEvent.setup()
    render(<ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar />)
    await screen.findByLabelText('Partida arancelaria del ítem 2')

    await asignarFiltros(user)
    await user.click(screen.getByRole('button', { name: 'Guardar clasificación arancelaria' }))

    expect(await screen.findByText('Los ítems 2 no tienen partida arancelaria asignada.')).toBeInTheDocument()
  })

  it('con documentos obligatorios pendientes (409 de HU-06) remite a la pestaña Documentos', async () => {
    vi.mocked(clasificacionApi.guardarItems).mockResolvedValue(ITEMS)
    vi.mocked(clasificacionApi.guardarClasificacion).mockRejectedValue(
      error409({ codigo: 'DOCUMENTACION_INCOMPLETA', pendientes: ['POLIZA_SEGURO'] }),
    )
    const user = userEvent.setup()
    render(<ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar />)
    await screen.findByLabelText('Partida arancelaria del ítem 2')

    await asignarFiltros(user)
    await user.click(screen.getByRole('button', { name: 'Guardar clasificación arancelaria' }))

    expect(await screen.findByText(/Faltan documentos obligatorios \(POLIZA_SEGURO\)/)).toBeInTheDocument()
  })

  it('agrega y quita ítems, y no envía filas incompletas', async () => {
    const user = userEvent.setup()
    render(<ClasificacionDespacho despachoId={7} estado="DOCUMENTACION_COMPLETA" puedeEditar />)
    await screen.findByLabelText('Descripción del ítem 2')

    await user.click(screen.getByRole('button', { name: 'Agregar ítem' }))
    await user.click(screen.getByRole('button', { name: 'Guardar ítems' }))

    expect(await screen.findByText('Ingrese la descripción del ítem')).toBeInTheDocument()
    expect(clasificacionApi.guardarItems).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Quitar ítem 3' }))
    await waitFor(() => expect(screen.queryByLabelText('Descripción del ítem 3')).not.toBeInTheDocument())
  })

  it('el perfil Cliente ve los ítems en solo lectura', async () => {
    render(<ClasificacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar={false} />)

    expect(await screen.findByText('Rodamiento 6205')).toBeInTheDocument()
    expect(screen.getByText('8482.10.00.00 · Rodamientos de bolas')).toBeInTheDocument()
    expect(screen.getByText('Sin partida')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar clasificación arancelaria' })).not.toBeInTheDocument()
  })
})

describe('validarItems', () => {
  const fila: FilaItem = { descripcion: 'Rodamiento', cantidad: '200', unidad: 'UND', valorFob: '4200.00', partida: null }

  it('acepta una fila válida aunque todavía no tenga partida', () => {
    expect(validarItems([fila])).toEqual({})
  })

  it('aplica las reglas del contrato C2 con las claves de error del API', () => {
    expect(validarItems([{ ...fila, cantidad: '0', valorFob: '10.555' }])).toEqual({
      'items[0].cantidad': 'Cantidad mayor a 0 (máximo 3 decimales)',
      'items[0].valorFob': 'Valor FOB mayor a 0 (USD, 2 decimales)',
    })
  })
})
