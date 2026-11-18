import axios, { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { descargarArchivo } from './descarga'

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
