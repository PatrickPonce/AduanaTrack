import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as damApi from '../api/dam'
import type { Dam, RectificacionDam } from '../types'
import { hoyIso } from '../utils/formato'
import { NumeracionDam, validarDam } from './NumeracionDam'

vi.mock('../api/dam')

const SIN_DAM: Dam = { numeroDam: null, fechaNumeracion: null, nombreCargo: null, estado: 'TRIBUTOS_PAGADOS' }

/** Ejemplo del contrato C4. */
const REGISTRADA: Dam = {
  numeroDam: '118-2026-10-123456',
  fechaNumeracion: '2026-10-26',
  nombreCargo: 'Cargo DAM.pdf',
  estado: 'DAM_NUMERADA',
}

const RECTIFICACION: RectificacionDam = {
  numeroAnterior: '118-2026-10-123456',
  fechaAnterior: '2026-10-26',
  numeroNuevo: '118-2026-10-654321',
  fechaNueva: '2026-10-26',
  motivo: 'Error de digitación',
  usuario: 'Agente Demo',
  fecha: '2026-10-28T10:00:00',
}

function pdf(nombre = 'cargo.pdf', bytes = 1024) {
  return new File([new Uint8Array(bytes)], nombre, { type: 'application/pdf' })
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

async function escribirNumero(user: ReturnType<typeof userEvent.setup>, numero: string) {
  await user.type(screen.getByLabelText(/^N° de DAM/), numero)
}

describe('NumeracionDam (HU-09)', () => {
  beforeEach(() => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue(SIN_DAM)
    vi.mocked(damApi.listarRectificacionesDam).mockResolvedValue([])
    vi.mocked(damApi.registrarDam).mockResolvedValue(REGISTRADA)
    vi.mocked(damApi.rectificarDam).mockResolvedValue({ ...REGISTRADA, numeroDam: '118-2026-10-654321' })
    vi.mocked(damApi.urlCargoDam).mockReturnValue('/api/v1/despachos/7/dam/cargo')
  })

  it('CA1: guarda el número de DAM junto con la fecha de numeración', async () => {
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118-2026-10-123456')
    fireEvent.change(screen.getByLabelText(/^Fecha de numeración/), { target: { value: '2026-10-26' } })
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(damApi.registrarDam).toHaveBeenCalledWith(7, {
      numeroDam: '118-2026-10-123456',
      fechaNumeracion: '2026-10-26',
      cargo: null,
    })
    const registrada = await screen.findByLabelText('DAM registrada')
    expect(within(registrada).getByText('118-2026-10-123456')).toBeInTheDocument()
    expect(within(registrada).getByText('26/10/2026')).toBeInTheDocument()
  })

  it('CA1: la fecha de numeración propone la de hoy', async () => {
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)

    expect(await screen.findByLabelText(/^Fecha de numeración/)).toHaveValue(hoyIso())
  })

  it('CA2: con el N° de DAM vacío no guarda y pide completar el dato', async () => {
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)

    await user.click(await screen.findByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(await screen.findByText('Ingrese el N° de DAM')).toBeInTheDocument()
    expect(damApi.registrarDam).not.toHaveBeenCalled()
  })

  it('CA3: al registrar, el despacho pasa a «DAM numerada» y se avisa al detalle del despacho', async () => {
    const alNumerar = vi.fn()
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar onDamNumerada={alNumerar} />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118-2026-10-123456')
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(await screen.findByText(/pasó a «DAM numerada»/)).toBeInTheDocument()
    expect(alNumerar).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Registrar numeración de DAM' })).not.toBeInTheDocument()
  })

  it('CA4: el cargo adjunto en PDF se envía junto con el registro y queda disponible para descargar', async () => {
    const cargo = pdf('Cargo DAM.pdf')
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118-2026-10-123456')
    await user.upload(screen.getByLabelText('Archivo del cargo de numeración'), cargo)
    expect(screen.getByText(/Cargo DAM\.pdf · 1\.0 KB/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(damApi.registrarDam).toHaveBeenCalledWith(7, {
      numeroDam: '118-2026-10-123456',
      fechaNumeracion: hoyIso(),
      cargo,
    })
    const enlace = await screen.findByRole('link', { name: /Descargar cargo · Cargo DAM\.pdf/ })
    expect(enlace).toHaveAttribute('href', '/api/v1/despachos/7/dam/cargo')
  })

  it('rechaza un cargo que no es PDF antes de enviarlo', async () => {
    // El navegador ya filtra por `accept`; aquí se prueba la revisión propia cuando el archivo llega igual.
    const user = userEvent.setup({ applyAccept: false })
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)

    await user.upload(
      await screen.findByLabelText('Archivo del cargo de numeración'),
      new File(['x'], 'cargo.docx', { type: 'application/msword' }),
    )

    expect(await screen.findByText('Solo se permiten archivos PDF')).toBeInTheDocument()
  })

  it('rechaza un cargo de más de 10 MB antes de enviarlo', async () => {
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)

    await user.upload(
      await screen.findByLabelText('Archivo del cargo de numeración'),
      pdf('grande.pdf', 11 * 1024 * 1024),
    )

    expect(await screen.findByText('El archivo supera el tamaño máximo de 10 MB')).toBeInTheDocument()
  })

  it('valida el formato del número y la fecha antes de enviar', async () => {
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118 2026 #1')
    fireEvent.change(screen.getByLabelText(/^Fecha de numeración/), { target: { value: '2999-01-01' } })
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(await screen.findByText('Use solo letras, números, guiones y barras')).toBeInTheDocument()
    expect(screen.getByText('La fecha de numeración no puede ser futura')).toBeInTheDocument()
    expect(damApi.registrarDam).not.toHaveBeenCalled()
  })

  it('muestra el mensaje del API si la DAM ya está en otro despacho (409 DAM_DUPLICADA)', async () => {
    vi.mocked(damApi.registrarDam).mockRejectedValue(
      errorApi(409, { codigo: 'DAM_DUPLICADA', mensaje: 'Ese N° de DAM ya está registrado en otro despacho.' }),
    )
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118-2026-10-123456')
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(await screen.findByText('Ese N° de DAM ya está registrado en otro despacho.')).toBeInTheDocument()
  })

  it('marca el campo que el API rechaza con 400', async () => {
    vi.mocked(damApi.registrarDam).mockRejectedValue(
      errorApi(400, { codigo: 'VALIDACION', mensaje: 'Datos no válidos', campos: { numeroDam: 'Máximo 30 caracteres' } }),
    )
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118-2026-10-123456')
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(await screen.findByText('Máximo 30 caracteres')).toBeInTheDocument()
  })

  it('traduce el 415 del cargo a un mensaje claro', async () => {
    vi.mocked(damApi.registrarDam).mockRejectedValue(errorApi(415, { codigo: 'FORMATO_NO_PERMITIDO', mensaje: 'x' }))
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)
    await screen.findByRole('button', { name: 'Registrar numeración de DAM' })

    await escribirNumero(user, '118-2026-10-123456')
    await user.click(screen.getByRole('button', { name: 'Registrar numeración de DAM' }))

    expect(await screen.findByText('El cargo no es un PDF válido.')).toBeInTheDocument()
  })

  it('con la DAM registrada muestra sus datos, sin formulario de registro y con «Sin cargo adjunto»', async () => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue({ ...REGISTRADA, nombreCargo: null })
    render(<NumeracionDam despachoId={7} estado="DAM_NUMERADA" puedeEditar />)

    const registrada = await screen.findByLabelText('DAM registrada')
    expect(within(registrada).getByText('118-2026-10-123456')).toBeInTheDocument()
    expect(within(registrada).getByText('Sin cargo adjunto')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar numeración de DAM' })).not.toBeInTheDocument()
    expect(screen.getByText('El número de DAM no se rectificó.')).toBeInTheDocument()
  })

  it('rectificar: corrige el número con un motivo y lo deja en el historial', async () => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue(REGISTRADA)
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="DAM_NUMERADA" puedeEditar />)

    await user.click(await screen.findByRole('button', { name: 'Rectificar DAM' }))
    expect(screen.getByLabelText(/^N° de DAM/)).toHaveValue('118-2026-10-123456')
    await user.clear(screen.getByLabelText(/^N° de DAM/))
    await user.type(screen.getByLabelText(/^N° de DAM/), '118-2026-10-654321')
    await user.type(screen.getByLabelText(/^Motivo de la rectificación/), 'Error de digitación')
    vi.mocked(damApi.listarRectificacionesDam).mockResolvedValue([RECTIFICACION])
    await user.click(screen.getByRole('button', { name: 'Guardar rectificación' }))

    expect(damApi.rectificarDam).toHaveBeenCalledWith(7, {
      numeroDam: '118-2026-10-654321',
      fechaNumeracion: '2026-10-26',
      cargo: null,
      motivo: 'Error de digitación',
    })
    expect(await screen.findByText(/DAM rectificada/)).toBeInTheDocument()
    const tabla = await screen.findByRole('table', { name: 'Historial de rectificaciones de la DAM' })
    expect(within(tabla).getByText('Error de digitación')).toBeInTheDocument()
    expect(within(tabla).getByText('118-2026-10-654321')).toBeInTheDocument()
    expect(within(tabla).getByText('Agente Demo')).toBeInTheDocument()
    expect(within(tabla).getByText('28/10/2026 10:00')).toBeInTheDocument()
  })

  it('rectificar: exige el motivo y no envía sin él', async () => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue(REGISTRADA)
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="DAM_NUMERADA" puedeEditar />)

    await user.click(await screen.findByRole('button', { name: 'Rectificar DAM' }))
    await user.click(screen.getByRole('button', { name: 'Guardar rectificación' }))

    expect(await screen.findByText('Indique el motivo de la rectificación')).toBeInTheDocument()
    expect(damApi.rectificarDam).not.toHaveBeenCalled()
  })

  it('rectificar: «Cancelar» cierra el formulario sin enviar', async () => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue(REGISTRADA)
    const user = userEvent.setup()
    render(<NumeracionDam despachoId={7} estado="DAM_NUMERADA" puedeEditar />)

    await user.click(await screen.findByRole('button', { name: 'Rectificar DAM' }))
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('button', { name: 'Guardar rectificación' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rectificar DAM' })).toBeInTheDocument()
    expect(damApi.rectificarDam).not.toHaveBeenCalled()
  })

  it('muestra el historial de rectificaciones que ya tiene el despacho', async () => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue({ ...REGISTRADA, numeroDam: '118-2026-10-654321' })
    vi.mocked(damApi.listarRectificacionesDam).mockResolvedValue([
      RECTIFICACION,
      { ...RECTIFICACION, fechaNueva: '2026-10-27', motivo: 'Fecha mal digitada', fecha: '2026-10-28T11:00:00' },
    ])
    render(<NumeracionDam despachoId={7} estado="DAM_NUMERADA" puedeEditar={false} />)

    const tabla = await screen.findByRole('table', { name: 'Historial de rectificaciones de la DAM' })
    expect(within(tabla).getAllByRole('row')).toHaveLength(3)
    expect(within(tabla).getByText('26/10/2026 → 27/10/2026')).toBeInTheDocument()
  })

  it('el perfil Cliente consulta la DAM pero no puede registrar ni rectificar', async () => {
    vi.mocked(damApi.obtenerDam).mockResolvedValue(REGISTRADA)
    render(<NumeracionDam despachoId={7} estado="DAM_NUMERADA" puedeEditar={false} />)

    expect(await screen.findByLabelText('DAM registrada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Rectificar DAM' })).not.toBeInTheDocument()
  })

  it('el perfil Cliente ve un aviso si todavía no hay DAM', async () => {
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar={false} />)

    expect(await screen.findByText('Todavía no se registró la numeración de la DAM.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar numeración de DAM' })).not.toBeInTheDocument()
  })

  it('avisa si no se pudo cargar la numeración', async () => {
    vi.mocked(damApi.obtenerDam).mockRejectedValue(errorApi(500, { codigo: 'ERROR_INTERNO', mensaje: 'Error interno.' }))
    render(<NumeracionDam despachoId={7} estado="TRIBUTOS_PAGADOS" puedeEditar />)

    expect(await screen.findByText('Error interno.')).toBeInTheDocument()
  })
})

