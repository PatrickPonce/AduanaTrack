import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as liquidacionApi from '../api/liquidacion'
import type { Liquidacion, LiquidacionConsulta } from '../types'
import { hoyIso } from '../utils/formato'
import { LiquidacionDespacho, validarLiquidacion, validarPago } from './LiquidacionDespacho'

vi.mock('../api/liquidacion')

/** Caso calculado a mano del contrato C3: una partida al 6 %. */
const LIQUIDACION: Liquidacion = {
  fob: 10000,
  flete: 800,
  seguro: 150,
  cif: 10950,
  adValorem: 657,
  igv: 1857.12,
  ipm: 232.14,
  percepcion: 479.37,
  total: 3225.63,
  moneda: 'USD',
  tasas: { igv: 16, ipm: 2, percepcion: 3.5 },
  estado: 'LIQUIDADO',
}

const SIN_GUARDAR: LiquidacionConsulta = { liquidacion: null, generadoPor: null, generadoEn: null, pago: null }

const GUARDADA: LiquidacionConsulta = {
  liquidacion: { ...LIQUIDACION, estado: 'TRIBUTOS_PAGADOS' },
  generadoPor: 'Agente Demo',
  generadoEn: '2026-10-13T09:05:00',
  pago: {
    fecha: '2026-10-14',
    monto: 3225.63,
    medioPago: 'VENTANILLA',
    numeroOperacion: 'OP-000145',
    registradoPor: 'Agente Demo',
    registradoEn: '2026-10-14T15:05:00',
  },
}

function errorApi(status: number, data: Record<string, unknown>) {
  return new AxiosError(String(status), String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  })
}

async function generar(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^Flete \(USD\)/), '800.00')
  await user.type(screen.getByLabelText(/^Seguro \(USD\)/), '150.00')
  await user.click(screen.getByRole('button', { name: 'Generar liquidación' }))
}

async function elegirMedioPago(user: ReturnType<typeof userEvent.setup>, texto: string) {
  await user.click(screen.getByRole('combobox', { name: /Medio de pago/ }))
  await user.click(await screen.findByRole('option', { name: texto }))
}

