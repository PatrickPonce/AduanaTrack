import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as documentosApi from '../api/documentos'
import type { DocumentoRequerido, DocumentosDespacho as Documentos } from '../types'
import { DocumentosDespacho } from './DocumentosDespacho'

vi.mock('../api/documentos')

function pendiente(tipo: string, nombre: string, obligatorio = true): DocumentoRequerido {
  return {
    tipo,
    nombre,
    obligatorio,
    estado: 'PENDIENTE',
    version: null,
    documentoId: null,
    archivo: null,
    tamanoBytes: null,
    cargadoPor: null,
    cargadoEn: null,
  }
}

function cargado(tipo: string, nombre: string, version: number, documentoId: number): DocumentoRequerido {
  return {
    tipo,
    nombre,
    obligatorio: true,
    estado: 'CARGADO',
    version,
    documentoId,
    archivo: `${tipo.toLowerCase()}.pdf`,
    tamanoBytes: 482113,
    cargadoPor: 'Agente Demo',
    cargadoEn: '2026-10-22T10:15:00',
  }
}

/** Ejemplo del contrato C1: factura en v2 y póliza de seguro pendiente. */
const PENDIENTE: Documentos = {
  estadoGeneral: 'PENDIENTE',
  documentos: [
    cargado('FACTURA_COMERCIAL', 'Factura comercial', 2, 41),
    cargado('CONOCIMIENTO_EMBARQUE', 'Conocimiento de embarque (BL)', 1, 30),
    cargado('LISTA_EMPAQUE', 'Lista de empaque', 1, 31),
    pendiente('POLIZA_SEGURO', 'Póliza de seguro'),
    pendiente('CERTIFICADO_ORIGEN', 'Certificado de origen', false),
  ],
}

const COMPLETO: Documentos = {
  estadoGeneral: 'COMPLETO',
  documentos: PENDIENTE.documentos.map((d) =>
    d.tipo === 'POLIZA_SEGURO' ? cargado('POLIZA_SEGURO', 'Póliza de seguro', 1, 42) : d,
  ),
}

function pdf(nombre: string, megas = 1): File {
  const archivo = new File(['%PDF-1.7'], nombre, { type: 'application/pdf' })
  Object.defineProperty(archivo, 'size', { value: megas * 1024 * 1024 })
  return archivo
}

function fila(nombre: string) {
  return screen.getByText(nombre).closest('tr')!
}

