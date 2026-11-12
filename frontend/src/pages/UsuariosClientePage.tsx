import {
  Alert,
  Autocomplete,
  Box,
  Button,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
} from '@mui/material'
import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'

import { leerError, mensajeDeError } from '../api/http'
import {
  asignarEmpresa,
  listarAccesosDenegados,
  listarEmpresasAsignables,
  listarUsuariosCliente,
} from '../api/usuariosCliente'
import { useEncabezado } from '../components/Encabezado'
import { EtiquetaColor } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import type { AccesoDenegado, EmpresaAsignable, Pagina, UsuarioCliente } from '../types'
import { formatoFecha, formatoFechaHora, hoyIso } from '../utils/formato'

const TAMANOS = [10, 20, 50]

type FiltroEstado = 'TODOS' | UsuarioCliente['estado']

/** «Filtro» y «Buscar usuario o RUC» del prototipo: el C11 indica hacerlos en pantalla sobre la lista. */
export function filtrarUsuarios(usuarios: UsuarioCliente[], estado: FiltroEstado, texto: string): UsuarioCliente[] {
  const buscado = texto.trim().toLowerCase()
  return usuarios.filter(
    (u) =>
      (estado === 'TODOS' || u.estado === estado) &&
      (!buscado ||
        u.usuario.toLowerCase().includes(buscado) ||
        u.nombre.toLowerCase().includes(buscado) ||
        (u.ruc ?? '').includes(buscado)),
  )
}

/**
 * HU-16 · Administración → Usuarios cliente (solo ADMIN, contrato C11): asigna a cada usuario CLIENTE la
 * empresa cuyos despachos podrá consultar y muestra los intentos de acceso denegados.
 */
