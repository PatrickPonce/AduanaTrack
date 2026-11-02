import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as inspeccionApi from '../api/inspeccion'
import type { InspeccionRegistrada } from '../api/inspeccion'
import { InspeccionDespacho, filasIniciales, resultadoDe, validarChecklist } from './InspeccionDespacho'

vi.mock('../api/inspeccion')

/** Catálogo de ejemplo del contrato C7. */
const CATALOGO = [
  'Cantidad de bultos',
  'Descripción de la mercancía',
  'Marcas y numeración',
  'Estado del contenedor y precintos',
  'Coincidencia con la factura comercial',
]

const CONFORME: InspeccionRegistrada = {
  resultado: 'CONFORME',
  estadoDespacho: 'INSPECCION_CONFORME',
  levanteHabilitado: true,
  actaNombre: 'acta.pdf',
}

const CON_OBSERVACIONES: InspeccionRegistrada = {
  resultado: 'CON_OBSERVACIONES',
  estadoDespacho: 'INSPECCION_PROGRAMADA',
  levanteHabilitado: false,
  actaNombre: null,
}

function pdf(nombre = 'acta.pdf', bytes = 1024) {
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

function mostrar(props: Partial<React.ComponentProps<typeof InspeccionDespacho>> = {}) {
  return render(
    <MemoryRouter>
      <InspeccionDespacho despachoId={7} estado="INSPECCION_PROGRAMADA" puedeEditar {...props} />
    </MemoryRouter>,
  )
}

const REGISTRAR = 'Registrar resultado de inspección'

describe('InspeccionDespacho (HU-12 · T-12.3)', () => {
  beforeEach(() => {
    vi.mocked(inspeccionApi.listarItemsInspeccion).mockResolvedValue(CATALOGO)
    vi.mocked(inspeccionApi.obtenerUltimaInspeccion).mockResolvedValue(null)
    vi.mocked(inspeccionApi.registrarInspeccion).mockResolvedValue(CONFORME)
    vi.mocked(inspeccionApi.urlActaInspeccion).mockReturnValue('/api/v1/despachos/7/inspeccion/acta')
  })

  it('lista un interruptor «Conforme» por ítem del catálogo, todos conformes al abrir', async () => {
    mostrar()

    await screen.findByRole('button', { name: REGISTRAR })

    for (const item of CATALOGO) {
      expect(screen.getByRole('switch', { name: `Conforme: ${item}` })).toBeChecked()
    }
    expect(within(screen.getByRole('status', { name: 'Resultado calculado' })).getByText('Conforme')).toBeInTheDocument()
  })

  it('CA1: con todos los ítems conformes el resultado es «Conforme» y se envía sin observaciones', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(inspeccionApi.registrarInspeccion).toHaveBeenCalledWith(7, {
      items: CATALOGO.map((descripcion) => ({ descripcion, conforme: true, observacion: null })),
      observaciones: null,
      acta: null,
    })
  })

  it('CA1: con un ítem no conforme el resultado es «Con observaciones» y pide la observación del ítem', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('switch', { name: 'Conforme: Marcas y numeración' }))

    expect(
      within(screen.getByRole('status', { name: 'Resultado calculado' })).getByText('Con observaciones'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText(/^Observación: Marcas y numeración/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Indique la observación del ítem')).toBeInTheDocument()
    expect(inspeccionApi.registrarInspeccion).not.toHaveBeenCalled()
  })

  it('CA2: envía la observación del ítem no conforme y las observaciones generales', async () => {
    vi.mocked(inspeccionApi.registrarInspeccion).mockResolvedValue(CON_OBSERVACIONES)
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('switch', { name: 'Conforme: Marcas y numeración' }))
    await user.type(screen.getByLabelText(/^Observación: Marcas y numeración/), 'Marcas no coinciden con el BL')
    await user.type(screen.getByLabelText('Observaciones generales'), 'Se solicita rectificación')
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(inspeccionApi.registrarInspeccion).toHaveBeenCalledWith(7, {
      items: CATALOGO.map((descripcion) => ({
        descripcion,
        conforme: descripcion !== 'Marcas y numeración',
        observacion: descripcion === 'Marcas y numeración' ? 'Marcas no coinciden con el BL' : null,
      })),
      observaciones: 'Se solicita rectificación',
      acta: null,
    })
    expect(await screen.findByText(/Se avisó al agente responsable/)).toBeInTheDocument()
  })

  it('CA2: con observaciones la pestaña queda en solo lectura sin «Ir a levante» y permite una nueva inspección', async () => {
    vi.mocked(inspeccionApi.registrarInspeccion).mockResolvedValue(CON_OBSERVACIONES)
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })
    await user.click(screen.getByRole('switch', { name: 'Conforme: Cantidad de bultos' }))
    await user.type(screen.getByLabelText(/^Observación: Cantidad de bultos/), 'Faltan 2 bultos')
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    const lectura = await screen.findByLabelText('Inspección registrada')
    expect(within(lectura).getByText('Con observaciones')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Ir a levante' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Registrar nueva inspección' }))
    expect(screen.getByRole('button', { name: REGISTRAR })).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: 'Conforme: Cantidad de bultos' })).toBeChecked()
  })

  it('CA3: una inspección conforme deja la pestaña en solo lectura con el botón «Ir a levante»', async () => {
    const alRegistrar = vi.fn()
    const user = userEvent.setup()
    mostrar({ onInspeccionRegistrada: alRegistrar })
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    const lectura = await screen.findByLabelText('Inspección registrada')
    expect(within(lectura).getByText('Conforme')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir a levante' })).toHaveAttribute('href', '/levante/7')
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
    expect(alRegistrar).toHaveBeenCalledTimes(1)
  })

  it('CA4: adjunta el acta en PDF y deja el enlace de descarga', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })
    const acta = pdf('acta.pdf')

    await user.upload(screen.getByLabelText('Archivo del acta de inspección'), acta)
    expect(screen.getByText(/acta\.pdf · 1\.0 KB|acta\.pdf · 1 KB/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(inspeccionApi.registrarInspeccion).toHaveBeenCalledWith(7, expect.objectContaining({ acta }))
    const enlace = await screen.findByRole('link', { name: /Descargar acta/ })
    expect(enlace).toHaveAttribute('href', '/api/v1/despachos/7/inspeccion/acta')
  })

  it('CA4: un acta de 11 MB se rechaza antes de enviar', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.upload(screen.getByLabelText('Archivo del acta de inspección'), pdf('grande.pdf', 11 * 1024 * 1024))

    expect(screen.getByText('El archivo supera el tamaño máximo de 10 MB')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: REGISTRAR }))
    expect(inspeccionApi.registrarInspeccion).toHaveBeenCalledWith(7, expect.objectContaining({ acta: null }))
  })

  it('un archivo que no es PDF se rechaza antes de enviar', async () => {
    const user = userEvent.setup({ applyAccept: false })
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.upload(
      screen.getByLabelText('Archivo del acta de inspección'),
      new File(['x'], 'acta.docx', { type: 'application/msword' }),
    )

    expect(screen.getByText('Solo se permiten archivos PDF')).toBeInTheDocument()
  })

  it('muestra el error del API cuando el despacho ya no está en inspección (409)', async () => {
    vi.mocked(inspeccionApi.registrarInspeccion).mockRejectedValue(
      errorApi(409, { codigo: 'INSPECCION_NO_PROGRAMADA', mensaje: 'Registre la cita de inspección primero.' }),
    )
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('Registre la cita de inspección primero.')).toBeInTheDocument()
  })

  it('un acta que el API rechaza por tamaño (413) muestra el mensaje del acta', async () => {
    vi.mocked(inspeccionApi.registrarInspeccion).mockRejectedValue(errorApi(413, { codigo: 'ARCHIVO_MUY_GRANDE' }))
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('button', { name: REGISTRAR })

    await user.click(screen.getByRole('button', { name: REGISTRAR }))

    expect(await screen.findByText('El acta supera el tamaño máximo de 10 MB.')).toBeInTheDocument()
  })

  it('con la inspección ya conforme muestra el resultado guardado en solo lectura, con sus ítems', async () => {
    vi.mocked(inspeccionApi.obtenerUltimaInspeccion).mockResolvedValue({
      ...CONFORME,
      items: [{ descripcion: 'Cantidad de bultos', conforme: true, observacion: null }],
      observaciones: 'Todo en orden',
      registradaPor: 'Agente Demo',
      registradaEn: '2026-11-12T10:15:00',
    })
    mostrar({ estado: 'INSPECCION_CONFORME' })

    const lectura = await screen.findByLabelText('Inspección registrada')

    expect(within(lectura).getByText('Todo en orden')).toBeInTheDocument()
    expect(within(lectura).getByText('12/11/2026 10:15 · Agente Demo')).toBeInTheDocument()
    expect(within(screen.getByRole('table', { name: 'Ítems verificados' })).getByText('Cantidad de bultos')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir a levante' })).toBeInTheDocument()
  })

  it('con una inspección anterior con observaciones ofrece registrar una nueva', async () => {
    vi.mocked(inspeccionApi.obtenerUltimaInspeccion).mockResolvedValue(CON_OBSERVACIONES)
    mostrar()

    expect(await screen.findByRole('button', { name: 'Registrar nueva inspección' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
  })

  it('el perfil Cliente solo consulta: sin formulario ni «Ir a levante»', async () => {
    vi.mocked(inspeccionApi.obtenerUltimaInspeccion).mockResolvedValue(CONFORME)
    mostrar({ puedeEditar: false, estado: 'INSPECCION_CONFORME' })

    await screen.findByLabelText('Inspección registrada')

    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Ir a levante' })).not.toBeInTheDocument()
  })

  it('sin cita de inspección todavía explica cuándo se registra la inspección', async () => {
    mostrar({ estado: 'CANAL_ASIGNADO' })

    expect(await screen.findByText(/cuando el despacho tiene una cita de inspección programada/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: REGISTRAR })).not.toBeInTheDocument()
  })

  it('si no carga el catálogo muestra el error', async () => {
    vi.mocked(inspeccionApi.listarItemsInspeccion).mockRejectedValue(errorApi(500, {}))
    mostrar()

    expect(await screen.findByText('No se pudo cargar la inspección.')).toBeInTheDocument()
  })
})

describe('reglas del checklist', () => {
  it('resultadoDe: conforme solo si todos los ítems lo son', () => {
    expect(resultadoDe([{ conforme: true }, { conforme: true }])).toBe('CONFORME')
    expect(resultadoDe([{ conforme: true }, { conforme: false }])).toBe('CON_OBSERVACIONES')
  })

  it('validarChecklist: pide observación en cada ítem no conforme y limita los largos', () => {
    const filas = filasIniciales(['A', 'B', 'C'])
    filas[1] = { descripcion: 'B', conforme: false, observacion: '   ' }
    filas[2] = { descripcion: 'C', conforme: false, observacion: 'x'.repeat(256) }

    expect(validarChecklist(filas, 'y'.repeat(501))).toEqual({
      'item-1': 'Indique la observación del ítem',
      'item-2': 'Máximo 255 caracteres',
      observaciones: 'Máximo 500 caracteres',
    })
    expect(validarChecklist(filasIniciales(['A']), '')).toEqual({})
  })
})
