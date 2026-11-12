import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as reportesApi from '../api/reportes'
import type { IndicadoresGestion } from '../api/reportes'
import { separarDestinatarios, validarDestinatarios } from '../components/EnvioProgramadoReporte'
import { renderEnRuta } from '../test/utils'
import type { Rol } from '../types'
import { ReportesPage, rangoInicial, sumarMeses, textoMes, textoTiempoPromedio, validarRango } from './ReportesPage'

vi.mock('../api/auth')
vi.mock('../api/reportes')

/** Respuesta de ejemplo del contrato C10. */
const INDICADORES: IndicadoresGestion = {
  totalDespachos: 12,
  conLevante: 5,
  tiempoPromedioDias: 9.4,
  despachosPorMes: [
    { mes: '2026-09', cantidad: 3 },
    { mes: '2026-10', cantidad: 6 },
  ],
  porCanal: { VERDE: 4, NARANJA: 2, ROJO: 1 },
}

function errorApi(status: number, data: Record<string, unknown>) {
  return new AxiosError(String(status), String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  })
}

function mostrar(rol: Rol = 'ADMIN') {
  vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'admin', nombre: 'Administrador', rol })
  return renderEnRuta(<ReportesPage />, { ruta: '/reportes', patron: '/reportes' })
}

const barras = () =>
  within(screen.getByRole('list', { name: 'Gráfico de despachos por mes' }))
    .getAllByRole('listitem')
    .map((b) => b.getAttribute('aria-label'))

async function aplicarRango(user: ReturnType<typeof userEvent.setup>, desde: string, hasta: string) {
  fireEvent.change(screen.getByLabelText('Desde'), { target: { value: desde } })
  fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: hasta } })
  await user.click(screen.getByRole('button', { name: 'Aplicar' }))
}

