import { Alert, Snackbar } from '@mui/material'
import { useState, type MouseEvent, type ReactElement } from 'react'

import { descargarArchivo } from '../api/descarga'

/**
 * Descarga de archivos con aviso de error. El enlace conserva su `href` (clic derecho, nueva pestaña);
 * el clic normal baja el archivo por la API y, si falla (p. ej. el PDF ya no está en el servidor), muestra
 * el motivo en lugar de dejar al usuario sin respuesta.
 */
export function useDescarga(): {
  alDescargar: (url: string) => (evento: MouseEvent) => void
  aviso: ReactElement
} {
  const [error, setError] = useState<string | null>(null)

  const alDescargar = (url: string) => (evento: MouseEvent) => {
    if (evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.button !== 0) return
    evento.preventDefault()
    setError(null)
    descargarArchivo(url).catch((e: unknown) =>
      setError(e instanceof Error ? e.message : 'No se pudo descargar el archivo.'),
    )
  }

  const aviso = (
    <Snackbar open={error !== null} autoHideDuration={8000} onClose={() => setError(null)}>
      <Alert severity="error" onClose={() => setError(null)} role="alert">
        {error}
      </Alert>
    </Snackbar>
  )
  return { alDescargar, aviso }
}
