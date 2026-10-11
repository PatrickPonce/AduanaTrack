import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

import type { CodigoHu } from '../utils/historias'

export interface DatosEncabezado {
  hu: CodigoHu
  /** Ruta de navegación que se muestra sobre el título, p. ej. ["Clientes", "Nuevo"]. */
  migas: string[]
}

interface ContextoEncabezado {
  encabezado: DatosEncabezado | null
  fijar: (datos: DatosEncabezado) => void
}

const Contexto = createContext<ContextoEncabezado>({ encabezado: null, fijar: () => {} })

/** Guarda la HU y las migas de la pantalla actual para que el Layout las muestre en la barra superior. */
export function EncabezadoProvider({ children }: { children: ReactNode }) {
  const [encabezado, setEncabezado] = useState<DatosEncabezado | null>(null)
  const valor = useMemo(() => ({ encabezado, fijar: setEncabezado }), [encabezado])
  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useEncabezadoActual() {
  return useContext(Contexto).encabezado
}

/** Cada pantalla declara a qué HU pertenece; el nombre de la HU se muestra como título. */
export function useEncabezado(hu: CodigoHu, ...migas: string[]) {
  const { fijar } = useContext(Contexto)
  const clave = migas.join('\u0000')
  useEffect(() => {
    fijar({ hu, migas: clave.split('\u0000') })
  }, [fijar, hu, clave])
}
