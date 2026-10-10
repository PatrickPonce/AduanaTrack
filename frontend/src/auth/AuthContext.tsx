import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

import * as authApi from '../api/auth'
import type { UsuarioSesion } from '../types'

interface AuthContextValue {
  usuario: UsuarioSesion | null
  cargando: boolean
  iniciarSesion: (usuario: string, password: string, recordarme: boolean) => Promise<void>
  cerrarSesion: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** Sesión del usuario: se consulta GET /auth/me al cargar la app (la cookie es HttpOnly). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let activo = true
    authApi
      .sesionActual()
      .then((u) => activo && setUsuario(u))
      .catch(() => activo && setUsuario(null))
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [])

  const iniciarSesion = useCallback(async (u: string, password: string, recordarme: boolean) => {
    setUsuario(await authApi.login(u, password, recordarme))
  }, [])

  const cerrarSesion = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setUsuario(null)
    }
  }, [])

  const valor = useMemo(
    () => ({ usuario, cargando, iniciarSesion, cerrarSesion }),
    [usuario, cargando, iniciarSesion, cerrarSesion],
  )
  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return contexto
}
