const PREFIJOS_RUC = ['10', '15', '16', '17', '20']
const FACTORES_RUC = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]

/** Mismo algoritmo que el backend: prefijo válido y dígito verificador módulo 11 (HU-04, CA1). */
export function validarRuc(ruc: string): string | null {
  if (!ruc) return 'Ingrese el RUC'
  if (!/^\d{11}$/.test(ruc)) return 'Debe tener 11 dígitos'
  if (!PREFIJOS_RUC.includes(ruc.slice(0, 2))) return 'RUC no válido: revise el dígito verificador'
  const suma = FACTORES_RUC.reduce((total, factor, i) => total + Number(ruc[i]) * factor, 0)
  let digito = 11 - (suma % 11)
  if (digito === 10) digito = 0
  if (digito === 11) digito = 1
  return digito === Number(ruc[10]) ? null : 'RUC no válido: revise el dígito verificador'
}

/** Valor ISO 6346 de cada letra: A=10 y se saltan los múltiplos de 11 (11, 22, 33). */
function valorLetra(letra: string): number {
  let valor = 10
  for (let c = 'A'.charCodeAt(0); c < letra.charCodeAt(0); c++) {
    valor++
    if (valor % 11 === 0) valor++
  }
  return valor
}

/** Número de contenedor ISO 6346: 3 letras, categoría U/J/Z, 6 dígitos y dígito de control (HU-05, CA2). */
export function validarContenedor(numero: string): string | null {
  const n = numero.trim().toUpperCase()
  if (!n) return 'Ingrese el número de contenedor'
  if (!/^[A-Z]{3}[UJZ]\d{7}$/.test(n)) return 'Formato ISO 6346 no válido (ej. CSQU3054383)'
  let suma = 0
  for (let i = 0; i < 10; i++) {
    const caracter = n[i]
    const valor = i < 4 ? valorLetra(caracter) : Number(caracter)
    suma += valor * 2 ** i
  }
  const control = (suma % 11) % 10
  return control === Number(n[10]) ? null : 'Dígito de control ISO 6346 incorrecto'
}

export const TAMANO_MAXIMO_PDF = 10 * 1024 * 1024

/** PDF de máximo 10 MB, revisado antes de enviar: carta poder (HU-04) y cargo de la DAM (HU-09). */
export function validarArchivoPdf(archivo: File): string | null {
  const esPdf = archivo.type === 'application/pdf' || archivo.name.toLowerCase().endsWith('.pdf')
  if (!esPdf) return 'Solo se permiten archivos PDF'
  if (archivo.size > TAMANO_MAXIMO_PDF) return 'El archivo supera el tamaño máximo de 10 MB'
  return null
}

/** Formatos del input de documentos del despacho (HU-06, regla de negocio 1: PDF y JPG). */
export const ACEPTA_DOCUMENTO = 'application/pdf,image/jpeg,.pdf,.jpg,.jpeg'

/** PDF o JPG de máximo 10 MB: documentos del despacho (HU-06, CA1 y regla de negocio 1). */
export function validarArchivoDocumento(archivo: File): string | null {
  const nombre = archivo.name.toLowerCase()
  const esPdf = archivo.type === 'application/pdf' || nombre.endsWith('.pdf')
  const esJpg = archivo.type === 'image/jpeg' || nombre.endsWith('.jpg') || nombre.endsWith('.jpeg')
  if (!esPdf && !esJpg) return 'Solo se permiten archivos PDF o JPG'
  if (archivo.size > TAMANO_MAXIMO_PDF) return 'El archivo supera el tamaño máximo de 10 MB'
  return null
}
