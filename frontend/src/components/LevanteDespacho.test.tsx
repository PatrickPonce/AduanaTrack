import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as levanteApi from '../api/levante'
import type { Levante } from '../api/levante'
import { hoyIso, sumarDias } from '../utils/formato'
import { LevanteDespacho, validarLevante } from './LevanteDespacho'

vi.mock('../api/levante')

const SIN_LEVANTE: Levante = {
  numeroLevante: null,
  fechaLevante: null,
  constanciaNombre: null,
  conAdvertencia: false,
  retiroHabilitado: false,
  estadoDespacho: 'INSPECCION_CONFORME',
}

/** Respuesta de ejemplo del contrato C8. */
const OTORGADO: Levante = {
  numeroLevante: 'LEV-2026-000318',
  fechaLevante: '2026-11-10',
  constanciaNombre: 'constancia.pdf',
  conAdvertencia: false,
  retiroHabilitado: true,
  estadoDespacho: 'LEVANTE_OTORGADO',
}

function pdf(nombre = 'constancia.pdf', bytes = 1024) {
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

const PENDIENTES = errorApi(409, {
  codigo: 'LEVANTE_REQUISITOS_PENDIENTES',
  mensaje: 'Faltan requisitos previos al levante: inspección conforme.',
  pendientes: ['Inspección conforme'],
})

function mostrar(props: Partial<React.ComponentProps<typeof LevanteDespacho>> = {}) {
  return render(<LevanteDespacho despachoId={7} estado="INSPECCION_CONFORME" puedeEditar {...props} />)
}

const REGISTRAR = 'Registrar levante'

async function escribirNumero(user: ReturnType<typeof userEvent.setup>, numero = 'LEV-2026-000318') {
  await user.type(screen.getByLabelText(/N° de levante \/ código de autorización SUNAT/), numero)
}

describe('LevanteDespacho (HU-13 · T-13.3 y T-13.4)', () => {
  beforeEach(() => {
    vi.mocked(levanteApi.obtenerLevante).mockResolvedValue(SIN_LEVANTE)
    vi.mocked(levanteApi.registrarLevante).mockResolvedValue(OTORGADO)
    vi.mocked(levanteApi.urlConstanciaLevante).mockReturnValue('/api/v1/despachos/7/levante/constancia')
  })

  it('CA1: con número y fecha registra el levante y avisa que el despacho pasó a «Levante otorgado»', async () => {
    const alRegistrar = vi.fn()
    const user = userEvent.setup()
    mostrar({ onLevanteRegistrado: alRegistrar })
    await screen.findByRole('button', { name: REGISTRAR })

    expect(screen.getByLabelText(/Fecha del levante/)).toHaveValue(hoyIso())
    await escribirNumero(user)
    fireEvent.change(screen.getByLabelText(/Fecha del levante/), { target: { value: '2026-10-30' } })
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(levanteApi.registrarLevante).toHaveBeenCalledWith(7, {
      numeroLevante: 'LEV-2026-000318',
      fechaLevante: '2026-10-30',
      confirmarSinRequisitos: false,
      constancia: null,
    })
    expect(await screen.findByText(/El despacho pasó a «Levante otorgado»/)).toBeInTheDocument()
    const lectura = screen.getByLabelText('Levante registrado')
    expect(within(lectura).getByText('LEV-2026-000318')).toBeInTheDocument()
    expect(within(lectura).getByText('10/11/2026')).toBeInTheDocument()
    expect(within(lectura).getByText('Cumplidos')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
    expect(alRegistrar).toHaveBeenCalledTimes(1)
  })

  it('CA1: sin número o con fecha futura no se envía', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    fireEvent.change(screen.getByLabelText(/Fecha del levante/), { target: { value: sumarDias(hoyIso(), 1) } })
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Ingrese el N° de levante')).toBeInTheDocument()
    expect(screen.getByText('La fecha del levante no puede ser futura')).toBeInTheDocument()
    expect(levanteApi.registrarLevante).not.toHaveBeenCalled()
  })

  it('CA2: sin inspección conforme pregunta con los pendientes y, al confirmar, guarda con advertencia', async () => {
    vi.mocked(levanteApi.registrarLevante)
      .mockRejectedValueOnce(PENDIENTES)
      .mockResolvedValueOnce({ ...OTORGADO, conAdvertencia: true })
    const user = userEvent.setup()
    mostrar({ estado: 'CANAL_ASIGNADO' })
    await screen.findByRole('button', { name: REGISTRAR })

    await escribirNumero(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    const dialogo = await screen.findByRole('dialog', { name: 'Requisitos previos pendientes' })
    expect(within(within(dialogo).getByRole('list', { name: 'Requisitos pendientes' })).getByText('Inspección conforme'))
      .toBeInTheDocument()

    await user.click(within(dialogo).getByRole('button', { name: 'Registrar de todas formas' }))

    expect(levanteApi.registrarLevante).toHaveBeenLastCalledWith(7, expect.objectContaining({ confirmarSinRequisitos: true }))
    expect(await screen.findByText(/Levante registrado con advertencia/)).toBeInTheDocument()
    expect(within(screen.getByLabelText('Levante registrado')).getByText('Registrado con advertencia')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('CA2: al cancelar la advertencia no se registra el levante', async () => {
    vi.mocked(levanteApi.registrarLevante).mockRejectedValue(PENDIENTES)
    const user = userEvent.setup()
    mostrar({ estado: 'CANAL_ASIGNADO' })
    await screen.findByRole('button', { name: REGISTRAR })

    await escribirNumero(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))
    const dialogo = await screen.findByRole('dialog', { name: 'Requisitos previos pendientes' })
    await user.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(levanteApi.registrarLevante).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: REGISTRAR })).toBeInTheDocument()
  })

  it('CA3: adjunta la constancia en PDF y deja el enlace de descarga', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })
    const constancia = pdf()

    await escribirNumero(user)
    await user.upload(screen.getByLabelText('Archivo de la constancia de levante'), constancia)
    expect(screen.getByText(/constancia\.pdf · 1(\.0)? KB/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(levanteApi.registrarLevante).toHaveBeenCalledWith(7, expect.objectContaining({ constancia }))
    const enlace = await screen.findByRole('link', { name: /Descargar constancia · constancia\.pdf/ })
    expect(enlace).toHaveAttribute('href', '/api/v1/despachos/7/levante/constancia')
  })

  it('CA3: una constancia de 11 MB o que no es PDF se rechaza antes de enviar', async () => {
    const user = userEvent.setup({ applyAccept: false })
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.upload(screen.getByLabelText('Archivo de la constancia de levante'), pdf('grande.pdf', 11 * 1024 * 1024))
    expect(screen.getByText('El archivo supera el tamaño máximo de 10 MB')).toBeInTheDocument()

    await user.upload(
      screen.getByLabelText('Archivo de la constancia de levante'),
      new File(['x'], 'constancia.docx', { type: 'application/msword' }),
    )
    expect(screen.getByText('Solo se permiten archivos PDF')).toBeInTheDocument()

    await escribirNumero(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))
    expect(levanteApi.registrarLevante).toHaveBeenCalledWith(7, expect.objectContaining({ constancia: null }))
  })

  it('CA4: con el levante otorgado ofrece «Registrar cita de retiro»', async () => {
    vi.mocked(levanteApi.obtenerLevante).mockResolvedValue(OTORGADO)
    const alRetiro = vi.fn()
    const user = userEvent.setup()
    mostrar({ estado: 'LEVANTE_OTORGADO', onRegistrarRetiro: alRetiro })

    await user.click(await screen.findByRole('button', { name: 'Registrar cita de retiro' }))

    expect(alRetiro).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
  })

  it('CA4: sin levante todavía no hay acceso a la cita de retiro', async () => {
    mostrar({ onRegistrarRetiro: vi.fn() })

    await screen.findByRole('button', { name: REGISTRAR })
    expect(screen.queryByRole('button', { name: 'Registrar cita de retiro' })).not.toBeInTheDocument()
  })

  it('un número ya usado en otro despacho muestra el 409 del API', async () => {
    vi.mocked(levanteApi.registrarLevante).mockRejectedValue(
      errorApi(409, { codigo: 'LEVANTE_DUPLICADO', mensaje: 'Ese N° de levante ya está registrado en otro despacho.' }),
    )
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await escribirNumero(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Ese N° de levante ya está registrado en otro despacho.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('una constancia que el API rechaza por tamaño (413) muestra el mensaje de la constancia', async () => {
    vi.mocked(levanteApi.registrarLevante).mockRejectedValue(errorApi(413, { codigo: 'ARCHIVO_MUY_GRANDE' }))
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await escribirNumero(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('La constancia supera el tamaño máximo de 10 MB.')).toBeInTheDocument()
  })

  it('el perfil Cliente solo consulta: sin formulario ni acceso a la cita de retiro', async () => {
    vi.mocked(levanteApi.obtenerLevante).mockResolvedValue(OTORGADO)
    mostrar({ estado: 'LEVANTE_OTORGADO', puedeEditar: false, onRegistrarRetiro: vi.fn() })

    expect(await screen.findByLabelText('Levante registrado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar cita de retiro' })).not.toBeInTheDocument()
  })

  it('antes del canal de control explica cuándo se registra el levante', async () => {
    mostrar({ estado: 'DAM_NUMERADA' })

    expect(await screen.findByText(/cuando el despacho tiene el canal de control asignado/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
  })
})

describe('validarLevante', () => {
  it('pide número y fecha, limita el largo y el formato, y no acepta fechas futuras', () => {
    expect(validarLevante({ numeroLevante: 'LEV-2026/318', fechaLevante: '2026-11-10' }, '2026-11-10')).toEqual({})
    expect(validarLevante({ numeroLevante: '', fechaLevante: '' }, '2026-11-10')).toEqual({
      numeroLevante: 'Ingrese el N° de levante',
      fechaLevante: 'Ingrese la fecha del levante',
    })
    expect(validarLevante({ numeroLevante: 'LEV 318', fechaLevante: '2026-11-11' }, '2026-11-10')).toEqual({
      numeroLevante: 'Use solo letras, números, guiones y barras',
      fechaLevante: 'La fecha del levante no puede ser futura',
    })
    expect(validarLevante({ numeroLevante: 'L'.repeat(31), fechaLevante: '2026-11-10' }, '2026-11-10')).toEqual({
      numeroLevante: 'Máximo 30 caracteres',
    })
  })
})