describe('validarDam', () => {
  const hoy = '2026-10-28'
  const valido = { numeroDam: '118-2026-10-123456', fechaNumeracion: '2026-10-28', motivo: '' }

  it('acepta un registro válido, con o sin fecha', () => {
    expect(validarDam(valido, hoy, { rectifica: false })).toEqual({})
    expect(validarDam({ ...valido, fechaNumeracion: '' }, hoy, { rectifica: false })).toEqual({})
  })

  it('aplica las reglas del número del contrato C4', () => {
    expect(validarDam({ ...valido, numeroDam: '  ' }, hoy, { rectifica: false })).toEqual({
      numeroDam: 'Ingrese el N° de DAM',
    })
    expect(validarDam({ ...valido, numeroDam: 'A'.repeat(31) }, hoy, { rectifica: false })).toEqual({
      numeroDam: 'Máximo 30 caracteres',
    })
    expect(validarDam({ ...valido, numeroDam: '-118' }, hoy, { rectifica: false })).toEqual({
      numeroDam: 'Use solo letras, números, guiones y barras',
    })
  })

  it('rechaza una fecha de numeración futura', () => {
    expect(validarDam({ ...valido, fechaNumeracion: '2026-10-29' }, hoy, { rectifica: false })).toEqual({
      fechaNumeracion: 'La fecha de numeración no puede ser futura',
    })
  })

  it('al rectificar exige el motivo, de hasta 200 caracteres', () => {
    expect(validarDam(valido, hoy, { rectifica: true })).toEqual({ motivo: 'Indique el motivo de la rectificación' })
    expect(validarDam({ ...valido, motivo: 'x'.repeat(201) }, hoy, { rectifica: true })).toEqual({
      motivo: 'Máximo 200 caracteres',
    })
    expect(validarDam({ ...valido, motivo: 'Error de digitación' }, hoy, { rectifica: true })).toEqual({})
  })
})
