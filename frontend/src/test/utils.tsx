import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'

import { AuthProvider } from '../auth/AuthContext'

/** Muestra la ruta y el estado de navegación actuales para comprobar redirecciones. */
export function UbicacionActual() {
  const ubicacion = useLocation()
  return (
    <div data-testid="ubicacion" data-state={JSON.stringify(ubicacion.state ?? null)}>
      {ubicacion.pathname + ubicacion.search}
    </div>
  )
}

/** Renderiza una pantalla en la ruta indicada, con AuthProvider y un visor de la ubicación. */
export function renderEnRuta(elemento: ReactElement, { ruta, patron }: { ruta: string; patron: string }) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AuthProvider>
        <Routes>
          <Route path={patron} element={elemento} />
          <Route path="*" element={null} />
        </Routes>
        <UbicacionActual />
      </AuthProvider>
    </MemoryRouter>,
  )
}
