import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as portalApi from '../api/portalCliente'
import { renderEnRuta } from '../test/utils'
import type { DespachoPortal, DetalleDespachoPortal, UsuarioSesion } from '../types'
import { MisDespachosPage } from './MisDespachosPage'

vi.mock('../api/auth')
vi.mock('../api/portalCliente', async (original) => ({
  ...(await original<typeof import('../api/portalCliente')>()),
  listarMisDespachos: vi.fn(),
  obtenerMiDespacho: vi.fn(),
}))

const RUTA = '/portal-cliente/mis-despachos'

const CLIENTE: UsuarioSesion = {
  usuario: 'rquispe',
  nombre: 'Rosa Quispe',
  rol: 'CLIENTE',
  empresa: { id: 1, ruc: '20100047218', razonSocial: 'Importadora Andina SAC' },
}

function despacho(id: number, cambios: Partial<DespachoPortal> = {}): DespachoPortal {
  return {
    id,
    numero: `DESP-2026-000${id}`,
    numeroBl: `HBL-00${id}`,
    fechaEta: '2026-11-10',
    estado: 'CANAL_ASIGNADO',
    canalControl: 'VERDE',
    actualizadoEn: '2026-11-08T10:30:00',
    ...cambios,
  }
}

const DETALLE: DetalleDespachoPortal = {
  ...despacho(2, { estado: 'LEVANTE_OTORGADO' }),
  aduana: 'Marítima del Callao',
  hitos: [
    { tipo: 'APERTURA', descripcion: 'Apertura del despacho', sugerido: false, usuario: 'Agente Demo', fecha: '2026-11-01T09:00:00' },
    { tipo: 'LEVANTE_OTORGADO', descripcion: 'Levante otorgado', sugerido: false, usuario: 'Agente Demo', fecha: '2026-11-08T10:30:00' },
  ],
  documentos: [
    { id: 31, tipo: 'Factura comercial', nombreArchivo: 'factura.pdf', tamanoBytes: 204800, cargadoEn: '2026-11-02T11:00:00' },
  ],
}

function errorHttp(status: number) {
  return new AxiosError('error', String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { codigo: 'NO_ENCONTRADO', mensaje: 'No encontrado' },
  })
}

const numeros = () =>
  within(screen.getByRole('table', { name: 'Mis despachos' }))
    .getAllByRole('row')
    .slice(1)
    .map((f) => within(f).queryAllByRole('cell')[0]?.textContent)

