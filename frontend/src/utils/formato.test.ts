import { describe, expect, it } from 'vitest'

import { formatoFecha, formatoFechaHora } from './formato'

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
