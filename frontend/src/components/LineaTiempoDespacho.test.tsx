import { render, screen, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as hitosApi from '../api/hitos'
import type { Hito } from '../types'
import { LineaTiempoDespacho } from './LineaTiempoDespacho'

vi.mock('../api/hitos')

const HITOS: Hito[] = [
  { tipo: 'APERTURA', descripcion: 'Despacho abierto (DESP-2026-0001)', sugerido: false, usuario: 'Agente Demo', fecha: '2026-10-26T09:15:00' },
  { tipo: 'CANAL_ASIGNADO', descripcion: 'Canal de control asignado: ROJO', sugerido: false, usuario: 'Agente Demo', fecha: '2026-10-28T10:00:00' },
  { tipo: 'HITO_SUGERIDO', descripcion: 'Programar inspección física', sugerido: true, usuario: 'Sistema', fecha: '2026-10-28T10:00:00' },
]

describe('LineaTiempoDespacho (HU-10)', () => {
  beforeEach(() => {
    vi.mocked(hitosApi.listarHitos).mockResolvedValue(HITOS)
  })

  it('CA4: lista cada hito con su fecha, hora y usuario responsable', async () => {
    render(<LineaTiempoDespacho despachoId={7} />)

    const tabla = await screen.findByRole('table', { name: 'Línea de tiempo del despacho' })
    const filas = within(tabla).getAllByRole('row')
    expect(filas).toHaveLength(4)
    expect(within(filas[1]!).getByText('26/10/2026 09:15')).toBeInTheDocument()
    expect(within(filas[1]!).getByText('Despacho abierto (DESP-2026-0001)')).toBeInTheDocument()
    expect(within(filas[2]!).getByText('Canal de control asignado: ROJO')).toBeInTheDocument()
    expect(within(filas[2]!).getByText('28/10/2026 10:00')).toBeInTheDocument()
    expect(hitosApi.listarHitos).toHaveBeenCalledWith(7)
  })

  it('CA2: marca como «Sugerido» el hito que propone el sistema y muestra al usuario «Sistema»', async () => {
    render(<LineaTiempoDespacho despachoId={7} />)

    const tabla = await screen.findByRole('table', { name: 'Línea de tiempo del despacho' })
    const sugerida = within(tabla).getByText('Programar inspección física').closest('tr')!
    expect(within(sugerida).getByText('Sugerido')).toBeInTheDocument()
    expect(within(sugerida).getByText('Sistema')).toBeInTheDocument()
    expect(within(tabla).getAllByText('Sugerido')).toHaveLength(1)
  })

  it('avisa si el despacho todavía no tiene hitos', async () => {
    vi.mocked(hitosApi.listarHitos).mockResolvedValue([])
    render(<LineaTiempoDespacho despachoId={7} />)

    expect(await screen.findByText('Todavía no hay hitos registrados.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('avisa si no se pudo cargar la línea de tiempo', async () => {
    vi.mocked(hitosApi.listarHitos).mockRejectedValue(
      new AxiosError('500', '500', undefined, undefined, {
        status: 500,
        statusText: '',
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: { codigo: 'ERROR_INTERNO', mensaje: 'Error interno.' },
      }),
    )
    render(<LineaTiempoDespacho despachoId={7} />)

    expect(await screen.findByText('Error interno.')).toBeInTheDocument()
  })
})