export function UsuariosClientePage() {
  useEncabezado('Administración', 'Usuarios cliente')
  const [usuarios, setUsuarios] = useState<UsuarioCliente[] | null>(null)
  const [errorUsuarios, setErrorUsuarios] = useState<string | null>(null)
  const [empresas, setEmpresas] = useState<EmpresaAsignable[]>([])
  const [errorEmpresas, setErrorEmpresas] = useState<string | null>(null)

  const [usuarioId, setUsuarioId] = useState<number | ''>('')
  const [empresa, setEmpresa] = useState<EmpresaAsignable | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)
  const formulario = useRef<HTMLFormElement>(null)
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('TODOS')
  const [busqueda, setBusqueda] = useState('')
  const visibles = useMemo(
    () => filtrarUsuarios(usuarios ?? [], filtroEstado, busqueda),
    [usuarios, filtroEstado, busqueda],
  )

  const cargarUsuarios = useCallback(() => {
    listarUsuariosCliente()
      .then((u) => {
        setUsuarios(u)
        setErrorUsuarios(null)
      })
      .catch((e) => {
        setUsuarios([])
        setErrorUsuarios(mensajeDeError(e, 'No se pudo cargar la lista de usuarios cliente.'))
      })
  }, [])

  useEffect(() => {
    cargarUsuarios()
    listarEmpresasAsignables()
      .then(setEmpresas)
      .catch((e) => setErrorEmpresas(mensajeDeError(e, 'No se pudo cargar la lista de empresas.')))
  }, [cargarUsuarios])

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setExito(null)
    setErrorGuardar(null)
    const faltan: Record<string, string> = {}
    if (usuarioId === '') faltan.usuarioId = 'Seleccione el usuario cliente'
    if (!empresa) faltan.clienteId = 'Seleccione la empresa importadora'
    setErrores(faltan)
    if (usuarioId === '' || !empresa) return
    setGuardando(true)
    try {
      const fila = await asignarEmpresa(usuarioId, empresa.id)
      setExito(`Se asignó ${fila.razonSocial ?? empresa.razonSocial} a ${fila.nombre}.`)
      setUsuarioId('')
      setEmpresa(null)
      cargarUsuarios()
    } catch (e) {
      const { status, body } = leerError(e)
      // 400 del C11: { clienteId } o { usuarioId } junto al campo que corresponde.
      if (status === 400 && body?.campos) setErrores(body.campos)
      else setErrorGuardar(mensajeDeError(e, 'No se pudo asignar la empresa.'))
    } finally {
      setGuardando(false)
    }
  }

  /** «Asignar» o «Cambiar» en la tabla: lleva al formulario con el usuario elegido. */
  function editar(u: UsuarioCliente) {
    setUsuarioId(u.id)
    setEmpresa(u.clienteId != null ? (empresas.find((e) => e.id === u.clienteId) ?? null) : null)
    setErrores({})
    setExito(null)
    setErrorGuardar(null)
    // El foco va al selector de usuario para que quien usa teclado siga desde el formulario.
    const selector = formulario.current?.querySelector<HTMLElement>('[role="combobox"]')
    selector?.focus()
    selector?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
  }

  return (
    <Stack spacing={2.5}>
      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion>Asignar empresa</TituloSeccion>
        {errorEmpresas && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorEmpresas}
          </Alert>
        )}
        <Box
          component="form"
          ref={formulario}
          noValidate
          onSubmit={guardar}
          aria-label="Asignar empresa a un usuario cliente"
          sx={{
            display: 'grid',
            gap: 2,
            alignItems: 'start',
            gridTemplateColumns: { xs: '1fr', md: '1fr 1.5fr 180px auto' },
          }}
        >
          <TextField
            select
            required
            label="Usuario cliente"
            value={usuarioId}
            onChange={(e) => {
              setUsuarioId(e.target.value === '' ? '' : Number(e.target.value))
              setErrores(({ usuarioId: _quitado, ...resto }) => resto)
            }}
            error={Boolean(errores.usuarioId)}
            helperText={errores.usuarioId}
          >
            {(usuarios ?? []).map((u) => (
              <MenuItem key={u.id} value={u.id}>
                {u.nombre} ({u.usuario})
              </MenuItem>
            ))}
          </TextField>
          <Autocomplete
            value={empresa}
            options={empresas}
            getOptionLabel={(e) => `${e.ruc} · ${e.razonSocial}`}
            isOptionEqualToValue={(a, b) => a.id === b.id}
            noOptionsText="Sin coincidencias"
            onChange={(_, nueva) => {
              setEmpresa(nueva)
              setErrores(({ clienteId: _quitado, ...resto }) => resto)
            }}
            renderInput={(params) => (
              <TextField
                {...params}
                required
                label="Empresa importadora (RUC · razón social)"
                error={Boolean(errores.clienteId)}
                helperText={errores.clienteId ?? 'Solo se listan empresas registradas con RUC válido.'}
              />
            )}
          />
          {/* Solo informativa: el servidor fija la fecha real de la asignación (asignadoEn). */}
          <TextField
            label="Fecha de asignación"
            value={formatoFecha(hoyIso())}
            helperText="La registra el sistema"
            slotProps={{ input: { readOnly: true } }}
          />
          <Button type="submit" variant="contained" disabled={guardando} sx={{ mt: { md: 1 } }}>
            {guardando ? 'Guardando…' : 'Asignar empresa'}
          </Button>
        </Box>
        {exito && (
          <Alert severity="success" sx={{ mt: 2 }}>
            {exito}
          </Alert>
        )}
        {errorGuardar && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {errorGuardar}
          </Alert>
        )}
      </Paper>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion>Usuarios cliente</TituloSeccion>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
          <TextField
            select
            size="small"
            label="Filtro"
            value={filtroEstado}
            onChange={(e) => setFiltroEstado(e.target.value as FiltroEstado)}
            sx={{ minWidth: { sm: 180 } }}
          >
            <MenuItem value="TODOS">Todos</MenuItem>
            <MenuItem value="ASIGNADO">Asignados</MenuItem>
            <MenuItem value="SIN_ASIGNAR">Sin asignar</MenuItem>
          </TextField>
          <TextField
            size="small"
            type="search"
            label="Buscar usuario o RUC"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            sx={{ flexGrow: 1, maxWidth: { sm: 360 } }}
          />
        </Stack>
        {errorUsuarios && (
          <Alert severity="error" sx={{ mb: 1.5 }}>
            {errorUsuarios}
          </Alert>
        )}
        <TableContainer>
          <Table size="small" aria-label="Usuarios cliente" sx={{ '& td, & th': { py: 1.25 } }}>
            <TableHead>
              <TableRow>
                <TableCell>Usuario</TableCell>
                <TableCell>Nombre</TableCell>
                <TableCell>Empresa</TableCell>
                <TableCell>RUC</TableCell>
                <TableCell>Última asignación</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell align="right">Acción</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visibles.map((u) => {
                const asignado = u.estado === 'ASIGNADO'
                return (
                  // Fila resaltada si está SIN_ASIGNAR, como en el prototipo.
                  <TableRow key={u.id} sx={asignado ? undefined : { bgcolor: '#fff7ed' }}>
                    <TableCell>{u.usuario}</TableCell>
                    <TableCell>{u.nombre}</TableCell>
                    <TableCell>{u.razonSocial ?? '—'}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{u.ruc ?? '—'}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(u.asignadoEn)}</TableCell>
                    <TableCell>
                      {asignado ? (
                        <EtiquetaColor texto="Asignado" tono="verde" />
                      ) : (
                        <EtiquetaColor texto="Sin asignar" tono="naranja" />
                      )}
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        onClick={() => editar(u)}
                        aria-label={`${asignado ? 'Cambiar' : 'Asignar'} empresa de ${u.nombre}`}
                      >
                        {asignado ? 'Cambiar' : 'Asignar'}
                      </Button>
                    </TableCell>
                  </TableRow>
                )
              })}
              {usuarios !== null && visibles.length === 0 && !errorUsuarios && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    {usuarios.length === 0 ? 'No hay usuarios con rol cliente.' : 'Ningún usuario coincide con el filtro.'}
                  </TableCell>
                </TableRow>
              )}
              {usuarios === null && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Cargando…
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <AccesosDenegados />
    </Stack>
  )
}

