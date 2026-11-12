# Contrato C11 · Portal del cliente y acceso por empresa (HU-16)

Publica e implementa: Patrick (backend HU-16) · Consume: Kiara (pantallas «Mis despachos» y «Usuarios cliente»). Prototipos: `Prototipo_HU16_Mis_despachos.png` y `Prototipo_HU16_Usuarios_cliente.png`.

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/` paquetes `acceso/` (filtro y administración) y `portal/` (Mis despachos). Migración **V017** (`V013` y `V016` son de Fabio).

## Regla de acceso por empresa

Cada usuario con rol `CLIENTE` tiene **una empresa** (`usuario.cliente_id`). Todo lo que consulta se filtra por esa empresa; `ADMIN` y `AGENTE` siguen viendo todo. La empresa se lee de la base en cada petición: si el administrador la cambia, rige de inmediato (sin volver a iniciar sesión).

| Rutas que usa el CLIENTE | Comportamiento |
|---|---|
| `/api/v1/despachos/{id}` y **todo** `/api/v1/despachos/{id}/...` (hitos, documentos y descargas, clasificación, ítems, liquidación, DAM y cargo, canal, citas, levante y constancia, embarque) | Si el despacho es de otra empresa, **no existe**, es un borrador, o el usuario no tiene empresa: `404 NO_ENCONTRADO` «Despacho no encontrado». La respuesta es idéntica en todos los casos, para no revelar que existe. |
| `GET /api/v1/dashboard/despachos`, `/indicadores`, `/despachos/excel` | Solo despachos de su empresa. El parámetro `clienteId` se **ignora** (siempre se usa el suyo). Sin empresa: lista vacía e indicadores en 0. |
| `GET /api/v1/clientes` | Solo su empresa (`contenido` con 1 fila, o vacío si no tiene). `q`, `page` y `size` no cambian el resultado. |
| `GET /api/v1/clientes/{id}`, `/historial`, `/carta-poder`, `/carta-poder/{cartaId}/archivo` | Solo si `{id}` es su empresa; si no, `404 NO_ENCONTRADO` «Cliente no encontrado». |
| `GET /api/v1/clientes/existe?ruc=` | `403 ACCESO_DENEGADO` (es un padrón de todas las empresas). |
| `POST` / `PUT` sobre despachos y clientes | `403 ACCESO_DENEGADO` (el portal es de solo lectura; ya era así). |
| `GET /api/v1/reportes/exportar` | `403` (ya era así). |
| Catálogos (`/catalogos/...`) y notificaciones (`/notificaciones`, `/usuarios/me/preferencias`) | Sin cambios: los catálogos son comunes y las notificaciones ya son del usuario de la sesión. |

Cada intento bloqueado (despacho de otra empresa, usuario sin empresa o borrador) queda en la tabla `acceso_denegado`; un despacho que no existe no se registra (no hay a quién atribuirlo y evita ruido por enlaces viejos).

Los errores usan el formato de siempre: `{ "codigo": "...", "mensaje": "...", "campos": { ... } }`.

---

## Portal del cliente (solo rol `CLIENTE`; `ADMIN` y `AGENTE` reciben `403`)

### GET `/api/v1/portal/mi-empresa`

Banner («Solo se muestran los despachos de …») y pie del menú lateral.

```json
200 { "clienteId": 1, "ruc": "20512345671", "razonSocial": "Importadora Andina S.A.C." }
200 { "clienteId": null, "ruc": null, "razonSocial": null }   // aún sin empresa asignada
```

Con `clienteId = null` la pantalla muestra «Aún no tiene una empresa asignada; contacte al administrador».

### GET `/api/v1/portal/despachos?numero=&estado=&etaDesde=&etaHasta=`

Tabla «Mis despachos». Todos los parámetros son opcionales y se combinan.

| Parámetro | Detalle |
|---|---|
| `numero` | Contiene el texto, sin distinguir mayúsculas (`0318` o `DESP-2026-0318`). |
| `estado` | Un valor de `EstadoDespacho` (`APERTURA`, `DOCUMENTACION_COMPLETA`, `CLASIFICADO`, `LIQUIDADO`, `TRIBUTOS_PAGADOS`, `DAM_NUMERADA`, `CANAL_ASIGNADO`, `INSPECCION_PROGRAMADA`, `INSPECCION_CONFORME`, `LEVANTE_OTORGADO`, `RETIRO_PROGRAMADO`). Los borradores nunca se listan. |
| `etaDesde`, `etaHasta` | `AAAA-MM-DD`, ambos extremos incluidos. Con rango, los despachos sin ETA no salen. |

```json
200 [ { "id": 318, "numero": "DESP-2026-0318", "numeroBl": "MSCU-BL-55120",
        "fechaEta": "2026-10-21", "estado": "INSPECCION_PROGRAMADA", "canalControl": "NARANJA",
        "ultimaActualizacion": "2026-11-05T18:40:00" },
      { "id": 355, "numero": "DESP-2026-0355", "numeroBl": null, "fechaEta": null,
        "estado": "DOCUMENTACION_COMPLETA", "canalControl": null,
        "ultimaActualizacion": "2026-11-04T09:12:00" } ]
```

Del más reciente al más antiguo. Lista vacía si no hay coincidencias o si el usuario no tiene empresa: para el mensaje rojo «Despacho no encontrado» del prototipo, mostrarlo cuando se buscó por `numero` y la lista viene vacía. `ultimaActualizacion` es la fecha del último hito.

```json
400 { "codigo": "VALIDACION", "mensaje": "Revise los campos marcados.",
      "campos": { "estado": "Estado no válido", "etaDesde": "Use el formato AAAA-MM-DD",
                  "etaHasta": "La fecha final no puede ser anterior a la inicial" } }
