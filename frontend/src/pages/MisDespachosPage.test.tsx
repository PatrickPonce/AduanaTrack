import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as despachosApi from '../api/despachos'
import * as documentosApi from '../api/documentos'
import * as hitosApi from '../api/hitos'
import * as portalApi from '../api/portalCliente'
import { renderEnRuta } from '../test/utils'
import type { Despacho, DespachoPortal, DocumentosDespacho, MiEmpresa } from '../types'
import { MisDespachosPage } from './MisDespachosPage'

vi.mock('../api/auth')
vi.mock('../api/despachos')
vi.mock('../api/hitos')
vi.mock('../api/documentos', async (original) => ({
  ...(await original<typeof import('../api/documentos')>()),
  listarDocumentos: vi.fn(),
}))
vi.mock('../api/portalCliente', async (original) => ({
  ...(await original<typeof import('../api/portalCliente')>()),
  obtenerMiEmpresa: vi.fn(),
  listarMisDespachos: vi.fn(),
  obtenerMiDespacho: vi.fn(),
}))

const RUTA = '/portal-cliente/mis-despachos'

/** Ejemplos con la forma del contrato C11. */
const ANDINA: MiEmpresa = { clienteId: 1, ruc: '20512345671', razonSocial: 'Importadora Andina S.A.C.' }

const DESPACHOS: DespachoPortal[] = [
  {
    id: 318,
    numero: 'DESP-2026-0318',
    numeroBl: 'MSCU-BL-55120',
    fechaEta: '2026-10-21',
    estado: 'INSPECCION_PROGRAMADA',
    canalControl: 'NARANJA',
    ultimaActualizacion: '2026-11-05T18:40:00',
  },
  {
    id: 355,
    numero: 'DESP-2026-0355',
    numeroBl: null,
    fechaEta: null,
    estado: 'DOCUMENTACION_COMPLETA',
    canalControl: null,
    ultimaActualizacion: '2026-11-04T09:12:00',
  },
]

const DESPACHO_318 = {
  id: 318,
  numero: 'DESP-2026-0318',
  estado: 'INSPECCION_PROGRAMADA',
  canalControl: 'NARANJA',
  aduana: { id: 1, codigo: '118', descripcion: 'Marítima del Callao' },
} as Despacho

const DOCUMENTOS: DocumentosDespacho = {
  estadoGeneral: 'PENDIENTE',
  documentos: [
    {
      tipo: 'FACTURA',
      nombre: 'Factura comercial',
      obligatorio: true,
      estado: 'CARGADO',
      version: 1,
      documentoId: 31,
      archivo: 'factura.pdf',
      tamanoBytes: 204800,
      cargadoPor: 'Agente Demo',
      cargadoEn: '2026-11-02T11:00:00',
    },
    {
      tipo: 'POLIZA',
      nombre: 'Póliza de seguro',
      obligatorio: false,
      estado: 'PENDIENTE',
      version: null,
      documentoId: null,
      archivo: null,
      tamanoBytes: null,
      cargadoPor: null,
      cargadoEn: null,
    },
  ],
}

function errorHttp(status: number, data: Record<string, unknown> = { codigo: 'NO_ENCONTRADO', mensaje: 'Despacho no encontrado' }) {
  return new AxiosError('error', String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  })
}

const numeros = () =>
  within(screen.getByRole('table', { name: 'Mis despachos' }))
    .getAllByRole('row')
    .slice(1)
    .map((f) => within(f).queryAllByRole('cell')[0]?.textContent)

