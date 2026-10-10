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

export const TAMANO_MAXIMO_CARTA_PODER = 10 * 1024 * 1024

/** Carta poder: solo PDF y máximo 10 MB, revisado antes de enviar (HU-04). */
export function validarArchivoCartaPoder(archivo: File): string | null {
  const esPdf = archivo.type === 'application/pdf' || archivo.name.toLowerCase().endsWith('.pdf')
  if (!esPdf) return 'Solo se permiten archivos PDF'
  if (archivo.size > TAMANO_MAXIMO_CARTA_PODER) return 'El archivo supera el tamaño máximo de 10 MB'
  return null
}