describe('LiquidacionDespacho (HU-08)', () => {
  beforeEach(() => {
    vi.mocked(liquidacionApi.obtenerLiquidacion).mockResolvedValue(SIN_GUARDAR)
    vi.mocked(liquidacionApi.generarLiquidacion).mockResolvedValue(LIQUIDACION)
    vi.mocked(liquidacionApi.registrarPago).mockResolvedValue({ estado: 'TRIBUTOS_PAGADOS' })
  })

  it('CA1: «Generar liquidación» envía flete y seguro y muestra el valor CIF (FOB + flete + seguro)', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    await generar(user)

    expect(liquidacionApi.generarLiquidacion).toHaveBeenCalledWith(7, { flete: 800, seguro: 150 })
    expect(await screen.findByLabelText('Valor FOB')).toHaveTextContent('10,000.00')
    expect(screen.getByLabelText('Flete')).toHaveTextContent('800.00')
    expect(screen.getByLabelText('Seguro')).toHaveTextContent('150.00')
    expect(screen.getByLabelText('Valor CIF')).toHaveTextContent('10,950.00')
  })

  it('CA2: muestra Ad Valorem, IGV, IPM y percepción con las tasas configuradas, y el total', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    await generar(user)

    expect(await screen.findByLabelText('Ad Valorem')).toHaveTextContent('657.00')
    expect(screen.getByLabelText('IGV (16 %)')).toHaveTextContent('1,857.12')
    expect(screen.getByLabelText('IPM (2 %)')).toHaveTextContent('232.14')
    expect(screen.getByLabelText('Percepción (3.5 %)')).toHaveTextContent('479.37')
    expect(screen.getByLabelText('Total de tributos')).toHaveTextContent('3,225.63')
  })

  it('avisa que el cálculo es referencial y que el pago se hace fuera del sistema', () => {
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    expect(screen.getByText(/Cálculo referencial de la agencia/)).toBeInTheDocument()
  })

  it('no envía la liquidación si falta el flete o el seguro', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    await user.click(screen.getByRole('button', { name: 'Generar liquidación' }))

    expect(await screen.findByText('Ingrese el flete (USD)')).toBeInTheDocument()
    expect(screen.getByText('Ingrese el seguro (USD)')).toBeInTheDocument()
    expect(liquidacionApi.generarLiquidacion).not.toHaveBeenCalled()
  })

  it('marca los campos que el API rechaza con 400', async () => {
    vi.mocked(liquidacionApi.generarLiquidacion).mockRejectedValue(
      errorApi(400, { codigo: 'VALIDACION', mensaje: 'Datos no válidos', campos: { flete: 'El flete no puede ser negativo' } }),
    )
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    await generar(user)

    expect(await screen.findByText('El flete no puede ser negativo')).toBeInTheDocument()
  })

  it('muestra el mensaje del API si el despacho no está clasificado (409)', async () => {
    vi.mocked(liquidacionApi.generarLiquidacion).mockRejectedValue(
      errorApi(409, { codigo: 'DESPACHO_NO_CLASIFICADO', mensaje: 'Clasifique todos los ítems del despacho.' }),
    )
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    await generar(user)

    expect(await screen.findByText('Clasifique todos los ítems del despacho.')).toBeInTheDocument()
  })

  it('al generar la liquidación avisa al detalle del despacho y ofrece el registro de pago', async () => {
    const alCambiar = vi.fn()
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar onEstadoCambiado={alCambiar} />)
    expect(screen.queryByRole('button', { name: 'Registrar pago realizado' })).not.toBeInTheDocument()

    await generar(user)

    expect(await screen.findByRole('button', { name: 'Registrar pago realizado' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^Monto pagado/)).toHaveValue('3225.63')
    expect(alCambiar).toHaveBeenCalledTimes(1)
  })

  it('CA3: «Registrar pago realizado» envía fecha, monto, medio de pago y número de operación', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar />)

    await user.type(screen.getByLabelText(/^Monto pagado/), '3225.63')
    await elegirMedioPago(user, 'Banca por convenio')
    await user.type(screen.getByLabelText(/^N° de operación/), '00123456')
    await user.click(screen.getByRole('button', { name: 'Registrar pago realizado' }))

    expect(await screen.findByText(/Pago de tributos registrado/)).toBeInTheDocument()
    expect(liquidacionApi.registrarPago).toHaveBeenCalledWith(7, {
      fecha: hoyIso(),
      monto: 3225.63,
      medioPago: 'BANCA_POR_CONVENIO',
      numeroOperacion: '00123456',
    })
  })

  it('CA4: con el pago registrado el despacho pasa a «Tributos pagados» y ya no se puede volver a pagar', async () => {
    const alCambiar = vi.fn()
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar onEstadoCambiado={alCambiar} />)

    await user.type(screen.getByLabelText(/^Monto pagado/), '3225.63')
    await elegirMedioPago(user, 'Ventanilla')
    await user.type(screen.getByLabelText(/^N° de operación/), '991')
    await user.click(screen.getByRole('button', { name: 'Registrar pago realizado' }))

    expect(await screen.findByText(/pasó a «Tributos pagados»/)).toBeInTheDocument()
    expect(alCambiar).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Registrar pago realizado' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /generar liquidación/i })).not.toBeInTheDocument()
  })

  it('no envía el pago si faltan datos y marca cada campo', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar />)

    await user.click(screen.getByRole('button', { name: 'Registrar pago realizado' }))

    expect(await screen.findByText('Ingrese el monto pagado')).toBeInTheDocument()
    expect(screen.getByText('Seleccione el medio de pago')).toBeInTheDocument()
    expect(screen.getByText('Ingrese el número de operación')).toBeInTheDocument()
    expect(liquidacionApi.registrarPago).not.toHaveBeenCalled()
  })

  it('rechaza una fecha de pago futura antes de enviar', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar />)

    fireEvent.change(screen.getByLabelText(/^Fecha del pago/), { target: { value: '2999-01-01' } })
    await user.click(screen.getByRole('button', { name: 'Registrar pago realizado' }))

    expect(await screen.findByText('La fecha del pago no puede ser futura')).toBeInTheDocument()
    expect(liquidacionApi.registrarPago).not.toHaveBeenCalled()
  })

  it('muestra el mensaje del API si se intenta pagar sin liquidación (409)', async () => {
    vi.mocked(liquidacionApi.registrarPago).mockRejectedValue(
      errorApi(409, { codigo: 'LIQUIDACION_PENDIENTE', mensaje: 'Genere la liquidación antes de registrar el pago.' }),
    )
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar />)

    await user.type(screen.getByLabelText(/^Monto pagado/), '10.00')
    await elegirMedioPago(user, 'Ventanilla')
    await user.type(screen.getByLabelText(/^N° de operación/), '1')
    await user.click(screen.getByRole('button', { name: 'Registrar pago realizado' }))

    expect(await screen.findByText('Genere la liquidación antes de registrar el pago.')).toBeInTheDocument()
  })

  it('con la liquidación ya generada muestra el detalle guardado y deja el flete y el seguro listos para regenerar', async () => {
    vi.mocked(liquidacionApi.obtenerLiquidacion).mockResolvedValue({
      ...GUARDADA,
      liquidacion: LIQUIDACION,
      pago: null,
    })
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar />)

    expect(await screen.findByLabelText('Total de tributos')).toHaveTextContent('3,225.63')
    expect(screen.getByText(/Generada por Agente Demo/)).toBeInTheDocument()
    expect(screen.getByLabelText(/^Flete \(USD\)/)).toHaveValue('800.00')
    expect(screen.getByLabelText(/^Monto pagado/)).toHaveValue('3225.63')
    expect(screen.getByRole('button', { name: 'Volver a generar liquidación' })).toBeInTheDocument()
  })

  it('con los tributos pagados muestra la liquidación y el pago registrado, sin formularios', async () => {
    vi.mocked(liquidacionApi.obtenerLiquidacion).mockResolvedValue(GUARDADA)
    render(<LiquidacionDespacho despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)

    expect(screen.getByText(/Los tributos ya fueron pagados/)).toBeInTheDocument()
    expect(await screen.findByLabelText('Valor CIF')).toHaveTextContent('10,950.00')
    const pago = screen.getByRole('table', { name: 'Pago registrado' })
    expect(within(pago).getByLabelText('Medio de pago')).toHaveTextContent('Ventanilla')
    expect(within(pago).getByLabelText('N° de operación')).toHaveTextContent('OP-000145')
    expect(within(pago).getByLabelText('Fecha del pago')).toHaveTextContent('14/10/2026')
    expect(screen.queryByRole('button', { name: /liquidación/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar pago realizado' })).not.toBeInTheDocument()
  })

  it('el perfil Cliente solo consulta: no ve formularios', () => {
    render(<LiquidacionDespacho despachoId={7} estado="LIQUIDADO" puedeEditar={false} />)

    expect(screen.getByText(/Solo el agente de aduana puede generar la liquidación/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /generar liquidación/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar pago realizado' })).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('el detalle se muestra en una tabla accesible', async () => {
    const user = userEvent.setup()
    render(<LiquidacionDespacho despachoId={7} estado="CLASIFICADO" puedeEditar />)

    await generar(user)

    const tabla = await screen.findByRole('table', { name: 'Detalle de la liquidación' })
    expect(within(tabla).getAllByRole('row')).toHaveLength(9)
  })
})

describe('validarLiquidacion', () => {
  it('acepta cero (sin flete o sin seguro) y la coma decimal', () => {
    expect(validarLiquidacion('0', '0.00')).toEqual({})
    expect(validarLiquidacion('800,50', '150')).toEqual({})
  })

  it('exige ambos montos y máximo 2 decimales', () => {
    expect(validarLiquidacion('', '10.123')).toEqual({
      flete: 'Ingrese el flete (USD)',
      seguro: 'Seguro no válido (USD, máximo 2 decimales)',
    })
  })

  it('rechaza negativos', () => {
    expect(validarLiquidacion('-1', '0')).toEqual({ flete: 'Flete no válido (USD, máximo 2 decimales)' })
  })
})

describe('validarPago', () => {
  const valido = { fecha: '2026-10-28', monto: '3225.63', medioPago: 'VENTANILLA', numeroOperacion: '00123456' }

  it('acepta un pago completo', () => {
    expect(validarPago(valido, '2026-10-28')).toEqual({})
  })

  it('aplica las reglas del contrato C3', () => {
    expect(validarPago({ fecha: '2026-10-29', monto: '0', medioPago: '', numeroOperacion: ' ' }, '2026-10-28')).toEqual({
      fecha: 'La fecha del pago no puede ser futura',
      monto: 'El monto debe ser mayor a 0',
      medioPago: 'Seleccione el medio de pago',
      numeroOperacion: 'Ingrese el número de operación',
    })
  })

  it('limita el número de operación a 50 caracteres', () => {
    expect(validarPago({ ...valido, numeroOperacion: 'x'.repeat(51) }, '2026-10-28')).toEqual({
      numeroOperacion: 'Máximo 50 caracteres',
    })
  })
})
