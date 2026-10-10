import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link as RouterLink, useNavigate, useParams } from 'react-router'

import { listarAduanas, listarRegimenes } from '../api/catalogos'
import { actualizarDespacho, crearDespacho, obtenerDespacho } from '../api/despachos'
import { leerError, mensajeDeError } from '../api/http'
import { ClienteAutocomplete } from '../components/ClienteAutocomplete'
import type { Aduana, ClienteResumen, Regimen } from '../types'

type Campo = 'clienteId' | 'regimenId' | 'aduanaId' | 'descripcionMercancia'

const OBLIGATORIO = 'Campo obligatorio'
const ORDEN_CAMPOS: Campo[] = ['clienteId', 'regimenId', 'aduanaId', 'descripcionMercancia']

/**
 * HU-03 · T-03.4: "Datos generales del despacho". Sirve para un despacho nuevo
 * (/despachos/nuevo) y para completar un borrador (/despachos/:id/editar).
 */
export function DespachoFormPage() {
  const { id } = useParams()
  const despachoId = id ? Number(id) : null
  const navegar = useNavigate()

  const [cliente, setCliente] = useState<ClienteResumen | null>(null)
  const [regimenId, setRegimenId] = useState<number | ''>('')
  const [aduanaId, setAduanaId] = useState<number | ''>('')
  const [referencia, setReferencia] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [numero, setNumero] = useState<string | null>(null)

  const [regimenes, setRegimenes] = useState<Regimen[]>([])
  const [aduanas, setAduanas] = useState<Aduana[]>([])
  const [errorCatalogos, setErrorCatalogos] = useState<string | null>(null)
  const [errores, setErrores] = useState<Partial<Record<Campo, string>>>({})
  const [restriccion, setRestriccion] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(despachoId !== null)
  const [guardando, setGuardando] = useState(false)

  const refs = {
    clienteId: useRef<HTMLDivElement>(null),
    regimenId: useRef<HTMLDivElement>(null),
    aduanaId: useRef<HTMLDivElement>(null),
    descripcionMercancia: useRef<HTMLDivElement>(null),
  }

  useEffect(() => {
    Promise.all([listarRegimenes(), listarAduanas()])
      .then(([r, a]) => {
        setRegimenes(r)
        setAduanas(a)
      })
      .catch(() => setErrorCatalogos('No se pudieron cargar las listas de régimen aduanero y aduana.'))
  }, [])

  useEffect(() => {
    if (despachoId === null) return
    obtenerDespacho(despachoId)
      .then((d) => {
        if (d.estado !== 'BORRADOR') {
          navegar(`/despachos/${d.id}`, { replace: true })
          return
        }
        setCliente({ ...d.cliente })
        setRegimenId(d.regimen?.id ?? '')
        setAduanaId(d.aduana?.id ?? '')
        setReferencia(d.referenciaCliente ?? '')
        setDescripcion(d.descripcionMercancia ?? '')
        setNumero(d.numero)
      })
      .catch((e) => setError(mensajeDeError(e, 'No se pudo cargar el despacho.')))
      .finally(() => setCargando(false))
  }, [despachoId, navegar])

  /** CA3: marca en rojo los obligatorios vacíos y lleva el foco al primero. */
  function marcarErrores(nuevos: Partial<Record<Campo, string>>) {
    setErrores(nuevos)
    const primero = ORDEN_CAMPOS.find((c) => nuevos[c])
    if (primero) {
      refs[primero].current
        ?.querySelector<HTMLElement>('[role="combobox"], textarea:not([aria-hidden="true"]), input:not([aria-hidden="true"])')
        ?.focus()
    }
  }

  async function guardar(borrador: boolean) {
    setError(null)
    setRestriccion(null)
    const nuevos: Partial<Record<Campo, string>> = {}
    if (!cliente) nuevos.clienteId = OBLIGATORIO
    if (!borrador) {
      if (regimenId === '') nuevos.regimenId = OBLIGATORIO
      if (aduanaId === '') nuevos.aduanaId = OBLIGATORIO
      if (!descripcion.trim()) nuevos.descripcionMercancia = OBLIGATORIO
    }
    if (Object.keys(nuevos).length > 0) {
      marcarErrores(nuevos)
      return
    }
    setErrores({})
    setGuardando(true)
    const solicitud = {
      clienteId: cliente!.id,
      regimenId: regimenId === '' ? null : regimenId,
      aduanaId: aduanaId === '' ? null : aduanaId,
      referenciaCliente: referencia,
      descripcionMercancia: descripcion,
      borrador,
    }
    try {
      const guardado = despachoId !== null ? await actualizarDespacho(despachoId, solicitud) : await crearDespacho(solicitud)
      const mensaje = borrador
        ? 'Borrador guardado. Puede completarlo más adelante.'
        : `Despacho ${guardado.numero} registrado en estado Apertura.`
      navegar(`/despachos/${guardado.id}`, { state: { mensaje } })
    } catch (e) {
      const { status, body } = leerError(e)
      if (status === 422) {
        setRestriccion(body?.mensaje ?? 'El cliente no cuenta con carta poder vigente.')
      } else if (status === 400 && body?.campos) {
        marcarErrores(body.campos as Partial<Record<Campo, string>>)
      } else {
        setError(mensajeDeError(e, 'No se pudo guardar el despacho.'))
      }
    } finally {
      setGuardando(false)
    }
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    void guardar(false)
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando despacho" />
      </Box>
    )
  }

  return (
    <Stack spacing={2}>
      <Typography variant="h5" component="h1">
        {despachoId !== null ? 'Completar despacho' : 'Nuevo despacho de importación'}
      </Typography>

      {errorCatalogos && <Alert severity="warning">{errorCatalogos}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}
      {restriccion && (
        <Alert severity="error" role="alert">
          {restriccion}{' '}
          {cliente && (
            <Link component={RouterLink} to={`/clientes/${cliente.id}`}>
              Ir al cliente
            </Link>
          )}
        </Alert>
      )}

      <Paper component="form" onSubmit={enviar} noValidate sx={{ p: 3 }}>
        <Stack spacing={2.5}>
          <Typography variant="subtitle1" component="h2" sx={{ fontWeight: 600 }}>
            Datos generales del despacho
          </Typography>

          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' } }}>
            <Box ref={refs.clienteId}>
            <ClienteAutocomplete
              label="Cliente / Importador"
              value={cliente}
              onChange={(c) => {
                setCliente(c)
                setErrores((e) => ({ ...e, clienteId: undefined }))
                setRestriccion(null)
              }}
              required
              error={errores.clienteId}
            />
            </Box>
            <TextField
              label="N° de Despacho"
              value={numero ?? 'Se generará al guardar'}
              slotProps={{ htmlInput: { readOnly: true } }}
              disabled
            />
          </Box>

          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
            <TextField
              select
              label="Régimen aduanero"
              value={regimenId}
              onChange={(e) => {
                setRegimenId(Number(e.target.value))
                setErrores((er) => ({ ...er, regimenId: undefined }))
              }}
              required
              error={Boolean(errores.regimenId)}
              helperText={errores.regimenId}
              ref={refs.regimenId}
            >
              {regimenes.map((r) => (
                <MenuItem key={r.id} value={r.id}>
                  {r.codigo} - {r.descripcion}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Aduana de destino"
              value={aduanaId}
              onChange={(e) => {
                setAduanaId(Number(e.target.value))
                setErrores((er) => ({ ...er, aduanaId: undefined }))
              }}
              required
              error={Boolean(errores.aduanaId)}
              helperText={errores.aduanaId}
              ref={refs.aduanaId}
            >
              {aduanas.map((a) => (
                <MenuItem key={a.id} value={a.id}>
                  {a.codigo} - {a.nombre}
                </MenuItem>
              ))}
            </TextField>
          </Box>

          <TextField
            label="Referencia del cliente"
            value={referencia}
            onChange={(e) => setReferencia(e.target.value)}
            slotProps={{ htmlInput: { maxLength: 100 } }}
          />
          <TextField
            label="Descripción general de la mercancía"
            value={descripcion}
            onChange={(e) => {
              setDescripcion(e.target.value)
              setErrores((er) => ({ ...er, descripcionMercancia: undefined }))
            }}
            required
            multiline
            minRows={3}
            error={Boolean(errores.descripcionMercancia)}
            helperText={errores.descripcionMercancia}
            ref={refs.descripcionMercancia}
            slotProps={{ htmlInput: { maxLength: 1000 } }}
          />

          <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
            <Button variant="outlined" disabled={guardando} onClick={() => void guardar(true)}>
              Guardar como borrador
            </Button>
            <Button type="submit" variant="contained" disabled={guardando}>
              Guardar y continuar
            </Button>
          </Stack>
        </Stack>
      </Paper>
    </Stack>
  )
}