```

### Detalle, línea de tiempo y documentos

Se usan las rutas que ya existen; el filtro por empresa se aplica solo:

- `GET /api/v1/despachos/{id}` · `GET /api/v1/despachos/{id}/hitos` (stepper «Apertura … Levante»).
- `GET /api/v1/despachos/{id}/documentos` y `GET /api/v1/despachos/{id}/documentos/{documentoId}/archivo` (botón «Descargar»).
- Liquidación `GET /api/v1/despachos/{id}/liquidacion`, DAM `GET .../dam` y `.../dam/cargo`, canal, citas, levante `GET .../levante` y `.../levante/constancia`.

Un despacho ajeno responde:

```json
404 { "codigo": "NO_ENCONTRADO", "mensaje": "Despacho no encontrado", "campos": null }
```

---

## Administración (solo rol `ADMIN`; `AGENTE` y `CLIENTE` reciben `403 ACCESO_DENEGADO`)

### GET `/api/v1/administracion/usuarios-cliente`

Tabla «Usuarios cliente», ordenada por usuario. «Filtro: Todos» y «Buscar usuario o RUC» se hacen en la pantalla sobre esta lista.

```json
200 [ { "id": 5, "usuario": "jperez", "correo": "jperez@andina.pe", "nombre": "Jorge Pérez", "activo": true,
        "clienteId": 1, "ruc": "20512345671", "razonSocial": "Importadora Andina S.A.C.",
        "asignadoEn": "2026-11-05T20:15:00", "estado": "ASIGNADO" },
      { "id": 6, "usuario": "cliente", "correo": "cliente@aduanatrack.pe", "nombre": "Cliente Demo", "activo": true,
        "clienteId": null, "ruc": null, "razonSocial": null, "asignadoEn": null, "estado": "SIN_ASIGNAR" } ]
```

`estado`: `ASIGNADO` (botón «Cambiar») o `SIN_ASIGNAR` (fila resaltada, botón «Asignar»).

### GET `/api/v1/administracion/empresas-asignables`

Combo «Empresa importadora (RUC · razón social)»: empresas activas con RUC válido, por razón social («Solo se listan empresas registradas con RUC válido (HU-04)»).

```json
200 [ { "id": 1, "ruc": "20512345671", "razonSocial": "Importadora Andina S.A.C." } ]
```

### PUT `/api/v1/administracion/usuarios-cliente/{usuarioId}/empresa`

Asigna o cambia la empresa. Lleva la cabecera `X-XSRF-TOKEN`. La «Fecha de asignación» del formulario es solo informativa: la fija el servidor (`asignadoEn`).

```json
PUT { "clienteId": 1 }
200 { "id": 6, "usuario": "cliente", "correo": "cliente@aduanatrack.pe", "nombre": "Cliente Demo", "activo": true,
      "clienteId": 1, "ruc": "20512345671", "razonSocial": "Importadora Andina S.A.C.",
      "asignadoEn": "2026-11-11T10:30:00", "estado": "ASIGNADO" }
```

Errores:

```json
400 { "codigo": "VALIDACION", "mensaje": "Revise los campos marcados.", "campos": { "clienteId": "Campo obligatorio" } }
400 { ..., "campos": { "clienteId": "La empresa seleccionada no existe" } }                  // inexistente o inactiva
400 { ..., "campos": { "clienteId": "Solo se pueden asignar empresas con RUC válido" } }
400 { ..., "campos": { "usuarioId": "El usuario no tiene el rol Cliente" } }
404 { "codigo": "NO_ENCONTRADO", "mensaje": "No existe el usuario solicitado." }
```

Repetir la misma empresa devuelve `200` sin cambios. **Auditoría:** cada asignación deja filas en el historial de la empresa (`cliente_historial`, campo `usuarioAsignado`, con el administrador y la fecha): una en la empresa nueva (`valorNuevo` = usuario) y, si había otra, una en la anterior (`valorAnterior` = usuario). Se ven en `GET /api/v1/clientes/{id}/historial`.

### GET `/api/v1/administracion/accesos-denegados?pagina=0&tamano=20`

Tabla «Intentos de acceso denegados», del más reciente al más antiguo. `tamano` por defecto 20, máximo 50.

```json
200 { "contenido": [
        { "id": 1, "fecha": "2026-11-05T20:22:00", "usuario": "jperez",
          "despachoId": 20, "numeroDespacho": "DESP-2026-0342",
          "clienteId": 2, "empresaDespacho": "Pacífico Trade E.I.R.L.",
          "resultado": "OTRA_EMPRESA", "mensaje": "Despacho no encontrado" } ],
      "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1 }
```

`resultado`: `OTRA_EMPRESA`, `SIN_EMPRESA` (el usuario no tenía empresa) o `BORRADOR`. La etiqueta del prototipo se arma como «Bloqueado · «{mensaje}»». `numeroDespacho` puede venir `null` (despacho en borrador).

---

## Cambios en lo existente

- `usuario.cliente_id` y `usuario.cliente_asignado_en` (V017). El usuario demo `cliente` queda asignado a la empresa de menor id que exista al migrar; en una base sin empresas queda `SIN_ASIGNAR` hasta que el administrador la asigne o se cargue el script de demo.
- `GET /api/v1/auth/me` y el login **no cambian**. Para el banner de empresa usar `GET /api/v1/portal/mi-empresa`.
- `GET /api/v1/dashboard/*`: para `ADMIN` y `AGENTE` el parámetro `clienteId` sigue funcionando igual.
