import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as citasApi from '../api/citas'
import type { Cita, Deposito } from '../api/citas'
import { CitasDespacho, aSolicitud, tiposPermitidos, validarCita, type FormCita } from './CitasDespacho'

vi.mock('../api/citas')

/** Depósitos de ejemplo del contrato C6. */
const DEPOSITOS: Deposito[] = [
  { id: 1, codigo: 'DT-01', nombre: 'Depósito temporal 1 (ejemplo)', tipo: 'DEPOSITO_TEMPORAL' },
  { id: 4, codigo: 'AL-01', nombre: 'Almacén 1 (ejemplo)', tipo: 'ALMACEN' },
]

const INSPECCION: Cita = {
  id: 7,
  depositoId: 1,
  deposito: 'Depósito temporal 1 (ejemplo)',
  numeroCita: 'TK-2026-004512',
  tipo: 'INSPECCION',
  fechaHoraConfirmada: '2026-11-12T09:30:00',
  estado: 'PROGRAMADA',
  transportista: null,
  registradaPor: 'Agente Demo',
  registradaEn: '2026-11-04T10:00:00',
  estadoDespacho: 'INSPECCION_PROGRAMADA',
}

const TRANSPORTISTA = {
  ruc: '20123456786',
  razonSocial: 'Transportes Demo SAC',
  placa: 'ABC-123',
  conductor: 'Juan Pérez',
}

