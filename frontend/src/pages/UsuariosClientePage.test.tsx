import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as authApi from '../api/auth'
import * as usuariosApi from '../api/usuariosCliente'
import { renderEnRuta } from '../test/utils'
import type { AccesoDenegado, EmpresaAsignable, UsuarioCliente } from '../types'
import { UsuariosClientePage } from './UsuariosClientePage'

vi.mock('../api/auth')
vi.mock('../api/usuariosCliente')

const RUTA = '/administracion/usuarios-cliente'

/** Ejemplos con la forma del contrato C11. */
const ANDINA: EmpresaAsignable = { id: 1, ruc: '20512345671', razonSocial: 'Importadora Andina S.A.C.' }
const PACIFICO: EmpresaAsignable = { id: 2, ruc: '20601574315', razonSocial: 'Pacífico Trade E.I.R.L.' }

const USUARIOS: UsuarioCliente[] = [
  {
    id: 6,
    usuario: 'cliente',
    correo: 'cliente@aduanatrack.pe',
    nombre: 'Cliente Demo',
    activo: true,
    clienteId: null,
    ruc: null,
    razonSocial: null,
    asignadoEn: null,
    estado: 'SIN_ASIGNAR',
  },
  {
    id: 5,
    usuario: 'jperez',
    correo: 'jperez@andina.pe',
    nombre: 'Jorge Pérez',
    activo: true,
    clienteId: 1,
    ruc: '20512345671',
    razonSocial: 'Importadora Andina S.A.C.',
    asignadoEn: '2026-11-05T20:15:00',
    estado: 'ASIGNADO',
  },
]

function acceso(id: number, cambios: Partial<AccesoDenegado> = {}): AccesoDenegado {
  return {
    id,
    fecha: '2026-11-05T20:22:00',
    usuario: 'jperez',
    despachoId: 20,
    numeroDespacho: 'DESP-2026-0342',
    clienteId: 2,
    empresaDespacho: 'Pacífico Trade E.I.R.L.',
    resultado: 'OTRA_EMPRESA',
    mensaje: 'Despacho no encontrado',
    ...cambios,
  }
}

function error400(campos: Record<string, string>) {
  return new AxiosError('400', '400', undefined, undefined, {
    status: 400,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { codigo: 'VALIDACION', mensaje: 'Revise los campos marcados.', campos },
  })
}

async function elegirEmpresa(user: ReturnType<typeof userEvent.setup>, empresa: EmpresaAsignable) {
  await user.click(screen.getByRole('combobox', { name: /Empresa importadora/ }))
  await user.click(await screen.findByRole('option', { name: `${empresa.ruc} · ${empresa.razonSocial}` }))
}