describe('MisDespachosPage (HU-16 · contrato C11)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'jperez', nombre: 'Jorge Pérez', rol: 'CLIENTE' })
    vi.mocked(portalApi.obtenerMiEmpresa).mockResolvedValue(ANDINA)
    vi.mocked(portalApi.listarMisDespachos).mockResolvedValue(DESPACHOS)
    vi.mocked(portalApi.obtenerMiDespacho).mockResolvedValue(DESPACHOS[0]!)
    vi.mocked(despachosApi.obtenerDespacho).mockResolvedValue(DESPACHO_318)
    vi.mocked(hitosApi.listarHitos).mockResolvedValue([
      { tipo: 'APERTURA', descripcion: 'Apertura del despacho', sugerido: false, usuario: 'Agente Demo', fecha: '2026-10-01T09:00:00' },
      { tipo: 'INSPECCION_PROGRAMADA', descripcion: 'Inspección programada', sugerido: false, usuario: 'Agente Demo', fecha: '2026-11-05T18:40:00' },
    ])
    vi.mocked(documentosApi.listarDocumentos).mockResolvedValue(DOCUMENTOS)
  })

  it('el aviso usa la empresa de GET /portal/mi-empresa', async () => {
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Solo se muestran los despachos de Importadora Andina S.A.C. (RUC 20512345671). La información es de solo lectura.',
    )
  })

  it('sin empresa asignada (clienteId nulo) lo indica y no pide despachos', async () => {
    vi.mocked(portalApi.obtenerMiEmpresa).mockResolvedValue({ clienteId: null, ruc: null, razonSocial: null })
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByText('Aún no tiene una empresa asignada; contacte al administrador.')).toBeInTheDocument()
    expect(portalApi.listarMisDespachos).not.toHaveBeenCalled()
  })

  it('lista los despachos con ultimaActualizacion y las columnas del prototipo', async () => {
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    const celdas = within((await screen.findByText('DESP-2026-0318')).closest('tr')!).getAllByRole('cell')
    expect(celdas.map((c) => c.textContent)).toEqual([
      'DESP-2026-0318',
      'MSCU-BL-55120',
      '21/10/2026',
      'Inspección programada',
      'Naranja',
      '05/11/2026 18:40',
      'Ver detalle',
    ])
    expect(portalApi.listarMisDespachos).toHaveBeenCalledWith({ numero: '', estado: undefined, etaDesde: '', etaHasta: '' })
  })

  it('envía los filtros al servidor: número, estado y rango de ETA', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0318')

    await user.type(screen.getByLabelText('N° de despacho'), '0318')
    await user.click(screen.getByRole('combobox', { name: 'Estado' }))
    await user.click(await screen.findByRole('option', { name: 'Inspección programada' }))
    fireEvent.change(screen.getByLabelText('ETA desde'), { target: { value: '2026-10-01' } })
    fireEvent.change(screen.getByLabelText('ETA hasta'), { target: { value: '2026-10-31' } })
    vi.mocked(portalApi.listarMisDespachos).mockResolvedValue([DESPACHOS[0]!])
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() =>
      expect(portalApi.listarMisDespachos).toHaveBeenLastCalledWith({
        numero: '0318',
        estado: 'INSPECCION_PROGRAMADA',
        etaDesde: '2026-10-01',
        etaHasta: '2026-10-31',
      }),
    )
    expect(numeros()).toEqual(['DESP-2026-0318'])
  })

  it('buscar por número sin resultados muestra «Despacho no encontrado»', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0318')

    vi.mocked(portalApi.listarMisDespachos).mockResolvedValue([])
    await user.type(screen.getByLabelText('N° de despacho'), 'DESP-2026-0342')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Despacho no encontrado')).toBeInTheDocument()
  })

  it('muestra junto a cada filtro los campos del 400', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('DESP-2026-0318')

    vi.mocked(portalApi.listarMisDespachos).mockRejectedValue(
      errorHttp(400, {
        codigo: 'VALIDACION',
        mensaje: 'Revise los campos marcados.',
        campos: { etaHasta: 'La fecha final no puede ser anterior a la inicial', estado: 'Estado no válido' },
      }),
    )
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('La fecha final no puede ser anterior a la inicial')).toBeInTheDocument()
    expect(screen.getByText('Estado no válido')).toBeInTheDocument()
    expect(screen.getByLabelText('ETA hasta')).toHaveAttribute('aria-invalid', 'true')
  })

  it('«Ver detalle» arma el detalle con /portal/despachos/{id}, /despachos/{id}, /hitos y /documentos', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })

    await user.click(await screen.findByRole('button', { name: 'Ver detalle del despacho DESP-2026-0318' }))

    expect(await screen.findByRole('heading', { name: 'Despacho DESP-2026-0318' })).toHaveFocus()
    expect(portalApi.obtenerMiDespacho).toHaveBeenCalledWith(318)
    expect(despachosApi.obtenerDespacho).toHaveBeenCalledWith(318)
    expect(hitosApi.listarHitos).toHaveBeenCalledWith(318)
    expect(documentosApi.listarDocumentos).toHaveBeenCalledWith(318)
    expect(screen.getByText('Marítima del Callao')).toBeInTheDocument()

    const hitos = within(screen.getByRole('list', { name: 'Línea de tiempo del despacho' })).getAllByRole('listitem')
    expect(hitos).toHaveLength(2)
    const descarga = screen.getByRole('link', { name: 'Descargar Factura comercial' })
    expect(descarga).toHaveAttribute('href', '/api/v1/despachos/318/documentos/31/archivo')
    // Un documento pendiente no tiene botón: solo la etiqueta.
    expect(screen.queryByRole('link', { name: 'Descargar Póliza de seguro' })).not.toBeInTheDocument()
    expect(screen.getByText('Pendiente')).toBeInTheDocument()
  })

  it('con un enlace directo la cabecera trae BL, ETA y última actualización aunque no esté en la lista', async () => {
    // La lista filtrada no incluye el 318: los datos salen de GET /portal/despachos/{id}.
    vi.mocked(portalApi.listarMisDespachos).mockResolvedValue([DESPACHOS[1]!])
    renderEnRuta(<MisDespachosPage />, { ruta: `${RUTA}?despacho=318`, patron: RUTA })

    const detalle = (await screen.findByRole('heading', { name: 'Despacho DESP-2026-0318' })).closest('section')!
    const datos = within(detalle)
    expect(datos.getByText('MSCU-BL-55120')).toBeInTheDocument()
    expect(datos.getByText('21/10/2026')).toBeInTheDocument()
    expect(datos.getByText('05/11/2026 18:40')).toBeInTheDocument()
  })

  it('un despacho de otra empresa (404) muestra «Despacho no encontrado»', async () => {
    vi.mocked(portalApi.obtenerMiDespacho).mockRejectedValue(errorHttp(404))
    vi.mocked(despachosApi.obtenerDespacho).mockRejectedValue(errorHttp(404))
    renderEnRuta(<MisDespachosPage />, { ruta: `${RUTA}?despacho=342`, patron: RUTA })

    expect(await screen.findByRole('alert')).toHaveTextContent('Despacho no encontrado')
    expect(screen.queryByRole('heading', { name: /Despacho DESP/ })).not.toBeInTheDocument()
  })

  it('es de solo lectura: no hay botones de crear, editar ni cargar', async () => {
    const user = userEvent.setup()
    renderEnRuta(<MisDespachosPage />, { ruta: RUTA, patron: RUTA })
    await user.click(await screen.findByRole('button', { name: 'Ver detalle del despacho DESP-2026-0318' }))
    await screen.findByRole('heading', { name: 'Despacho DESP-2026-0318' })

    const textos = [...screen.getAllByRole('button'), ...screen.getAllByRole('link')].map((b) => b.textContent ?? '')
    expect(textos.filter((t) => /\b(nuevo|crear|editar|guardar|cargar|subir|eliminar|registrar)\b/i.test(t))).toEqual([])
  })
})
