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

  it('muestra los estados del Sprint 2 con su texto', () => {
    render(
      <>
        <EstadoDespachoChip estado="DOCUMENTACION_COMPLETA" />
        <EstadoDespachoChip estado="TRIBUTOS_PAGADOS" />
        <EstadoDespachoChip estado="CANAL_ASIGNADO" />
      </>,
    )
    expect(screen.getByText('Documentación completa')).toBeInTheDocument()
    expect(screen.getByText('Tributos pagados')).toBeInTheDocument()
    expect(screen.getByText('Canal asignado')).toBeInTheDocument()
  })

  it('un estado futuro sin configurar se muestra tal cual', () => {
    render(<EstadoDespachoChip estado="LEVANTE" />)
    expect(screen.getByText('LEVANTE')).toBeInTheDocument()
  })
})
