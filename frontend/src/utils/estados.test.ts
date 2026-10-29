import { describe, expect, it } from 'vitest'

import { alcanzoEstado } from './estados'

describe('alcanzoEstado', () => {
  it('compara según el orden en que avanza el despacho', () => {
    expect(alcanzoEstado('APERTURA', 'DOCUMENTACION_COMPLETA')).toBe(false)
    expect(alcanzoEstado('DOCUMENTACION_COMPLETA', 'DOCUMENTACION_COMPLETA')).toBe(true)
    expect(alcanzoEstado('LIQUIDADO', 'CLASIFICADO')).toBe(true)
  })

  it('un estado del Sprint 3 cuenta como posterior', () => {
    expect(alcanzoEstado('LEVANTE', 'CANAL_ASIGNADO')).toBe(true)
  })
})
