import { createContext, useContext, useEffect } from 'react'

export interface Encabezado {
  /** Ruta de migas, p. ej. "Clientes / Nuevo". */
  migas: string
  titulo: string
}

/** El Layout muestra en su barra superior las migas y el título que fija cada pantalla. */
export const EncabezadoContext = createContext<((encabezado: Encabezado) => void) | null>(null)

export function useEncabezado(migas: string, titulo: string) {
  const fijar = useContext(EncabezadoContext)
  useEffect(() => {
    fijar?.({ migas, titulo })
    document.title = `${titulo} · AduanaTrack`
  }, [fijar, migas, titulo])
}
