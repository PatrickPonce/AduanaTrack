import { describe, expect, it } from 'vitest'

import type { EmbarqueRequest } from '../types'
import { validarEmbarque } from './EmbarqueForm'

const COMPLETO: EmbarqueRequest = {
  numeroBl: 'HBL-2026-00125',
  tipoBl: 'HOUSE',
  naviera: 'Maersk Line',
  buque: '',
  puertoEmbarque: 'Shanghai',
  puertoDescarga: 'Callao',
  fechaEmbarque: '2026-09-25',
  fechaEta: '2026-10-20',
  contenedores: [
    { numero: 'CSQU3054383', tipo: '40HC', precinto: '' },
    { numero: 'MSKU1234565', tipo: '20GP', precinto: '' },
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
      contenedores: [...COMPLETO.contenedores, { numero: 'CSQU3054384', tipo: '', precinto: '' }],
    })
    expect(Object.keys(errores)).toEqual(['contenedores[2].numero'])
  })

  it('marca contenedores repetidos', () => {
    const errores = validarEmbarque({
      ...COMPLETO,
      contenedores: [COMPLETO.contenedores[0], { numero: 'csqu3054383', tipo: '', precinto: '' }],
    })
    expect(errores['contenedores[1].numero']).toMatch(/repetido/)
  })

  it('la fecha de embarque no puede ser posterior a la ETA', () => {
    expect(validarEmbarque({ ...COMPLETO, fechaEmbarque: '2026-10-21' })).toHaveProperty('fechaEmbarque')
  })
})