describe('MisDespachosPage (HU-16 · Portal del cliente)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue(CLIENTE)
    vi.mocked(portalApi.listarMisDespachos).mockResolvedValue([
      despacho(1, { estado: 'INSPECCION_PROGRAMADA', canalControl: 'ROJO', fechaEta: '2026-10-20' }),
      despacho(2, { estado: 'LEVANTE_OTORGADO', fechaEta: '2026-11-05' }),
      despacho(3, { estado: 'APERTURA', canalControl: null, numeroBl: null, fechaEta: null }),
    ])
    vi.mocked(portalApi.obtenerMiDespacho).mockResolvedValue(DETALLE)
  })

  it('avisa de qué empresa son los despachos y que la información es de solo lectura', async () => {
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    const aviso = await screen.findByText(/Solo se muestran los despachos de/)
    expect(aviso).toHaveTextContent(
      'Solo se muestran los despachos de Importadora Andina SAC (RUC 20100047218). La información es de solo lectura.',
    )
  })

  it('lista los despachos con N° despacho, BL, ETA, estado, canal, última actualización y Ver detalle', async () => {
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0001')

    const tabla = screen.getByRole('table', { name: 'Mis despachos' })
    expect(within(tabla).getAllByRole('columnheader').map((c) => c.textContent)).toEqual([
      'N° despacho',
      'BL',
      'ETA',
      'Estado',
      'Canal',
      'Última actualización',
      'Acciones',
    ])
    const celdas = within(screen.getByText('DESP-2026-0001').closest('tr')!).getAllByRole('cell')
    expect(celdas.map((c) => c.textContent)).toEqual([
      'DESP-2026-0001',
      'HBL-001',
      '20/10/2026',
      'Inspección programada',
      'Rojo',
      '08/11/2026 10:30',
      'Ver detalle',
    ])
  })

  it('es de solo lectura: no hay botones de crear, editar ni cargar', async () => {
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0001')

    const botones = screen.getAllByRole('button').map((b) => b.textContent ?? '')
    expect(botones.filter((t) => /nuevo|crear|editar|guardar|cargar|subir|eliminar|registrar/i.test(t))).toEqual([])
    // Tampoco el filtro de «Cliente» ni «Exportar a Excel» del panel del agente.
    expect(screen.queryByLabelText('Cliente')).not.toBeInTheDocument()
    expect(screen.queryByText('Exportar a Excel')).not.toBeInTheDocument()
  })

  it('filtra por N° de despacho y por estado', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0001')

    await user.type(screen.getByLabelText('N° de despacho'), '0002')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(numeros()).toEqual(['DESP-2026-0002'])

    await user.click(screen.getByRole('button', { name: 'Limpiar' }))
    await user.click(screen.getByRole('combobox', { name: 'Estado' }))
    await user.click(await screen.findByRole('option', { name: 'Apertura' }))
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(numeros()).toEqual(['DESP-2026-0003'])
  })

  it('filtra por rango de ETA y avisa si el rango está invertido', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0001')

    await user.type(screen.getByLabelText('ETA desde'), '2026-11-01')
    await user.type(screen.getByLabelText('ETA hasta'), '2026-11-30')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(numeros()).toEqual(['DESP-2026-0002'])

    await user.clear(screen.getByLabelText('ETA hasta'))
    await user.type(screen.getByLabelText('ETA hasta'), '2026-10-01')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(screen.getByText('La fecha «ETA desde» no puede ser posterior a «ETA hasta».')).toBeInTheDocument()
  })

  it('«Ver detalle» muestra debajo la línea de tiempo y los documentos descargables', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    await user.click(await screen.findByRole('button', { name: 'Ver detalle del despacho DESP-2026-0002' }))

    const titulo = await screen.findByRole('heading', { name: 'Despacho DESP-2026-0002' })
    expect(titulo).toHaveFocus()
    expect(portalApi.obtenerMiDespacho).toHaveBeenCalledWith(2)
    expect(screen.getByTestId('ubicacion')).toHaveTextContent(`${RUTA}?despacho=2`)

    const hitos = within(screen.getByRole('list', { name: 'Línea de tiempo del despacho' })).getAllByRole('listitem')
    expect(hitos.map((h) => h.textContent)).toEqual([
      'Apertura del despacho 01/11/2026 09:00 · Agente Demo',
      'Levante otorgado 08/11/2026 10:30 · Agente Demo',
    ])
    const descarga = screen.getByRole('link', { name: 'Descargar Factura comercial' })
    expect(descarga).toHaveAttribute('href', '/api/v1/portal-cliente/despachos/2/documentos/31/archivo')
    expect(descarga).toHaveAttribute('download')
  })

  it('«Despacho no encontrado» si el despacho no es de su empresa (404 del backend)', async () => {
    vi.mocked(portalApi.obtenerMiDespacho).mockRejectedValue(errorHttp(404))
    renderEnRuta(<MisDespachosPage />, { ruta: `${RUTA}?despacho=99`, patron: RUTA })

    expect(await screen.findByRole('alert')).toHaveTextContent('Despacho no encontrado')
    expect(screen.queryByRole('heading', { name: /Despacho DESP/ })).not.toBeInTheDocument()
  })

  it('«Despacho no encontrado» también con un id que no es número, sin llamar al servidor', async () => {
    renderEnRuta(<MisDespachosPage />, { ruta: `${RUTA}?despacho=abc`, patron: RUTA })

    expect(await screen.findByRole('alert')).toHaveTextContent('Despacho no encontrado')
    expect(portalApi.obtenerMiDespacho).not.toHaveBeenCalled()
  })

  it('sin empresa asignada lo explica y no consulta despachos', async () => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ ...CLIENTE, empresa: null })
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByText(/todavía no tiene una empresa asignada/)).toBeInTheDocument()
    await waitFor(() => expect(portalApi.listarMisDespachos).not.toHaveBeenCalled())
  })

  it('si el servidor falla muestra el error', async () => {
    vi.mocked(portalApi.listarMisDespachos).mockRejectedValue(new Error('red'))
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByText('No se pudo conectar con el servidor.')).toBeInTheDocument()
  })
})
