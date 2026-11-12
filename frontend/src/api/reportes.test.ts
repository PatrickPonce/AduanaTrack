import { afterEach, describe, expect, it, vi } from 'vitest'

import { http } from './http'
import { normalizarIndicadores, obtenerIndicadoresGestion } from './reportes'

const RANGO = { desde: '2026-09-01', hasta: '2026-11-30' }

/** Respuesta de ejemplo del contrato C10 (sin noviembre, que no tiene despachos). */
const C10 = {
  totalDespachos: 12,
  conLevante: 5,
  tiempoPromedioDias: 9.4,
  despachosPorMes: [
    { mes: '2026-09', cantidad: 3 },
    { mes: '2026-10', cantidad: 6 },
  ],
  porCanal: { VERDE: 4, NARANJA: 2, ROJO: 1 },
}

describe('normalizarIndicadores (HU-15 · gráfico «Despachos por mes»)', () => {
  it('con la lista del C10 completa con 0 los meses del rango que el servidor omite', () => {
    expect(normalizarIndicadores(C10, RANGO).despachosPorMes).toEqual([
      { mes: '2026-09', cantidad: 3 },
      { mes: '2026-10', cantidad: 6 },
      { mes: '2026-11', cantidad: 0 },
    ])
  })

  it('acepta el objeto { "AAAA-MM": n } de un Map<YearMonth, Long> y lo ordena', () => {
    const datos = normalizarIndicadores({ ...C10, despachosPorMes: { '2026-10': 6, '2026-09': 3 } }, RANGO)
    expect(datos.despachosPorMes).toEqual([
      { mes: '2026-09', cantidad: 3 },
      { mes: '2026-10', cantidad: 6 },
      { mes: '2026-11', cantidad: 0 },
    ])
  })

  it('un rango que cruza el año lista los meses en orden', () => {
    const datos = normalizarIndicadores(
      { ...C10, despachosPorMes: [{ mes: '2027-01', cantidad: 2 }] },
      { desde: '2026-11-15', hasta: '2027-02-10' },
    )
    expect(datos.despachosPorMes.map((m) => m.mes)).toEqual(['2026-11', '2026-12', '2027-01', '2027-02'])
  })

  it('sin despachos con levante el promedio queda nulo y los datos que faltan valen 0', () => {
    const datos = normalizarIndicadores({ tiempoPromedioDias: null, despachosPorMes: null, porCanal: null }, RANGO)
    expect(datos).toEqual({
      totalDespachos: 0,
      conLevante: 0,
      tiempoPromedioDias: null,
      despachosPorMes: [
        { mes: '2026-09', cantidad: 0 },
        { mes: '2026-10', cantidad: 0 },
        { mes: '2026-11', cantidad: 0 },
      ],
      porCanal: {},
    })
  })

  it('convierte a número un promedio que llegue como texto (BigDecimal serializado)', () => {
    expect(normalizarIndicadores({ ...C10, tiempoPromedioDias: '9.4' }, RANGO).tiempoPromedioDias).toBe(9.4)
  })
})

describe('obtenerIndicadoresGestion', () => {
  afterEach(() => vi.restoreAllMocks())

  it('pide GET /reportes/indicadores con el rango y entrega la respuesta normalizada', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: C10 })

    const datos = await obtenerIndicadoresGestion(RANGO)

    expect(get).toHaveBeenCalledWith('/reportes/indicadores', { params: RANGO })
    expect(datos.totalDespachos).toBe(12)
    expect(datos.despachosPorMes).toHaveLength(3)
  })
})
