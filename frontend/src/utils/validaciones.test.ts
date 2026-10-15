import { describe, expect, it } from 'vitest'

import { validarArchivoCartaPoder, validarContenedor, validarRuc } from './validaciones'

describe('validarRuc', () => {
  it.each(['20787789324', '20230972151', '20923478826', '10688126342'])('acepta el RUC de prueba %s', (ruc) => {
    expect(validarRuc(ruc)).toBeNull()
  })

  it('rechaza un RUC de 10 dígitos', () => {
    expect(validarRuc('2078778932')).toBe('Debe tener 11 dígitos')
  })

  it('rechaza un dígito verificador errado', () => {
    expect(validarRuc('20787789325')).toMatch(/dígito verificador/)
  })
})

describe('validarContenedor (ISO 6346)', () => {
  it.each(['CSQU3054383', 'MSKU1234565', 'TGHU9876542', 'csqu3054383'])('acepta %s', (numero) => {
    expect(validarContenedor(numero)).toBeNull()
  })

  it('rechaza un dígito de control errado', () => {
    expect(validarContenedor('CSQU3054384')).toMatch(/control/)
  })

  it('rechaza un formato incompleto', () => {
    expect(validarContenedor('CSQ3054383')).toMatch(/Formato/)
  })
})

describe('validarArchivoCartaPoder', () => {
  it('rechaza un PDF de más de 10 MB', () => {
    const archivo = new File(['x'], 'carta.pdf', { type: 'application/pdf' })
    Object.defineProperty(archivo, 'size', { value: 12 * 1024 * 1024 })
    expect(validarArchivoCartaPoder(archivo)).toMatch(/10 MB/)
  })

  it('rechaza un archivo que no es PDF', () => {
    const archivo = new File(['x'], 'carta.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    expect(validarArchivoCartaPoder(archivo)).toMatch(/PDF/)
  })

  it('acepta un PDF pequeño', () => {
    expect(validarArchivoCartaPoder(new File(['%PDF-1.7'], 'carta.pdf', { type: 'application/pdf' }))).toBeNull()
  })
})