describe('ReportesPage (HU-15 · T-15.4)', () => {
  beforeEach(() => {
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockResolvedValue(INDICADORES)
    vi.mocked(reportesApi.exportarReporte).mockResolvedValue()
    vi.mocked(reportesApi.listarEnviosProgramados).mockResolvedValue([])
  })

  it('CA1: muestra el total, los despachos con levante y el tiempo promedio de despacho del rango', async () => {
    mostrar()

    await waitFor(() => expect(screen.getByLabelText('Tiempo promedio de despacho')).toHaveTextContent('9,4 días'))
    expect(screen.getByLabelText('Total de despachos')).toHaveTextContent('12')
    expect(screen.getByLabelText('Con levante')).toHaveTextContent('5')
    expect(reportesApi.obtenerIndicadoresGestion).toHaveBeenCalledWith(rangoInicial())
  })

  it('CA1: sin despachos con levante en el rango el tiempo promedio se muestra como «—»', async () => {
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockResolvedValue({ ...INDICADORES, conLevante: 0, tiempoPromedioDias: null })
    mostrar()

    await waitFor(() => expect(screen.getByLabelText('Con levante')).toHaveTextContent('0'))
    expect(screen.getByLabelText('Tiempo promedio de despacho')).toHaveTextContent('—')
  })

  it('CA2: el gráfico «Despachos por mes» tiene una barra por mes y el resumen por canal', async () => {
    mostrar()

    await screen.findByRole('list', { name: 'Gráfico de despachos por mes' })
    expect(barras()).toEqual(['sep 2026: 3 despachos', 'oct 2026: 6 despachos'])
    const canales = screen.getByLabelText('Despachos por canal')
    expect(within(canales).getByText('Verde: 4')).toBeInTheDocument()
    expect(within(canales).getByText('Naranja: 2')).toBeInTheDocument()
    expect(within(canales).getByText('Rojo: 1')).toBeInTheDocument()
  })

  it('CA2: al cambiar el rango se vuelven a pedir los indicadores y el gráfico se actualiza', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('list', { name: 'Gráfico de despachos por mes' })
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockResolvedValue({
      totalDespachos: 1,
      conLevante: 1,
      tiempoPromedioDias: 7,
      despachosPorMes: [{ mes: '2026-08', cantidad: 1 }],
      porCanal: { VERDE: 1, NARANJA: 0, ROJO: 0 },
    })

    await aplicarRango(user, '2026-08-01', '2026-08-31')

    expect(reportesApi.obtenerIndicadoresGestion).toHaveBeenLastCalledWith({ desde: '2026-08-01', hasta: '2026-08-31' })
    await waitFor(() => expect(barras()).toEqual(['ago 2026: 1 despacho']))
    expect(screen.getByLabelText('Total de despachos')).toHaveTextContent(/^1$/)
    expect(screen.getByLabelText('Tiempo promedio de despacho')).toHaveTextContent('7,0 días')
    expect(screen.getByText('Mostrando del 01/08/2026 al 31/08/2026 (máximo 12 meses).')).toBeInTheDocument()
  })

  it('no consulta con «Desde» posterior a «Hasta» ni con un rango de más de 12 meses', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('list', { name: 'Gráfico de despachos por mes' })
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockClear()

    await aplicarRango(user, '2026-10-10', '2026-10-01')
    expect(screen.getByText('«Desde» no puede ser posterior a «Hasta»')).toBeInTheDocument()

    await aplicarRango(user, '2025-01-01', '2026-02-01')
    expect(screen.getByText('El rango no puede pasar de 12 meses')).toBeInTheDocument()

    expect(reportesApi.obtenerIndicadoresGestion).not.toHaveBeenCalled()
  })

  it('CA3: «Exportar a Excel» y «Exportar a PDF» descargan el reporte del rango aplicado', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('list', { name: 'Gráfico de despachos por mes' })
    await aplicarRango(user, '2026-09-01', '2026-11-30')

    await user.click(screen.getByRole('button', { name: 'Exportar a Excel' }))
    await user.click(screen.getByRole('button', { name: 'Exportar a PDF' }))

    const rango = { desde: '2026-09-01', hasta: '2026-11-30' }
    expect(reportesApi.exportarReporte).toHaveBeenNthCalledWith(1, 'xlsx', rango)
    expect(reportesApi.exportarReporte).toHaveBeenNthCalledWith(2, 'pdf', rango)
  })

  it('CA3: si la exportación falla muestra el motivo', async () => {
    vi.mocked(reportesApi.exportarReporte).mockRejectedValue(
      errorApi(400, { codigo: 'VALIDACION', mensaje: 'Datos no válidos', campos: { hasta: 'El rango no puede pasar de 12 meses' } }),
    )
    const user = userEvent.setup()
    mostrar()
    await screen.findByRole('list', { name: 'Gráfico de despachos por mes' })

    await user.click(screen.getByRole('button', { name: 'Exportar a PDF' }))

    expect(await screen.findByText('El rango no puede pasar de 12 meses')).toBeInTheDocument()
  })

  it('CA4: programa un envío semanal a los destinatarios y lo lista', async () => {
    vi.mocked(reportesApi.programarEnvio).mockResolvedValue({
      id: 3,
      periodicidad: 'SEMANAL',
      destinatarios: ['jefe.operaciones@ejemplo.pe', 'gerencia@ejemplo.pe'],
      activo: true,
      ultimoEnvio: null,
    })
    const user = userEvent.setup()
    mostrar()
    await screen.findByText('No hay envíos programados.')

    await user.type(screen.getByLabelText('Destinatarios'), 'Jefe.Operaciones@ejemplo.pe, gerencia@ejemplo.pe')
    await user.click(screen.getByRole('button', { name: 'Programar envío' }))

    expect(reportesApi.programarEnvio).toHaveBeenCalledWith({
      periodicidad: 'SEMANAL',
      destinatarios: ['jefe.operaciones@ejemplo.pe', 'gerencia@ejemplo.pe'],
    })
    const tabla = await screen.findByRole('table', { name: 'Envíos programados' })
    expect(within(tabla).getByText('Semanal')).toBeInTheDocument()
    expect(within(tabla).getByText('jefe.operaciones@ejemplo.pe, gerencia@ejemplo.pe')).toBeInTheDocument()
    expect(within(tabla).getByText('Todavía no se envía')).toBeInTheDocument()
  })

  it('CA4: elige la periodicidad mensual y no envía con un correo no válido', async () => {
    const user = userEvent.setup()
    mostrar()
    await screen.findByText('No hay envíos programados.')

    await user.click(screen.getByRole('combobox', { name: /Periodicidad/ }))
    await user.click(await screen.findByRole('option', { name: 'Mensual' }))
    await user.type(screen.getByLabelText('Destinatarios'), 'jefe.operaciones')
    await user.click(screen.getByRole('button', { name: 'Programar envío' }))

    expect(screen.getByText('Correo no válido: jefe.operaciones')).toBeInTheDocument()
    expect(reportesApi.programarEnvio).not.toHaveBeenCalled()
    expect(screen.getByRole('combobox', { name: /Periodicidad/ })).toHaveTextContent('Mensual')
  })

  it('CA4: elimina un envío programado', async () => {
    vi.mocked(reportesApi.listarEnviosProgramados).mockResolvedValue([
      { id: 3, periodicidad: 'MENSUAL', destinatarios: ['jefe.operaciones@ejemplo.pe'], ultimoEnvio: '2026-11-01T07:00:00' },
    ])
    vi.mocked(reportesApi.eliminarEnvioProgramado).mockResolvedValue()
    const user = userEvent.setup()
    mostrar()

    const tabla = await screen.findByRole('table', { name: 'Envíos programados' })
    expect(within(tabla).getByText('01/11/2026 07:00')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Eliminar envío mensual a jefe.operaciones@ejemplo.pe' }))

    expect(reportesApi.eliminarEnvioProgramado).toHaveBeenCalledWith(3)
    expect(await screen.findByText('No hay envíos programados.')).toBeInTheDocument()
  })

  it('mientras el envío programado no esté publicado (404) lo explica en lugar de fallar', async () => {
    vi.mocked(reportesApi.listarEnviosProgramados).mockRejectedValue(errorApi(404, { codigo: 'NO_ENCONTRADO' }))
    mostrar()

    expect(await screen.findByText(/todavía no está disponible en el servidor/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Programar envío' })).not.toBeInTheDocument()
  })

  it('si los indicadores no cargan muestra el error y deja exportar', async () => {
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockRejectedValue(errorApi(500, {}))
    mostrar()

    expect(await screen.findByText('No se pudieron cargar los indicadores del rango.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar a Excel' })).toBeEnabled()
    // El panel del gráfico no queda en blanco.
    expect(
      screen.getByText('El gráfico se mostrará cuando el servidor entregue los indicadores del rango.'),
    ).toBeInTheDocument()
  })

  it('con todos los meses en 0 dice que no hay despachos en vez de dibujar barras vacías', async () => {
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockResolvedValue({
      ...INDICADORES,
      totalDespachos: 0,
      despachosPorMes: [
        { mes: '2026-09', cantidad: 0 },
        { mes: '2026-10', cantidad: 0 },
      ],
    })
    mostrar()

    expect(await screen.findByText('No hay despachos abiertos en el rango.')).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Gráfico de despachos por mes' })).not.toBeInTheDocument()
  })

  it('mientras los indicadores no estén publicados (404) lo explica y deja exportar', async () => {
    vi.mocked(reportesApi.obtenerIndicadoresGestion).mockRejectedValue(
      errorApi(404, { codigo: 'NO_ENCONTRADO', mensaje: 'Recurso no encontrado.' }),
    )
    mostrar()

    expect(await screen.findByText(/Los indicadores todavía no están disponibles en el servidor/)).toBeInTheDocument()
    expect(screen.queryByText('Recurso no encontrado.')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar a PDF' })).toBeEnabled()
  })

  it('el perfil Cliente no ve los reportes de gestión', async () => {
    mostrar('CLIENTE')

    expect(await screen.findByText(/son para el administrador y el agente de aduana/)).toBeInTheDocument()
    expect(reportesApi.obtenerIndicadoresGestion).not.toHaveBeenCalled()
  })
})

describe('reglas de la página «Reportes»', () => {
  it('sumarMeses se comporta como LocalDate.plusMonths', () => {
    expect(sumarMeses('2026-01-31', 1)).toBe('2026-02-28')
    expect(sumarMeses('2026-11-03', -2)).toBe('2026-09-03')
    expect(sumarMeses('2025-11-30', 12)).toBe('2026-11-30')
  })

  it('rangoInicial va del primer día de hace dos meses hasta hoy', () => {
    expect(rangoInicial('2026-11-03')).toEqual({ desde: '2026-09-01', hasta: '2026-11-03' })
    expect(rangoInicial('2026-01-15')).toEqual({ desde: '2025-11-01', hasta: '2026-01-15' })
  })

  it('validarRango acepta 12 meses exactos y rechaza uno más', () => {
    expect(validarRango({ desde: '2025-11-30', hasta: '2026-11-30' })).toEqual({})
    expect(validarRango({ desde: '2025-11-30', hasta: '2026-12-01' })).toEqual({ hasta: 'El rango no puede pasar de 12 meses' })
    expect(validarRango({ desde: '', hasta: '' })).toEqual({
      desde: 'Indique la fecha «Desde»',
      hasta: 'Indique la fecha «Hasta»',
    })
  })

  it('textoMes y textoTiempoPromedio', () => {
    expect(textoMes('2026-09')).toBe('sep 2026')
    expect(textoTiempoPromedio(9.4)).toBe('9,4 días')
    expect(textoTiempoPromedio(1)).toBe('1,0 día')
    expect(textoTiempoPromedio(null)).toBe('—')
  })

  it('separarDestinatarios y validarDestinatarios', () => {
    expect(separarDestinatarios(' A@x.pe; b@x.pe , a@x.pe ')).toEqual(['a@x.pe', 'b@x.pe'])
    expect(validarDestinatarios('')).toBe('Ingrese al menos un correo')
    expect(validarDestinatarios('a@x.pe, malo')).toBe('Correo no válido: malo')
    expect(validarDestinatarios(Array.from({ length: 11 }, (_, i) => `u${i}@x.pe`).join(','))).toBe('Máximo 10 destinatarios')
  })
})