describe('UsuariosClientePage (HU-16 · contrato C11)', () => {
  beforeEach(() => {
    vi.mocked(authApi.sesionActual).mockResolvedValue({ usuario: 'admin', nombre: 'Admin', rol: 'ADMIN' })
    vi.mocked(usuariosApi.listarUsuariosCliente).mockResolvedValue(USUARIOS)
    vi.mocked(usuariosApi.listarEmpresasAsignables).mockResolvedValue([ANDINA, PACIFICO])
    vi.mocked(usuariosApi.listarAccesosDenegados).mockResolvedValue({
      contenido: [acceso(1)],
      pagina: 0,
      tamano: 20,
      totalElementos: 1,
      totalPaginas: 1,
    })
    vi.mocked(usuariosApi.asignarEmpresa).mockImplementation(async (id, clienteId) => ({
      ...USUARIOS.find((u) => u.id === id)!,
      clienteId,
      ruc: PACIFICO.ruc,
      razonSocial: PACIFICO.razonSocial,
      asignadoEn: '2026-11-11T10:30:00',
      estado: 'ASIGNADO',
    }))
  })

  it('muestra la lista plana con estado, empresa, RUC y «Última asignación» (asignadoEn)', async () => {
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    const tabla = await screen.findByRole('table', { name: 'Usuarios cliente' })
    const jorge = within(within(tabla).getByText('Jorge Pérez').closest('tr')!).getAllByRole('cell')
    expect(jorge.map((c) => c.textContent)).toEqual([
      'jperez',
      'Jorge Pérez',
      'Importadora Andina S.A.C.',
      '20512345671',
      '05/11/2026 20:15',
      'Asignado',
      'Cambiar',
    ])
    const demo = within(within(tabla).getByText('Cliente Demo').closest('tr')!).getAllByRole('cell')
    expect(demo.map((c) => c.textContent)).toEqual(['cliente', 'Cliente Demo', '—', '—', '—', 'Sin asignar', 'Asignar'])
  })

  it('filtra en pantalla por estado y busca por usuario o RUC', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    const tabla = await screen.findByRole('table', { name: 'Usuarios cliente' })
    const nombres = () =>
      within(tabla)
        .getAllByRole('row')
        .slice(1)
        .map((f) => within(f).queryAllByRole('cell')[1]?.textContent)

    await user.click(screen.getByRole('combobox', { name: 'Filtro' }))
    await user.click(await screen.findByRole('option', { name: 'Sin asignar' }))
    expect(nombres()).toEqual(['Cliente Demo'])

    await user.click(screen.getByRole('combobox', { name: 'Filtro' }))
    await user.click(await screen.findByRole('option', { name: 'Todos' }))
    await user.type(screen.getByRole('searchbox', { name: 'Buscar usuario o RUC' }), '205123')
    expect(nombres()).toEqual(['Jorge Pérez'])

    await user.clear(screen.getByRole('searchbox', { name: 'Buscar usuario o RUC' }))
    await user.type(screen.getByRole('searchbox', { name: 'Buscar usuario o RUC' }), 'nadie')
    expect(within(tabla).getByText('Ningún usuario coincide con el filtro.')).toBeInTheDocument()
    // El filtro no vuelve a pedir la lista al servidor.
    expect(usuariosApi.listarUsuariosCliente).toHaveBeenCalledTimes(1)
  })

  it('el combo de empresa sale de /administracion/empresas-asignables', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Cliente Demo')

    await user.click(screen.getByRole('combobox', { name: /Empresa importadora/ }))
    const opciones = await screen.findAllByRole('option')
    expect(opciones.map((o) => o.textContent)).toEqual([
      '20512345671 · Importadora Andina S.A.C.',
      '20601574315 · Pacífico Trade E.I.R.L.',
    ])
  })

  it('asigna la empresa con PUT { clienteId } y recarga la tabla', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Cliente Demo')

    await user.click(screen.getByRole('button', { name: 'Asignar empresa de Cliente Demo' }))
    expect(screen.getByRole('combobox', { name: /Usuario cliente/ })).toHaveFocus()
    await elegirEmpresa(user, PACIFICO)
    await user.click(screen.getByRole('button', { name: 'Asignar empresa' }))

    expect(usuariosApi.asignarEmpresa).toHaveBeenCalledWith(6, 2)
    expect(await screen.findByText('Se asignó Pacífico Trade E.I.R.L. a Cliente Demo.')).toBeInTheDocument()
    await waitFor(() => expect(usuariosApi.listarUsuariosCliente).toHaveBeenCalledTimes(2))
  })

  it('«Cambiar» trae la empresa actual del usuario al formulario', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Jorge Pérez')

    await user.click(screen.getByRole('button', { name: 'Cambiar empresa de Jorge Pérez' }))

    expect(screen.getByRole('combobox', { name: /Empresa importadora/ })).toHaveValue(
      '20512345671 · Importadora Andina S.A.C.',
    )
  })

  it('no envía sin usuario ni empresa', async () => {
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Cliente Demo')

    await user.click(screen.getByRole('button', { name: 'Asignar empresa' }))

    expect(screen.getByText('Seleccione el usuario cliente')).toBeInTheDocument()
    expect(screen.getByText('Seleccione la empresa importadora')).toBeInTheDocument()
    expect(usuariosApi.asignarEmpresa).not.toHaveBeenCalled()
  })

  it('muestra en el formulario los campos del 400 (clienteId y usuarioId)', async () => {
    vi.mocked(usuariosApi.asignarEmpresa).mockRejectedValue(
      error400({
        clienteId: 'Solo se pueden asignar empresas con RUC válido',
        usuarioId: 'El usuario no tiene el rol Cliente',
      }),
    )
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Cliente Demo')

    await user.click(screen.getByRole('button', { name: 'Asignar empresa de Cliente Demo' }))
    await elegirEmpresa(user, ANDINA)
    await user.click(screen.getByRole('button', { name: 'Asignar empresa' }))

    expect(await screen.findByText('Solo se pueden asignar empresas con RUC válido')).toBeInTheDocument()
    expect(screen.getByText('El usuario no tiene el rol Cliente')).toBeInTheDocument()
  })

  it('la fecha de asignación del formulario es solo informativa', async () => {
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })
    await screen.findByText('Cliente Demo')

    expect(screen.getByLabelText('Fecha de asignación')).toHaveAttribute('readonly')
    expect(screen.getByText('La registra el sistema')).toBeInTheDocument()
  })

  it('accesos denegados: despacho solicitado, empresa del despacho y «Bloqueado · «mensaje»»', async () => {
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    const tabla = await screen.findByRole('table', { name: 'Intentos de acceso denegados' })
    const fila = (await within(tabla).findByText('DESP-2026-0342')).closest('tr')!
    expect(within(fila).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      '05/11/2026 20:22',
      'jperez',
      'DESP-2026-0342',
      'Pacífico Trade E.I.R.L.',
      'Bloqueado · «Despacho no encontrado»',
    ])
    expect(within(tabla).getAllByRole('columnheader').map((c) => c.textContent)).toEqual([
      'Fecha y hora',
      'Usuario',
      'Despacho solicitado',
      'Empresa del despacho',
      'Resultado',
    ])
  })

  it('pagina los accesos denegados en el servidor', async () => {
    vi.mocked(usuariosApi.listarAccesosDenegados).mockImplementation(async (pagina = 0, tamano = 20) => ({
      contenido: [acceso(pagina + 1, { numeroDespacho: pagina === 0 ? 'DESP-2026-0342' : null })],
      pagina,
      tamano,
      totalElementos: 25,
      totalPaginas: 2,
    }))
    const user = userEvent.setup()
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByText('1–20 de 25')).toBeInTheDocument()
    expect(usuariosApi.listarAccesosDenegados).toHaveBeenLastCalledWith(0, 20)

    await user.click(screen.getByRole('button', { name: 'Página siguiente' }))

    expect(await screen.findByText('21–25 de 25')).toBeInTheDocument()
    expect(usuariosApi.listarAccesosDenegados).toHaveBeenLastCalledWith(1, 20)
    // Despacho en borrador: numeroDespacho llega nulo.
    const tabla = screen.getByRole('table', { name: 'Intentos de acceso denegados' })
    expect(within(tabla).getAllByRole('row')[1]).toHaveTextContent('—')
  })

  it('si falla el registro de accesos lo avisa sin ocultar la tabla de usuarios', async () => {
    vi.mocked(usuariosApi.listarAccesosDenegados).mockRejectedValue(new Error('red'))
    renderEnRuta(<UsuariosClientePage />, { ruta: RUTA, patron: RUTA })

    expect(await screen.findByText('No se pudo conectar con el servidor.')).toBeInTheDocument()
    expect(screen.getByText('Jorge Pérez')).toBeInTheDocument()
  })
})