/** «Intentos de acceso denegados», paginado en el servidor. */
function AccesosDenegados() {
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(20)
  const [datos, setDatos] = useState<Pagina<AccesoDenegado> | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let activo = true
    setError(null)
    listarAccesosDenegados(pagina, tamano)
      .then((d) => activo && setDatos(d))
      .catch((e) => {
        if (!activo) return
        setDatos(null)
        setError(mensajeDeError(e, 'No se pudo cargar el registro de accesos denegados.'))
      })
    return () => {
      activo = false
    }
  }, [pagina, tamano])

  return (
    <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
      <TituloSeccion>Intentos de acceso denegados</TituloSeccion>
      {error && (
        <Alert severity="error" sx={{ mb: 1.5 }}>
          {error}
        </Alert>
      )}
      <TableContainer>
        <Table size="small" aria-label="Intentos de acceso denegados" sx={{ '& td, & th': { py: 1.25 } }}>
          <TableHead>
            <TableRow>
              <TableCell>Fecha y hora</TableCell>
              <TableCell>Usuario</TableCell>
              <TableCell>Despacho solicitado</TableCell>
              <TableCell>Empresa del despacho</TableCell>
              <TableCell>Resultado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {datos?.contenido.map((a) => (
              <TableRow key={a.id}>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(a.fecha)}</TableCell>
                <TableCell>{a.usuario}</TableCell>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{a.numeroDespacho ?? '—'}</TableCell>
                <TableCell>{a.empresaDespacho ?? '—'}</TableCell>
                <TableCell>
                  <EtiquetaColor texto={`Bloqueado · «${a.mensaje}»`} tono="rojo" />
                </TableCell>
              </TableRow>
            ))}
            {datos?.contenido.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  No hay intentos de acceso denegados.
                </TableCell>
              </TableRow>
            )}
            {!datos && !error && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  Cargando…
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {datos && datos.totalElementos > 0 && (
        <TablePagination
          component="div"
          count={datos.totalElementos}
          page={pagina}
          rowsPerPage={tamano}
          rowsPerPageOptions={TAMANOS}
          onPageChange={(_, nueva) => setPagina(nueva)}
          onRowsPerPageChange={(e) => {
            setTamano(Number(e.target.value))
            setPagina(0)
          }}
          labelRowsPerPage="Filas por página"
          labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
          getItemAriaLabel={(tipo) => (tipo === 'next' ? 'Página siguiente' : tipo === 'previous' ? 'Página anterior' : tipo === 'first' ? 'Primera página' : 'Última página')}
        />
      )}
    </Paper>
  )
}