describe('DocumentosDespacho (HU-06)', () => {
  beforeEach(() => {
    vi.mocked(documentosApi.listarDocumentos).mockResolvedValue(PENDIENTE)
    vi.mocked(documentosApi.urlArchivoDocumento).mockImplementation(
      (despachoId, documentoId) => `/api/v1/despachos/${despachoId}/documentos/${documentoId}/archivo`,
    )
  })

  it('muestra la tabla de documentos requeridos con estado, versión y descarga', async () => {
    render(<DocumentosDespacho despachoId={7} puedeCargar />)

    const factura = within(await screen.findByText('Factura comercial').then((c) => c.closest('tr')!))
    expect(factura.getByText('Cargado')).toBeInTheDocument()
    expect(factura.getByText('v2')).toBeInTheDocument()
    expect(factura.getByText('22/10/2026 10:15')).toBeInTheDocument()
    expect(factura.getByRole('link', { name: 'Descargar Factura comercial' })).toHaveAttribute(
      'href',
      '/api/v1/despachos/7/documentos/41/archivo',
    )
    expect(within(fila('Póliza de seguro')).getByText('Pendiente')).toBeInTheDocument()
  })

  it('CA1: un PDF de menos de 10 MB se carga y queda «Cargado»', async () => {
    vi.mocked(documentosApi.cargarDocumento).mockResolvedValue({
      documentoId: 42,
      tipo: 'POLIZA_SEGURO',
      version: 1,
      estado: 'CARGADO',
    })
    const user = userEvent.setup({ applyAccept: false })
    render(<DocumentosDespacho despachoId={7} puedeCargar />)

    await user.click(await screen.findByRole('button', { name: 'Cargar Póliza de seguro' }))
    const archivo = pdf('poliza.pdf', 2)
    await user.upload(screen.getByLabelText('Archivo del documento'), archivo)
    vi.mocked(documentosApi.listarDocumentos).mockResolvedValue(COMPLETO)
    await user.click(screen.getByRole('button', { name: 'Cargar documento' }))

    expect(await screen.findByText('Póliza de seguro cargado como versión 1.')).toBeInTheDocument()
    expect(documentosApi.cargarDocumento).toHaveBeenCalledWith(7, 'POLIZA_SEGURO', archivo)
    expect(within(fila('Póliza de seguro')).getByText('Cargado')).toBeInTheDocument()
  })

  it('CA1: un PDF de 11 MB o un .docx se rechazan antes de enviarlos', async () => {
    const user = userEvent.setup({ applyAccept: false })
    render(<DocumentosDespacho despachoId={7} puedeCargar />)
    await user.click(await screen.findByRole('button', { name: 'Cargar Póliza de seguro' }))

    await user.upload(screen.getByLabelText('Archivo del documento'), pdf('poliza.pdf', 11))
    expect(screen.getByRole('status')).toHaveTextContent('supera el tamaño máximo de 10 MB')
    expect(screen.getByRole('button', { name: 'Cargar documento' })).toBeDisabled()

    const docx = new File(['x'], 'poliza.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    fireEvent.drop(screen.getByLabelText('Zona de arrastre del documento'), { dataTransfer: { files: [docx] } })
    expect(screen.getByRole('status')).toHaveTextContent('Solo se permiten archivos PDF o JPG')
    expect(screen.getByRole('button', { name: 'Cargar documento' })).toBeDisabled()
    expect(documentosApi.cargarDocumento).not.toHaveBeenCalled()
  })

  it('CA1: si el backend rechaza el contenido (415) muestra que no es un PDF ni un JPG válido', async () => {
    vi.mocked(documentosApi.cargarDocumento).mockRejectedValue(
      new AxiosError('415', '415', undefined, undefined, {
        status: 415,
        statusText: '',
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: { codigo: 'FORMATO_NO_PERMITIDO', mensaje: 'Solo se aceptan archivos PDF.' },
      }),
    )
    const user = userEvent.setup({ applyAccept: false })
    render(<DocumentosDespacho despachoId={7} puedeCargar />)
    await user.click(await screen.findByRole('button', { name: 'Cargar Póliza de seguro' }))

    fireEvent.drop(screen.getByLabelText('Zona de arrastre del documento'), {
      dataTransfer: { files: [pdf('renombrado.pdf')] },
    })
    await user.click(screen.getByRole('button', { name: 'Cargar documento' }))

    expect(await screen.findByText('El archivo no es un PDF ni una imagen JPG válida.')).toBeInTheDocument()
  })

  it('CA2: con un obligatorio pendiente avisa que bloquea la clasificación y lo nombra', async () => {
    render(<DocumentosDespacho despachoId={7} puedeCargar />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Falta 1 documento obligatorio: Póliza de seguro. No se podrá registrar la clasificación arancelaria',
    )
    expect(screen.getByLabelText('Estado general de la documentación')).toHaveTextContent('Pendiente')
  })

  it('CA3: al reemplazar confirma que la versión anterior se conserva y lista las versiones', async () => {
    vi.mocked(documentosApi.listarVersiones).mockResolvedValue([
      {
        documentoId: 41,
        version: 2,
        vigente: true,
        archivo: 'factura.pdf',
        tamanoBytes: 482113,
        cargadoPor: 'Agente Demo',
        cargadoEn: '2026-10-22T10:15:00',
      },
      {
        documentoId: 40,
        version: 1,
        vigente: false,
        archivo: 'factura-borrador.pdf',
        tamanoBytes: 401000,
        cargadoPor: 'Agente Demo',
        cargadoEn: '2026-10-21T18:00:00',
      },
    ])
    const user = userEvent.setup()
    render(<DocumentosDespacho despachoId={7} puedeCargar />)

    await user.click(await screen.findByRole('button', { name: 'Reemplazar Factura comercial' }))
    const dialogo = screen.getByRole('dialog')
    expect(dialogo).toHaveTextContent('La versión vigente (v2 · factura_comercial.pdf) se conservará en el historial')
    expect(dialogo).toHaveTextContent('quedará como versión 3 vigente')
    expect(within(dialogo).getByRole('button', { name: 'Reemplazar documento' })).toBeDisabled()
    await user.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: 'Ver versiones de Factura comercial' }))
    const versiones = await screen.findByRole('dialog', { name: /Versiones/ })
    expect(await within(versiones).findByText('Vigente')).toBeInTheDocument()
    expect(within(versiones).getByText('Histórica')).toBeInTheDocument()
    expect(within(versiones).getByRole('link', { name: 'Descargar versión 1' })).toHaveAttribute(
      'href',
      '/api/v1/despachos/7/documentos/40/archivo',
    )
    expect(documentosApi.listarVersiones).toHaveBeenCalledWith(7, 'FACTURA_COMERCIAL')
  })

  it('CA4: con el último obligatorio el estado general pasa a «Completo» y se avisa al detalle', async () => {
    vi.mocked(documentosApi.cargarDocumento).mockResolvedValue({
      documentoId: 42,
      tipo: 'POLIZA_SEGURO',
      version: 1,
      estado: 'CARGADO',
    })
    const alCompletar = vi.fn()
    const user = userEvent.setup()
    render(<DocumentosDespacho despachoId={7} puedeCargar onDocumentacionCompleta={alCompletar} />)

    await user.click(await screen.findByRole('button', { name: 'Cargar Póliza de seguro' }))
    fireEvent.drop(screen.getByLabelText('Zona de arrastre del documento'), { dataTransfer: { files: [pdf('poliza.pdf')] } })
    vi.mocked(documentosApi.listarDocumentos).mockResolvedValue(COMPLETO)
    await user.click(screen.getByRole('button', { name: 'Cargar documento' }))

    await waitFor(() => expect(alCompletar).toHaveBeenCalledTimes(1))
    expect(screen.getByLabelText('Estado general de la documentación')).toHaveTextContent('Completo')
    expect(screen.queryByText(/documento obligatorio/)).not.toBeInTheDocument()
  })

  it('regla de negocio 1: acepta una foto JPG del documento además del PDF', async () => {
    vi.mocked(documentosApi.cargarDocumento).mockResolvedValue({
      documentoId: 43,
      tipo: 'POLIZA_SEGURO',
      version: 1,
      estado: 'CARGADO',
    })
    const user = userEvent.setup()
    render(<DocumentosDespacho despachoId={7} puedeCargar />)

    await user.click(await screen.findByRole('button', { name: 'Cargar Póliza de seguro' }))
    const foto = new File(['\xFF\xD8\xFF'], 'poliza.jpg', { type: 'image/jpeg' })
    fireEvent.drop(screen.getByLabelText('Zona de arrastre del documento'), { dataTransfer: { files: [foto] } })
    expect(screen.getByRole('status')).toHaveTextContent('poliza.jpg')
    await user.click(screen.getByRole('button', { name: 'Cargar documento' }))

    await waitFor(() => expect(documentosApi.cargarDocumento).toHaveBeenCalledWith(7, 'POLIZA_SEGURO', foto))
  })

  it('el perfil Cliente consulta y descarga, pero no carga ni reemplaza', async () => {
    render(<DocumentosDespacho despachoId={7} puedeCargar={false} />)

    await screen.findByText('Factura comercial')
    expect(screen.queryByRole('button', { name: /^(Cargar|Reemplazar) / })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Descargar Factura comercial' })).toBeInTheDocument()
  })
})
