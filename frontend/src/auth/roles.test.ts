import { describe, expect, it } from 'vitest'

import { inicioDe, rutaPermitida } from './roles'

describe('roles (HU-16 · rutas por rol)', () => {
  it('el CLIENTE empieza en «Mis despachos»; administrador y agente, en el Dashboard', () => {
    expect(inicioDe('CLIENTE')).toBe('/portal-cliente/mis-despachos')
    expect(inicioDe('AGENTE')).toBe('/dashboard')
    expect(inicioDe('ADMIN')).toBe('/dashboard')
  })

  it('el CLIENTE solo puede abrir el portal y las notificaciones', () => {
    expect(rutaPermitida('CLIENTE', '/portal-cliente/mis-despachos?despacho=3')).toBe(true)
    expect(rutaPermitida('CLIENTE', '/notificaciones')).toBe(true)
    for (const ruta of ['/dashboard', '/clientes', '/embarques', '/citas', '/despachos/3', '/reportes']) {
      expect(rutaPermitida('CLIENTE', ruta)).toBe(false)
    }
    // Un prefijo parecido no cuela una pantalla de agente.
    expect(rutaPermitida('CLIENTE', '/notificaciones-falsas')).toBe(false)
  })

  it('el agente no abre el portal ni la administración; el administrador sí abre la administración', () => {
    expect(rutaPermitida('AGENTE', '/clientes')).toBe(true)
    expect(rutaPermitida('AGENTE', '/portal-cliente/mis-despachos')).toBe(false)
    expect(rutaPermitida('AGENTE', '/administracion/usuarios-cliente')).toBe(false)
    expect(rutaPermitida('ADMIN', '/administracion/usuarios-cliente')).toBe(true)
  })
})
