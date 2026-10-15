import { Autocomplete, CircularProgress, TextField } from '@mui/material'
import { useEffect, useState } from 'react'

import { buscarClientes } from '../api/clientes'
import type { ClienteResumen } from '../types'

interface Props {
  label: string
  value: ClienteResumen | null
  onChange: (cliente: ClienteResumen | null) => void
  error?: string
  required?: boolean
  size?: 'small' | 'medium'
}

/** Autocompletar contra GET /clientes?q= (búsqueda por RUC o razón social). */
export function ClienteAutocomplete({ label, value, onChange, error, required, size }: Props) {
  const [texto, setTexto] = useState('')
  const [opciones, setOpciones] = useState<ClienteResumen[]>([])
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    let activo = true
    const espera = setTimeout(() => {
      setCargando(true)
      buscarClientes(texto, 0, 20)
        .then((pagina) => activo && setOpciones(pagina.contenido))
        .catch(() => activo && setOpciones([]))
        .finally(() => activo && setCargando(false))
    }, 300)
    return () => {
      activo = false
      clearTimeout(espera)
    }
  }, [texto])

  return (
    <Autocomplete
      value={value}
      options={value && !opciones.some((o) => o.id === value.id) ? [value, ...opciones] : opciones}
      loading={cargando}
      filterOptions={(x) => x}
      getOptionLabel={(c) => `${c.razonSocial} — RUC ${c.ruc}`}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      noOptionsText="Sin coincidencias"
      loadingText="Buscando…"
      onChange={(_, nuevo) => onChange(nuevo)}
      onInputChange={(_, nuevoTexto, razon) => {
        if (razon === 'input') setTexto(nuevoTexto)
      }}
      size={size}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          required={required}
          error={Boolean(error)}
          helperText={error}
          slotProps={{
            ...params.slotProps,
            input: {
              ...params.slotProps.input,
              endAdornment: (
                <>
                  {cargando ? <CircularProgress color="inherit" size={18} /> : null}
                  {params.slotProps.input.endAdornment}
                </>
              ),
            },
          }}
        />
      )}
    />
  )
}
