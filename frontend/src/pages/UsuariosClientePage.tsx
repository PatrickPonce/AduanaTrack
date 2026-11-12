import {
  Alert,
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
  TableRow,
  TextField,
} from '@mui/material'
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'

import { mensajeDeError } from '../api/http'
import { asignarEmpresa, listarAccesosDenegados, listarUsuariosCliente } from '../api/usuariosCliente'
import { ClienteAutocomplete } from '../components/ClienteAutocomplete'
import { useEncabezado } from '../components/Encabezado'
import { EtiquetaColor } from '../components/EtiquetaColor'
import { TituloSeccion } from '../components/Seccion'
import type { AccesoDenegado, ClienteResumen, UsuarioCliente } from '../types'
import { formatoFechaHora } from '../utils/formato'
import { validarRuc } from '../utils/validaciones'

/**
 * HU-16 · Administración → Usuarios cliente (solo ADMIN): asigna a cada usuario CLIENTE la
 * empresa cuyos despachos podrá consultar y muestra los intentos de acceso denegados.
 */
export function UsuariosClientePage() {
  useEncabezado('Administración', 'Usuarios cliente')
  const [usuarios, setUsuarios] = useState<UsuarioCliente[] | null>(null)
  const [errorUsuarios, setErrorUsuarios] = useState<string | null>(null)
  const [accesos, setAccesos] = useState<AccesoDenegado[] | null>(null)
  const [errorAccesos, setErrorAccesos] = useState<string | null>(null)

  const [usuarioId, setUsuarioId] = useState<number | ''>('')
  const [empresa, setEmpresa] = useState<ClienteResumen | null>(null)
  const [intentado, setIntentado] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)
  const formulario = useRef<HTMLFormElement>(null)

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
    listarAccesosDenegados()
      .then(setAccesos)
      .catch((e) => {
        setAccesos([])
        setErrorAccesos(mensajeDeError(e, 'No se pudo cargar el registro de accesos denegados.'))
      })
  }, [cargarUsuarios])

  const errorUsuario = intentado && usuarioId === '' ? 'Seleccione el usuario cliente' : undefined
  const errorEmpresa = !intentado
    ? undefined
    : !empresa
      ? 'Seleccione la empresa'
      : validarRuc(empresa.ruc)
        ? `La empresa tiene un RUC no válido (${empresa.ruc}): corríjalo en Clientes`
        : undefined

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setIntentado(true)
    setExito(null)
    setErrorGuardar(null)
    if (usuarioId === '' || !empresa || validarRuc(empresa.ruc)) return
    setGuardando(true)
    try {
      const actualizado = await asignarEmpresa(usuarioId, empresa.id)
      setExito(`Se asignó ${empresa.razonSocial} a ${actualizado.nombre}.`)
      setUsuarioId('')
      setEmpresa(null)
      setIntentado(false)
      cargarUsuarios()
    } catch (e) {
      setErrorGuardar(mensajeDeError(e, 'No se pudo asignar la empresa.'))
    } finally {
      setGuardando(false)
    }
  }

  /** «Asignar» o «Cambiar» en la tabla: lleva al formulario con el usuario elegido. */
  function editar(u: UsuarioCliente) {
    setUsuarioId(u.id)
    // El autocompletar solo usa id, RUC y razón social; la carta poder no se muestra aquí.
    setEmpresa(u.empresa ? { ...u.empresa, estadoCartaPoder: 'VIGENTE' } : null)
    setIntentado(false)
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
            gridTemplateColumns: { xs: '1fr', md: '1fr 1.5fr auto' },
          }}
        >
          <TextField
            select
            required
            label="Usuario cliente"
            value={usuarioId}
            onChange={(e) => setUsuarioId(e.target.value === '' ? '' : Number(e.target.value))}
            error={Boolean(errorUsuario)}
            helperText={errorUsuario}
          >
            {(usuarios ?? []).map((u) => (
              <MenuItem key={u.id} value={u.id}>
                {u.nombre} ({u.usuario})
              </MenuItem>
            ))}
          </TextField>
          <ClienteAutocomplete
            label="Empresa (razón social o RUC)"
            value={empresa}
            onChange={setEmpresa}
            error={errorEmpresa}
            required
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
                <TableCell>Estado</TableCell>
                <TableCell align="right">Acción</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {usuarios?.map((u) => (
                <TableRow key={u.id}>
                  <TableCell>{u.usuario}</TableCell>
                  <TableCell>{u.nombre}</TableCell>
                  <TableCell>{u.empresa?.razonSocial ?? '—'}</TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{u.empresa?.ruc ?? '—'}</TableCell>
                  <TableCell>
                    {u.empresa ? (
                      <EtiquetaColor texto="Asignado" tono="verde" />
                    ) : (
                      <EtiquetaColor texto="Sin asignar" tono="gris" />
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      onClick={() => editar(u)}
                      aria-label={`${u.empresa ? 'Cambiar' : 'Asignar'} empresa de ${u.nombre}`}
                    >
                      {u.empresa ? 'Cambiar' : 'Asignar'}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {usuarios?.length === 0 && !errorUsuarios && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No hay usuarios con rol cliente.
                  </TableCell>
                </TableRow>
              )}
              {usuarios === null && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Cargando…
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Paper sx={{ p: { xs: 2, md: 2.5 } }}>
        <TituloSeccion>Intentos de acceso denegados</TituloSeccion>
        {errorAccesos && (
          <Alert severity="error" sx={{ mb: 1.5 }}>
            {errorAccesos}
          </Alert>
        )}
        <TableContainer>
          <Table size="small" aria-label="Intentos de acceso denegados" sx={{ '& td, & th': { py: 1.25 } }}>
            <TableHead>
              <TableRow>
                <TableCell>Fecha y hora</TableCell>
                <TableCell>Usuario</TableCell>
                <TableCell>Empresa</TableCell>
                <TableCell>Recurso solicitado</TableCell>
                <TableCell>Motivo</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {accesos?.map((a) => (
                <TableRow key={a.id}>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFechaHora(a.fecha)}</TableCell>
                  <TableCell>{a.usuario}</TableCell>
                  <TableCell>{a.empresa ?? 'Sin empresa'}</TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: 12.5, overflowWrap: 'anywhere' }}>
                    {a.recurso}
                  </TableCell>
                  <TableCell>{a.motivo}</TableCell>
                </TableRow>
              ))}
              {accesos?.length === 0 && !errorAccesos && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No hay intentos de acceso denegados.
                  </TableCell>
                </TableRow>
              )}
              {accesos === null && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Cargando…
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Stack>
  )
}
