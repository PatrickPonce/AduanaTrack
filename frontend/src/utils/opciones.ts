import type { ModalidadDespacho, TipoOperacion } from '../types'

export interface Opcion<T extends string> {
  valor: T
  texto: string
}

/** "Tipo de operación" del prototipo de HU-03. */
export const TIPOS_OPERACION: Opcion<TipoOperacion>[] = [
  { valor: 'IMPORTACION_DEFINITIVA', texto: 'Importación definitiva' },
  { valor: 'ADMISION_TEMPORAL', texto: 'Admisión temporal' },
  { valor: 'REIMPORTACION', texto: 'Reimportación en el mismo estado' },
]

/** "Modalidad de despacho" del prototipo de HU-03. */
export const MODALIDADES_DESPACHO: Opcion<ModalidadDespacho>[] = [
  { valor: 'ANTICIPADO', texto: 'Despacho anticipado' },
  { valor: 'DIFERIDO', texto: 'Despacho diferido' },
  { valor: 'URGENTE', texto: 'Despacho urgente' },
]

/** Tipos de contenedor de la tabla "Contenedores asociados" (guardados como 20GP, 40GP, 40HC). */
export const TIPOS_CONTENEDOR: Opcion<string>[] = [
  { valor: '20GP', texto: "20' GP" },
  { valor: '40GP', texto: "40' GP" },
  { valor: '40HC', texto: "40' HC" },
]

/** Unidades de los ítems de la factura comercial (HU-07); el API acepta hasta 10 caracteres. */
export const UNIDADES_ITEM: Opcion<string>[] = [
  { valor: 'UND', texto: 'Unidades (UND)' },
  { valor: 'KG', texto: 'Kilogramos (KG)' },
  { valor: 'M', texto: 'Metros (M)' },
  { valor: 'L', texto: 'Litros (L)' },
  { valor: 'PAR', texto: 'Pares (PAR)' },
]

export function textoOpcion<T extends string>(opciones: Opcion<T>[], valor: T | null | undefined): string {
  return opciones.find((o) => o.valor === valor)?.texto ?? valor ?? '—'
}

/** Tipo de contribuyente según el prefijo del RUC (10/15/16/17 persona natural, 20 persona jurídica). */
export function tipoContribuyente(ruc: string): string {
  if (ruc.length < 2) return ''
  const prefijo = ruc.slice(0, 2)
  if (prefijo === '20') return 'Persona Jurídica'
  if (['10', '15', '16', '17'].includes(prefijo)) return 'Persona Natural'
  return ''
}
