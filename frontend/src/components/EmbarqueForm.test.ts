import { describe, expect, it } from 'vitest'

import type { Embarque, EmbarqueRequest } from '../types'
import { plazoAlmacenaje, validarEmbarque } from './EmbarqueForm'

const COMPLETO: EmbarqueRequest = {
  numeroBl: 'HBL-2026-00125',
  tipoBl: 'HOUSE',
  naviera: 'Maersk Line',
  buque: '',
  numeroViaje: 'V.226E',
  puertoEmbarque: 'Shanghai',
  puertoDescarga: 'Callao',
  fechaEmbarque: '2026-09-25',
  fechaEta: '2026-10-20',
  contenedores: [
    { numero: 'CSQU3054383', tipo: '40HC', precinto: '', pesoBrutoKg: '18540' },
    { numero: 'MSKU1234565', tipo: '20GP', precinto: '', pesoBrutoKg: '' },
  ],
}

describe('validarEmbarque', () => {
  it('acepta un BL con 2 contenedores válidos', () => {
    expect(validarEmbarque(COMPLETO)).toEqual({})
  })

  it('CA4: sin N° de BL impide continuar', () => {
    expect(validarEmbarque({ ...COMPLETO, numeroBl: '' })).toHaveProperty('numeroBl')
  })

  it('exige al menos un contenedor', () => {
    expect(validarEmbarque({ ...COMPLETO, contenedores: [] })).toHaveProperty('contenedores')
  })

  it('CA2: marca la fila del contenedor con dígito de control errado', () => {
    const errores = validarEmbarque({
      ...COMPLETO,
      contenedores: [...COMPLETO.contenedores, { numero: 'CSQU3054384', tipo: '', precinto: '', pesoBrutoKg: '' }],
    })
    expect(Object.keys(errores)).toEqual(['contenedores[2].numero'])
  })

  it('marca contenedores repetidos', () => {
    const errores = validarEmbarque({
      ...COMPLETO,
      contenedores: [COMPLETO.contenedores[0]!, { numero: 'csqu3054383', tipo: '', precinto: '', pesoBrutoKg: '' }],
    })
    expect(errores['contenedores[1].numero']).toMatch(/repetido/)
  })

  it('la fecha de zarpe no puede ser posterior a la ETA', () => {
    expect(validarEmbarque({ ...COMPLETO, fechaEmbarque: '2026-10-21' })).toHaveProperty('fechaEmbarque')
  })

  it('el peso bruto debe ser un número positivo con hasta 2 decimales', () => {
    const conPeso = (pesoBrutoKg: string) =>
      validarEmbarque({ ...COMPLETO, contenedores: [{ ...COMPLETO.contenedores[0]!, pesoBrutoKg }] })
    expect(conPeso('17980,5')).toEqual({})
    expect(conPeso('0')).toHaveProperty(['contenedores[0].pesoBrutoKg'])
    expect(conPeso('abc')).toHaveProperty(['contenedores[0].pesoBrutoKg'])
  })
})

describe('plazoAlmacenaje (HU-05, CA3)', () => {
  it('recalcula la fecha límite al cambiar la ETA', () => {
    expect(plazoAlmacenaje('2026-10-20', null, '2026-10-08')?.fechaLimite).toBe('2026-11-19')
    expect(plazoAlmacenaje('2026-10-25', null, '2026-10-08')?.fechaLimite).toBe('2026-11-24')
  })

  it('usa los días de almacenaje del último guardado y alerta con 3 días o menos', () => {
    const guardado = { fechaEta: '2026-10-01', fechaLimiteAlmacenaje: '2026-10-21' } as Embarque
    const plazo = plazoAlmacenaje('2026-10-01', guardado, '2026-10-19')
    expect(plazo).toEqual({ fechaLimite: '2026-10-21', diasRestantes: 2, alerta: true })
  })

  it('sin ETA no calcula plazo', () => {
    expect(plazoAlmacenaje('', null)).toBeNull()
  })
})
