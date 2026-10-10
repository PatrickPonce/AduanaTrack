# frontend

Interfaz web de AduanaTrack: login, clientes, apertura de despacho, embarque/BL y Dashboard.

**Stack:** React 19 + TypeScript + Vite · Material UI · React Router · Axios · Vitest + Testing Library. Node ≥ 20.19.

## Ejecutar

```bash
npm ci
npm run dev      # http://localhost:5173 (proxy /api → http://localhost:8080)
npm test         # vitest run
npm run build    # tsc -b && vite build → dist/
npm run lint     # oxlint
```

En Docker Compose, nginx sirve `dist/` y hace de proxy de `/api` hacia el backend (`nginx.conf`).

## Estructura

| Carpeta | Contenido |
|---|---|
| `src/api` | Cliente Axios (`http.ts`: `baseURL /api/v1`, `withCredentials`, cabecera `X-XSRF-TOKEN`, redirección al login ante un 401) y un módulo por recurso |
| `src/auth` | `AuthContext` (consulta `GET /auth/me` al cargar) y `ProtectedRoute` |
| `src/pages` | Login, Clientes, ClienteForm, DespachoForm, DespachoDetalle, Dashboard |
| `src/components` | Layout, `EstadoDespachoChip`, `CartaPoderChip`, `ClienteAutocomplete`, `EmbarqueForm` |
| `src/utils` | Validación de RUC (módulo 11), contenedor ISO 6346, carta poder y formato de fechas |
| `src/types` | Tipos del contrato del API |

La sesión vive en la cookie HttpOnly `AT_SESSION`: el token nunca se guarda en LocalStorage.

## Pantallas del Sprint 1

| Ruta | HU · Tarea |
|---|---|
| `/login` | HU-01 · T-01.3 |
| `/clientes`, `/clientes/nuevo`, `/clientes/:id` (pestaña «Historial de cambios») | HU-04 · T-04.4 |
| `/despachos/nuevo`, `/despachos/:id/editar` (completar borrador) | HU-03 · T-03.4 |
| `/despachos/:id` (pestañas «Datos generales» y «Embarque / BL») | HU-03 · T-03.5 · HU-05 · T-05.4 |
| `/dashboard?aduanaId=&clienteId=` | HU-02 · T-02.3 · T-02.4 |

Endpoints de otras tareas que consume: `GET /catalogos/regimenes` y `/catalogos/aduanas` (T-03.2), `POST /clientes/{id}/carta-poder` (T-04.2) y `GET /dashboard/indicadores` (T-02.1). Mientras no existan, la pantalla muestra un aviso o «—» en lugar del dato.
