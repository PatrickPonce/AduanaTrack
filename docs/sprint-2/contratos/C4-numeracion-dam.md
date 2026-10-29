# Contrato C4 · Numeración de la DAM (HU-09)

Publica e implementa: Patrick (T-09.1 · SCRUM-67) · Consume: Kiara (T-09.2 · SCRUM-68).

Rutas bajo `/api/v1/despachos/{id}/dam`. La consulta es para los tres perfiles y el POST solo para `ADMIN` y `AGENTE`, con la cabecera `X-XSRF-TOKEN`. La DAM se numera en el sistema externo de SUNAT; aquí solo se deja constancia del número.

## GET `/dam`

Siempre responde 200. Mientras no se registra, `numeroDam`, `fechaNumeracion` y `nombreCargo` vienen `null`.

```json
200 { "numeroDam": "118-2026-10-123456", "fechaNumeracion": "2026-10-27",
      "nombreCargo": "Cargo DAM.pdf", "estado": "DAM_NUMERADA" }
```

## POST `/dam` (multipart/form-data)

| Parte | Obligatoria | Regla |
|---|---|---|
| `numeroDam` | Sí | Hasta 30 caracteres: letras, números, guiones y barras. Se guardan sin espacios en los extremos |
| `fechaNumeracion` | No | `AAAA-MM-DD`, no futura. Si falta, se usa la fecha de hoy |
| `cargo` | No | PDF de hasta 10 MB (misma validación que los documentos del despacho) |

Exige el estado `TRIBUTOS_PAGADOS`; pasa el despacho a `DAM_NUMERADA` y registra el hito.

```
201 { "numeroDam": "118-2026-10-123456", "fechaNumeracion": "2026-10-27",
      "nombreCargo": "Cargo DAM.pdf", "estado": "DAM_NUMERADA" }
```

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos.numeroDam` | Vacío, más de 30 caracteres o con caracteres no permitidos (CA2) |
| 400 `VALIDACION` con `campos.fechaNumeracion` | Fecha futura o con formato distinto de `AAAA-MM-DD` |
| 404 `NO_ENCONTRADO` | El despacho no existe |
| 409 `TRIBUTOS_PENDIENTES` | El despacho no está en `TRIBUTOS_PAGADOS`, o la DAM ya fue registrada |
| 409 `DAM_DUPLICADA` | Otro despacho ya tiene ese N° de DAM |
| 413 `ARCHIVO_MUY_GRANDE` | El cargo pasa de 10 MB |
| 415 `FORMATO_NO_PERMITIDO` | El cargo no es un PDF |

## GET `/dam/cargo`

Descarga el PDF del cargo con su nombre original. 404 `NO_ENCONTRADO` si el despacho no existe o no se adjuntó cargo.

## Base de datos

`V009__numeracion_dam.sql` agrega a `despacho`: `numero_dam` (único), `fecha_numeracion`, `cargo_nombre` y `cargo_ruta` (relativa al volumen `documentos`, `{despachoId}/cargo-dam/{uuid}.pdf`).
