import { useEffect, useState } from 'react'

import { obtenerMiEmpresa } from '../api/portalCliente'
import type { MiEmpresa } from '../types'

export interface EstadoMiEmpresa {
  empresa: MiEmpresa | null
  cargando: boolean
  error: string | null
}

/** Empresa del usuario CLIENTE (para el aviso de «Mis despachos» y el pie del menú). Con `activo` false no consulta. */
export function useMiEmpresa(activo: boolean): EstadoMiEmpresa {
  const [estado, setEstado] = useState<EstadoMiEmpresa>({ empresa: null, cargando: activo, error: null })

  useEffect(() => {
    if (!activo) {
      setEstado({ empresa: null, cargando: false, error: null })
      return
    }
    let vigente = true
    setEstado({ empresa: null, cargando: true, error: null })
    obtenerMiEmpresa()
      .then((empresa) => vigente && setEstado({ empresa, cargando: false, error: null }))
      .catch(() => vigente && setEstado({ empresa: null, cargando: false, error: 'No se pudieron cargar los datos de su empresa.' }))
    return () => {
      vigente = false
    }
  }, [activo])

  return estado
}