const RETIRO: Cita = {
  ...INSPECCION,
  id: 8,
  depositoId: 4,
  deposito: 'Almacén 1 (ejemplo)',
  numeroCita: 'RT-2026-000210',
  tipo: 'RETIRO',
  fechaHoraConfirmada: '2026-11-16T14:00:00',
  transportista: TRANSPORTISTA,
  estadoDespacho: 'RETIRO_PROGRAMADO',
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

function mostrar(props: Partial<React.ComponentProps<typeof CitasDespacho>> = {}) {
  return render(<CitasDespacho despachoId={7} estado="CANAL_ASIGNADO" canal="ROJO" puedeEditar {...props} />)
}

const REGISTRAR = 'Registrar cita'

/** Llena depósito, número y fecha; el tipo queda el que el formulario propone. */
async function llenarDatosBasicos(user: ReturnType<typeof userEvent.setup>, numero = 'TK-2026-004512') {
  await user.click(screen.getByRole('combobox', { name: /Depósito \/ Almacén/ }))
  await user.click(await screen.findByRole('option', { name: 'DT-01 · Depósito temporal 1 (ejemplo)' }))
  await user.type(screen.getByLabelText(/N° de cita \/ ticket/), numero)
  fireEvent.change(screen.getByLabelText(/Fecha y hora confirmada/), { target: { value: '2026-11-12T09:30' } })
}

describe('CitasDespacho (HU-11 · T-11.3 y T-11.4)', () => {
  beforeEach(() => {
    vi.mocked(citasApi.listarDepositos).mockResolvedValue(DEPOSITOS)
    vi.mocked(citasApi.listarCitas).mockResolvedValue([])
    vi.mocked(citasApi.registrarCita).mockResolvedValue(INSPECCION)
  })

  it('CA1: una cita con depósito y número queda registrada para el despacho', async () => {
    const alRegistrar = vi.fn()
    const user = userEvent.setup()
    mostrar({ onCitaRegistrada: alRegistrar })
    await screen.findByRole('button', { name: REGISTRAR })
    vi.mocked(citasApi.listarCitas).mockResolvedValue([INSPECCION])

    await llenarDatosBasicos(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(citasApi.registrarCita).toHaveBeenCalledWith(7, {
      depositoId: 1,
      numeroCita: 'TK-2026-004512',
      tipo: 'INSPECCION',
      fechaHoraConfirmada: '2026-11-12T09:30:00',
      transportista: null,
    })
    expect(await screen.findByText(/Cita de inspección TK-2026-004512 registrada para el 12\/11\/2026 09:30/)).toBeInTheDocument()
    expect(alRegistrar).toHaveBeenCalledTimes(1)
    // El formulario queda limpio para la siguiente cita.
    expect(screen.getByLabelText(/N° de cita \/ ticket/)).toHaveValue('')
  })

  it('CA2: la cita de inspección aparece en el listado como «Programada»', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })
    vi.mocked(citasApi.listarCitas).mockResolvedValue([INSPECCION])

    await llenarDatosBasicos(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    const tabla = await screen.findByRole('table', { name: 'Citas del despacho' })
    const fila = within(tabla).getByText('TK-2026-004512').closest('tr')!
    expect(within(fila).getByText('Inspección')).toBeInTheDocument()
    expect(within(fila).getByText('Programada')).toBeInTheDocument()
    expect(within(fila).getByText('12/11/2026 09:30')).toBeInTheDocument()
  })

  it('CA2: en canal naranja o rojo la cita de inspección se ofrece; en verde no', () => {
    expect(tiposPermitidos('CANAL_ASIGNADO', 'ROJO')).toEqual(['INSPECCION'])
    expect(tiposPermitidos('CANAL_ASIGNADO', 'NARANJA')).toEqual(['INSPECCION'])
    // Con observaciones el despacho sigue en INSPECCION_PROGRAMADA y se puede reprogramar.
    expect(tiposPermitidos('INSPECCION_PROGRAMADA', 'ROJO')).toEqual(['INSPECCION'])
    expect(tiposPermitidos('CANAL_ASIGNADO', 'VERDE')).toEqual([])
    expect(tiposPermitidos('INSPECCION_CONFORME', 'ROJO')).toEqual([])
  })

  it('CA3: la cita de retiro pide y envía los datos del transportista con la fecha y hora confirmadas', async () => {
    vi.mocked(citasApi.registrarCita).mockResolvedValue(RETIRO)
    const user = userEvent.setup()
    mostrar({ estado: 'LEVANTE_OTORGADO', canal: 'VERDE' })
    await screen.findByRole('button', { name: REGISTRAR })

    // Después del levante el formulario propone el retiro y muestra los datos del transportista.
    expect(screen.getByRole('combobox', { name: /Tipo de cita/ })).toHaveTextContent('Retiro de mercancía')
    const transportista = screen.getByLabelText('Datos del transportista')
    await llenarDatosBasicos(user, 'RT-2026-000210')
    await user.type(within(transportista).getByLabelText(/RUC del transportista/), TRANSPORTISTA.ruc)
    await user.type(within(transportista).getByLabelText(/Razón social del transportista/), TRANSPORTISTA.razonSocial)
    await user.type(within(transportista).getByLabelText(/Placa del vehículo/), 'abc-123')
    await user.type(within(transportista).getByLabelText(/Nombre del conductor/), TRANSPORTISTA.conductor)
    vi.mocked(citasApi.listarCitas).mockResolvedValue([RETIRO])
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(citasApi.registrarCita).toHaveBeenCalledWith(7, {
      depositoId: 1,
      numeroCita: 'RT-2026-000210',
      tipo: 'RETIRO',
      fechaHoraConfirmada: '2026-11-12T09:30:00',
      transportista: TRANSPORTISTA,
    })
    const fila = (await screen.findByText('RT-2026-000210')).closest('tr')!
    expect(within(fila).getByText('Transportes Demo SAC')).toBeInTheDocument()
    expect(within(fila).getByText('Placa ABC-123 · Juan Pérez')).toBeInTheDocument()
    expect(within(fila).getByText('16/11/2026 14:00')).toBeInTheDocument()
  })

  it('CA3: sin levante el retiro no se puede elegir y los datos del transportista no aparecen', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    expect(screen.queryByLabelText('Datos del transportista')).not.toBeInTheDocument()
    expect(screen.getByText('El retiro de mercancía se habilita después del levante')).toBeInTheDocument()
    await user.click(screen.getByRole('combobox', { name: /Tipo de cita/ }))
    expect(await screen.findByRole('option', { name: 'Retiro de mercancía' })).toHaveAttribute('aria-disabled', 'true')
  })

  it('CA3: el RUC del transportista se valida con su dígito verificador antes de enviar', async () => {
    const user = userEvent.setup()
    mostrar({ estado: 'LEVANTE_OTORGADO', canal: 'VERDE' })
    await screen.findByRole('button', { name: REGISTRAR })

    await llenarDatosBasicos(user)
    await user.type(screen.getByLabelText(/RUC del transportista/), '20123456789')
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('RUC no válido')).toBeInTheDocument()
    expect(screen.getByText('Ingrese la razón social del transportista')).toBeInTheDocument()
    expect(citasApi.registrarCita).not.toHaveBeenCalled()
  })

  it('CA4: sin «Fecha y hora confirmada» no se guarda y se marca el campo', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('combobox', { name: /Depósito \/ Almacén/ }))
    await user.click(await screen.findByRole('option', { name: 'DT-01 · Depósito temporal 1 (ejemplo)' }))
    await user.type(screen.getByLabelText(/N° de cita \/ ticket/), 'TK-2026-004512')
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Ingrese la fecha y hora confirmada de la cita')).toBeInTheDocument()
    expect(citasApi.registrarCita).not.toHaveBeenCalled()
  })

  it('CA4: si el API responde 400 por la fecha y hora, la marca en su campo y no muestra la cita', async () => {
    vi.mocked(citasApi.registrarCita).mockRejectedValue(
      errorApi(400, {
        codigo: 'VALIDACION',
        mensaje: 'Datos no válidos',
        campos: { fechaHoraConfirmada: 'Ingrese la fecha y hora confirmada de la cita' },
      }),
    )
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await llenarDatosBasicos(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Ingrese la fecha y hora confirmada de la cita')).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Citas del despacho' })).not.toBeInTheDocument()
  })

  it('muestra el 409 del API, p. ej. cuando el retiro llega antes del levante', async () => {
    vi.mocked(citasApi.registrarCita).mockRejectedValue(
      errorApi(409, {
        codigo: 'CITA_RETIRO_SIN_LEVANTE',
        mensaje: 'Registre el levante antes de programar el retiro de la mercancía.',
      }),
    )
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await llenarDatosBasicos(user)
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Registre el levante antes de programar el retiro de la mercancía.')).toBeInTheDocument()
  })

  it('con el canal verde explica que no hay inspección y que el retiro va después del levante', async () => {
    mostrar({ canal: 'VERDE' })

    expect(await screen.findByText(/El canal verde no requiere inspección/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
  })

  it('el perfil Cliente solo ve el listado de citas', async () => {
    vi.mocked(citasApi.listarCitas).mockResolvedValue([INSPECCION])
    mostrar({ puedeEditar: false })

    expect(await screen.findByRole('table', { name: 'Citas del despacho' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
  })

  it('si no cargan las citas muestra el error', async () => {
    vi.mocked(citasApi.listarCitas).mockRejectedValue(errorApi(500, {}))
    mostrar()

    expect(await screen.findByText('No se pudieron cargar las citas del despacho.')).toBeInTheDocument()
  })
})

describe('reglas del formulario de citas', () => {
  const BASE: FormCita = {
    deposito: DEPOSITOS[0]!,
    numeroCita: 'TK-1',
    tipo: 'INSPECCION',
    fechaHoraConfirmada: '2026-11-12T09:30',
    ruc: '',
    razonSocial: '',
    placa: '',
    conductor: '',
  }

  it('validarCita: pide los obligatorios y limita el N° de cita a 30 caracteres', () => {
    expect(validarCita(BASE)).toEqual({})
    expect(validarCita({ ...BASE, deposito: null, numeroCita: 'x'.repeat(31), fechaHoraConfirmada: '' })).toEqual({
      depositoId: 'Seleccione el depósito o almacén',
      numeroCita: 'Máximo 30 caracteres',
      fechaHoraConfirmada: 'Ingrese la fecha y hora confirmada de la cita',
    })
  })

  it('validarCita: en el retiro valida la placa como el backend', () => {
    const retiro = { ...BASE, tipo: 'RETIRO' as const, ...TRANSPORTISTA }
    expect(validarCita(retiro)).toEqual({})
    expect(validarCita({ ...retiro, placa: 'ABC 123' })).toEqual({
      'transportista.placa': 'Placa no válida (letras, números y guion)',
    })
  })

  it('aSolicitud: agrega los segundos y pasa la placa a mayúsculas', () => {
    expect(aSolicitud({ ...BASE, tipo: 'RETIRO', ...TRANSPORTISTA, placa: ' abc-123 ' })).toEqual({
      depositoId: 1,
      numeroCita: 'TK-1',
      tipo: 'RETIRO',
      fechaHoraConfirmada: '2026-11-12T09:30:00',
      transportista: TRANSPORTISTA,
    })
  })
})
