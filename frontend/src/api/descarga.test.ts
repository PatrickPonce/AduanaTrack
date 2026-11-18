import axios, { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { decodificarNombre, descargarArchivo } from './descarga'

function errorHttp(status: number, data?: unknown) {
  const respuesta = { status, data, headers: {}, statusText: '', config: { headers: new AxiosHeaders() } }
  return new AxiosError('x', 'ERR', undefined, undefined, respuesta as never)
}

describe('descargarArchivo', () => {
  afterEach(() => vi.restoreAllMocks())

  it('un 404 avisa que el archivo no está disponible', async () => {
    vi.spyOn(axios, 'get').mockRejectedValue(errorHttp(404))
    await expect(descargarArchivo('/api/v1/x')).rejects.toThrow('El archivo no está disponible en el servidor')
  })

  it('un 403 avisa que no tiene permiso', async () => {
    vi.spyOn(axios, 'get').mockRejectedValue(errorHttp(403))
    await expect(descargarArchivo('/api/v1/x')).rejects.toThrow('No tiene permiso')
  })

  it('sin respuesta del servidor avisa de la conexión', async () => {
    vi.spyOn(axios, 'get').mockRejectedValue(new AxiosError('red'))
    await expect(descargarArchivo('/api/v1/x')).rejects.toThrow('No se pudo conectar con el servidor.')
  })
})

describe('decodificarNombre', () => {
  it('decodifica RFC 2047 en base64 y en quoted-printable', () => {
    expect(decodificarNombre('=?UTF-8?B?RmFjdHVyYSBjb21lcmNpYWwgbsO6bWVybyAxLnBkZg==?=')).toBe('Factura comercial número 1.pdf')
    expect(decodificarNombre('=?UTF-8?Q?Informe_a=C3=B1o.pdf?=')).toBe('Informe año.pdf')
  })

  it('deja igual un nombre sin codificar', () => {
    expect(decodificarNombre('factura.pdf')).toBe('factura.pdf')
  })
})
