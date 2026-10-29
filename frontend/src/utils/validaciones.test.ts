import { describe, expect, it } from 'vitest'

import { validarArchivoDocumento, validarArchivoPdf, validarContenedor, validarRuc } from './validaciones'

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

describe('validarArchivoPdf (carta poder y cargo de la DAM)', () => {
  it('rechaza un PDF de más de 10 MB', () => {
    const archivo = new File(['x'], 'carta.pdf', { type: 'application/pdf' })
    Object.defineProperty(archivo, 'size', { value: 12 * 1024 * 1024 })
    expect(validarArchivoPdf(archivo)).toMatch(/10 MB/)
  })

  it('rechaza un archivo que no es PDF', () => {
    const archivo = new File(['x'], 'carta.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    expect(validarArchivoPdf(archivo)).toMatch(/PDF/)
  })

  it('acepta un PDF pequeño', () => {
    expect(validarArchivoPdf(new File(['%PDF-1.7'], 'carta.pdf', { type: 'application/pdf' }))).toBeNull()
  })
})

describe('validarArchivoDocumento (documentos del despacho, HU-06 regla de negocio 1)', () => {
  it('acepta PDF y JPG', () => {
    expect(validarArchivoDocumento(new File(['%PDF-1.7'], 'factura.pdf', { type: 'application/pdf' }))).toBeNull()
    expect(validarArchivoDocumento(new File(['x'], 'factura.jpg', { type: 'image/jpeg' }))).toBeNull()
    expect(validarArchivoDocumento(new File(['x'], 'FACTURA.JPEG', { type: '' }))).toBeNull()
  })

  it('rechaza otros formatos y archivos de más de 10 MB', () => {
    expect(validarArchivoDocumento(new File(['x'], 'factura.png', { type: 'image/png' }))).toBe(
      'Solo se permiten archivos PDF o JPG',
    )
    const grande = new File(['x'], 'factura.jpg', { type: 'image/jpeg' })
    Object.defineProperty(grande, 'size', { value: 12 * 1024 * 1024 })
    expect(validarArchivoDocumento(grande)).toMatch(/10 MB/)
  })
})
