import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as canalApi from '../api/canal'
import type { CanalGuardado } from '../types'
import { CanalControlDespacho } from './CanalControlDespacho'

vi.mock('../api/canal')

/** Ejemplos del contrato C5. */
const ROJO: CanalGuardado = {
  canal: 'ROJO',
  estado: 'CANAL_ASIGNADO',
  levanteHabilitado: false,
  hitoSugerido: 'Programar inspección física',
}
const VERDE: CanalGuardado = { canal: 'VERDE', estado: 'CANAL_ASIGNADO', levanteHabilitado: true, hitoSugerido: null }
const NARANJA: CanalGuardado = { canal: 'NARANJA', estado: 'CANAL_ASIGNADO', levanteHabilitado: false, hitoSugerido: null }

function errorApi(status: number, data: Record<string, unknown>) {
  return new AxiosError(String(status), String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  })
}

function mostrar(props: Partial<Parameters<typeof CanalControlDespacho>[0]> = {}) {
  return render(
    <MemoryRouter>
      <CanalControlDespacho despachoId={7} canal={null} puedeEditar {...props} />
    </MemoryRouter>,
  )
}

async function guardarCanal(user: ReturnType<typeof userEvent.setup>, texto: string) {
  await user.click(screen.getByRole('combobox', { name: /Canal informado por SUNAT/ }))
  await user.click(await screen.findByRole('option', { name: texto }))
  await user.click(screen.getByRole('button', { name: 'Guardar canal' }))
}

describe('CanalControlDespacho (HU-10)', () => {
  beforeEach(() => {
    vi.mocked(canalApi.guardarCanal).mockResolvedValue(NARANJA)
  })

  it('CA1: guarda el canal informado por SUNAT y lo muestra de forma visible en el despacho', async () => {
    const alGuardar = vi.fn()
    const user = userEvent.setup()
    mostrar({ onCanalGuardado: alGuardar })
    expect(within(screen.getByLabelText('Canal de control actual')).getByText('-')).toBeInTheDocument()

    await guardarCanal(user, 'Naranja')

    expect(canalApi.guardarCanal).toHaveBeenCalledWith(7, 'NARANJA')
    expect(await screen.findByText(/Canal naranja guardado/)).toBeInTheDocument()
    expect(within(screen.getByLabelText('Canal de control actual')).getByText('Naranja')).toBeInTheDocument()
    expect(alGuardar).toHaveBeenCalledTimes(1)
  })

  it('CA1: ofrece los tres canales del estándar SUNAT', async () => {
    const user = userEvent.setup()
    mostrar()

    await user.click(screen.getByRole('combobox', { name: /Canal informado por SUNAT/ }))

    const opciones = within(await screen.findByRole('listbox')).getAllByRole('option')
    expect(opciones.map((o) => o.textContent)).toEqual(['Verde', 'Naranja', 'Rojo'])
  })

  it('CA2: con canal rojo muestra el hito sugerido para programar la inspección', async () => {
    vi.mocked(canalApi.guardarCanal).mockResolvedValue(ROJO)
    const user = userEvent.setup()
    mostrar()

    await guardarCanal(user, 'Rojo')

    expect(await screen.findByText('Hito sugerido: Programar inspección física.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Ir al módulo de levante' })).not.toBeInTheDocument()
  })

  it('CA3: con canal verde habilita el acceso directo al módulo de levante', async () => {
    vi.mocked(canalApi.guardarCanal).mockResolvedValue(VERDE)
    const user = userEvent.setup()
    mostrar()
    expect(screen.queryByRole('link', { name: 'Ir al módulo de levante' })).not.toBeInTheDocument()

    await guardarCanal(user, 'Verde')

    expect(await screen.findByRole('link', { name: 'Ir al módulo de levante' })).toHaveAttribute('href', '/levante/7')
    expect(screen.queryByText(/Hito sugerido/)).not.toBeInTheDocument()
  })

  it('CA3: el acceso al levante sigue habilitado al volver a abrir un despacho de canal verde', () => {
    mostrar({ canal: 'VERDE' })

    expect(screen.getByRole('link', { name: 'Ir al módulo de levante' })).toBeInTheDocument()
  })

  it('un despacho de canal naranja o rojo no muestra el acceso al levante', () => {
    mostrar({ canal: 'ROJO' })

    expect(screen.queryByRole('link', { name: 'Ir al módulo de levante' })).not.toBeInTheDocument()
  })

  it('CA4: tras guardar avisa que se agregó un hito y lleva a la línea de tiempo', async () => {
    const alVer = vi.fn()
    const user = userEvent.setup()
    mostrar({ onVerLineaTiempo: alVer })

    await guardarCanal(user, 'Naranja')
    await user.click(await screen.findByRole('button', { name: 'Ver línea de tiempo' }))

    expect(screen.getByText(/Se agregó un hito a la línea de tiempo del despacho/)).toBeInTheDocument()
    expect(alVer).toHaveBeenCalledTimes(1)
  })

  it('muestra el canal actual preseleccionado para poder corregirlo', () => {
    mostrar({ canal: 'ROJO' })

    expect(screen.getByRole('combobox', { name: /Canal informado por SUNAT/ })).toHaveTextContent('Rojo')
    expect(within(screen.getByLabelText('Canal de control actual')).getByText('Rojo')).toBeInTheDocument()
  })

  it('no envía si no se eligió ningún canal', async () => {
    const user = userEvent.setup()
    mostrar()

    await user.click(screen.getByRole('button', { name: 'Guardar canal' }))

    expect(await screen.findByText('Seleccione el canal informado por SUNAT')).toBeInTheDocument()
    expect(canalApi.guardarCanal).not.toHaveBeenCalled()
  })

  it('muestra el mensaje del API si el despacho todavía no tiene DAM (409)', async () => {
    vi.mocked(canalApi.guardarCanal).mockRejectedValue(
      errorApi(409, { codigo: 'DAM_NO_NUMERADA', mensaje: 'Registre la numeración de la DAM antes de asignar el canal.' }),
    )
    const user = userEvent.setup()
    mostrar()

    await guardarCanal(user, 'Verde')

    expect(await screen.findByText('Registre la numeración de la DAM antes de asignar el canal.')).toBeInTheDocument()
  })

  it('marca el campo que el API rechaza con 400', async () => {
    vi.mocked(canalApi.guardarCanal).mockRejectedValue(
      errorApi(400, { codigo: 'VALIDACION', mensaje: 'Datos no válidos', campos: { canal: 'Canal no válido' } }),
    )
    const user = userEvent.setup()
    mostrar()

    await guardarCanal(user, 'Verde')

    expect(await screen.findByText('Canal no válido')).toBeInTheDocument()
  })

  it('el perfil Cliente ve el canal pero no puede registrarlo', () => {
    mostrar({ canal: 'NARANJA', puedeEditar: false })

    expect(within(screen.getByLabelText('Canal de control actual')).getByText('Naranja')).toBeInTheDocument()
    expect(screen.getByText('Solo el agente de aduana puede registrar el canal de control.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar canal' })).not.toBeInTheDocument()
  })
})
