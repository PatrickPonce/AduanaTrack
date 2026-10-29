import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { EstadoDespachoChip } from './EstadoDespachoChip'

describe('EstadoDespachoChip', () => {
  it('HU-03 CA4: muestra «Apertura»', () => {
    render(<EstadoDespachoChip estado="APERTURA" />)
    expect(screen.getByText('Apertura')).toBeInTheDocument()
  })

  it('muestra «Borrador»', () => {
    render(<EstadoDespachoChip estado="BORRADOR" />)
    expect(screen.getByText('Borrador')).toBeInTheDocument()
  })

  it('muestra el texto de cada estado del Sprint 2', () => {
    const esperados: Record<string, string> = {
      DOCUMENTACION_COMPLETA: 'Documentación completa',
      CLASIFICADO: 'Clasificado',
      LIQUIDADO: 'Liquidado',
      TRIBUTOS_PAGADOS: 'Tributos pagados',
      DAM_NUMERADA: 'DAM numerada',
      CANAL_ASIGNADO: 'Canal asignado',
    }
    for (const [estado, texto] of Object.entries(esperados)) {
      const { unmount } = render(<EstadoDespachoChip estado={estado} />)
      expect(screen.getByText(texto)).toBeInTheDocument()
      unmount()
    }
  })

  it('un estado futuro sin configurar se muestra tal cual', () => {
    render(<EstadoDespachoChip estado="LEVANTE" />)
    expect(screen.getByText('LEVANTE')).toBeInTheDocument()
  })
})
