import { describe, expect, it } from 'vitest'

import { alcanzoEstado } from './estados'

describe('alcanzoEstado', () => {
  it('compara según el orden en que avanza el despacho', () => {
    expect(alcanzoEstado('APERTURA', 'DOCUMENTACION_COMPLETA')).toBe(false)
    expect(alcanzoEstado('DOCUMENTACION_COMPLETA', 'DOCUMENTACION_COMPLETA')).toBe(true)
    expect(alcanzoEstado('LIQUIDADO', 'CLASIFICADO')).toBe(true)
  })

  it('HU-11: los estados del Sprint 3 siguen a CANAL_ASIGNADO en su orden', () => {
    expect(alcanzoEstado('CANAL_ASIGNADO', 'INSPECCION_PROGRAMADA')).toBe(false)
    expect(alcanzoEstado('INSPECCION_PROGRAMADA', 'CANAL_ASIGNADO')).toBe(true)
    expect(alcanzoEstado('INSPECCION_PROGRAMADA', 'INSPECCION_CONFORME')).toBe(false)
    expect(alcanzoEstado('INSPECCION_CONFORME', 'LEVANTE_OTORGADO')).toBe(false)
    expect(alcanzoEstado('RETIRO_PROGRAMADO', 'LEVANTE_OTORGADO')).toBe(true)
    expect(alcanzoEstado('LEVANTE_OTORGADO', 'RETIRO_PROGRAMADO')).toBe(false)
  })

  it('un estado desconocido cuenta como posterior', () => {
    expect(alcanzoEstado('ESTADO_FUTURO', 'CANAL_ASIGNADO')).toBe(true)
  })
})
