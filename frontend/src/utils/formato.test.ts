import { describe, expect, it } from 'vitest'

import { formatoFecha, formatoFechaHora, formatoMonto, formatoTamano } from './formato'

describe('formatoFecha', () => {
  it('convierte la fecha ISO sin desfase de zona horaria', () => {
    expect(formatoFecha('2026-10-20')).toBe('20/10/2026')
    expect(formatoFecha('2026-11-01T00:30:00')).toBe('01/11/2026')
  })

  it('muestra un guion si no hay fecha', () => {
    expect(formatoFecha(null)).toBe('—')
  })

  it('incluye la hora cuando existe', () => {
    expect(formatoFechaHora('2026-10-10T09:15:30.123')).toBe('10/10/2026 09:15')
  })
})

describe('formatoTamano', () => {
  it('usa KB o MB según el tamaño', () => {
    expect(formatoTamano(512)).toBe('512 B')
    expect(formatoTamano(482113)).toBe('470.8 KB')
    expect(formatoTamano(5 * 1024 * 1024)).toBe('5.0 MB')
  })

  it('muestra un guion si no hay tamaño', () => {
    expect(formatoTamano(null)).toBe('—')
  })
})

describe('formatoMonto', () => {
  it('usa dos decimales y separador de miles', () => {
    expect(formatoMonto(4200)).toBe('4,200.00')
    expect(formatoMonto(3225.625)).toBe('3,225.63')
  })
})
