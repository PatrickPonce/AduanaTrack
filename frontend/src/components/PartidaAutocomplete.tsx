import { Autocomplete, Box, CircularProgress, TextField, Typography } from '@mui/material'
import { useEffect, useState } from 'react'

import { buscarPartidas } from '../api/clasificacion'
import type { PartidaArancelaria, PartidaResumen } from '../types'

interface Props {
  /** Nombre accesible del campo, p. ej. "Partida arancelaria del ítem 2". */
  etiqueta: string
  value: PartidaResumen | null
  onChange: (partida: PartidaResumen | null) => void
  error?: string
  disabled?: boolean
}

/**
 * HU-07 · «Buscador de partida arancelaria (referencial)» contra GET /catalogos/partidas?q=.
 * Solo consulta cuando hay texto: la tabla tiene un buscador por ítem y no conviene buscar al montar.
 */
export function PartidaAutocomplete({ etiqueta, value, onChange, error, disabled }: Props) {
  const [texto, setTexto] = useState('')
  const [opciones, setOpciones] = useState<PartidaResumen[]>([])
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    const q = texto.trim()
    if (!q) {
      setOpciones([])
      return
    }
    let activo = true
    const espera = setTimeout(() => {
      setCargando(true)
      buscarPartidas(q)
        .then((partidas) => activo && setOpciones(partidas))
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
      disabled={disabled}
      filterOptions={(x) => x}
      getOptionLabel={(p) => `${p.codigoFormateado} · ${p.descripcion}`}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      noOptionsText={texto.trim() ? 'Sin coincidencias en el catálogo' : 'Escriba un código o una descripción'}
      loadingText="Buscando…"
      onChange={(_, nueva) =>
        onChange(nueva && { id: nueva.id, codigoFormateado: nueva.codigoFormateado, descripcion: nueva.descripcion })
      }
      onInputChange={(_, nuevoTexto, razon) => {
        if (razon === 'input') setTexto(nuevoTexto)
      }}
      renderOption={({ key, ...props }, p) => (
        <Box component="li" key={key} {...props}>
          <Box>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              {p.codigoFormateado}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {p.descripcion}
              {'tasaAdValorem' in p && ` · Ad Valorem ${(p as PartidaArancelaria).tasaAdValorem} %`}
            </Typography>
          </Box>
        </Box>
      )}
      size="small"
      sx={{ minWidth: 260 }}
      renderInput={(params) => (
        <TextField
          {...params}
          placeholder="Buscar por código o descripción"
          error={Boolean(error)}
          helperText={error}
          slotProps={{
            ...params.slotProps,
            htmlInput: { ...params.slotProps.htmlInput, 'aria-label': etiqueta },
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
