# Contrato C8 · Levante de la mercancía (HU-13)

Publica e implementa: Patrick (T-13.1 · SCRUM-86 y T-13.2 · SCRUM-87) · Consume: Kiara (T-13.3 · SCRUM-88).

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/levante/` (`LevanteController`, `LevanteService`, DTO `LevanteRequest` y `LevanteResponse`). Migración **V014** con las columnas del levante en `despacho`: la guía reservaba V013, pero la numeración se corrió una posición porque V011 la ocupa el Arancel de Aduanas 2022 en `develop`. `despacho.fecha_levante` ya existía desde V001.

## POST `/api/v1/despachos/{id}/levante`

Solo `ADMIN` y `AGENTE`, con la cabecera `X-XSRF-TOKEN`. Es `multipart/form-data` con dos partes:

- `datos`: JSON (la parte debe llevar `Content-Type: application/json`).
- `constancia`: archivo PDF de hasta 10 MB, opcional.

```json
datos = { "numeroLevante": "LEV-2026-000318", "fechaLevante": "2026-11-13",
          "confirmarSinRequisitos": false }
```

```json
201 { "numeroLevante": "LEV-2026-000318", "fechaLevante": "2026-11-13",
      "constanciaNombre": "constancia.pdf", "conAdvertencia": false,
      "retiroHabilitado": true, "estadoDespacho": "LEVANTE_OTORGADO" }

409 { "codigo": "LEVANTE_REQUISITOS_PENDIENTES",
      "mensaje": "Faltan requisitos previos al levante: pago de tributos, inspección conforme.",
      "pendientes": ["Pago de tributos", "Inspección conforme"] }
    // CA2: la pantalla pregunta y reenvía con "confirmarSinRequisitos": true
```

Reglas:

- **Registro** (CA1): guarda número y fecha, llena `fecha_levante`, pasa el despacho a `LEVANTE_OTORGADO` y registra el hito `LEVANTE_OTORGADO`. El indicador «Con levante registrado» del Dashboard cuenta `fecha_levante`, así que se actualiza solo.
- **Requisitos previos** (CA2): el pago de tributos (el despacho alcanzó `TRIBUTOS_PAGADOS`) y la inspección conforme (canal verde o `INSPECCION_CONFORME`). Si falta alguno y no llega `confirmarSinRequisitos = true`, responde 409 con `pendientes`; con la confirmación guarda con `conAdvertencia = true` y registra además el hito `LEVANTE_CON_ADVERTENCIA`. El sistema advierte pero no bloquea.
- **Constancia** (CA3): PDF opcional, validado por su firma (`%PDF`) y de máximo 10 MB, guardado en `{documentos}/{despachoId}/constancia-levante/{uuid}.pdf`.
- **Retiro** (CA4): `retiroHabilitado` es `true` desde `LEVANTE_OTORGADO`, y el servicio de citas (contrato C6) acepta las de tipo `RETIRO` solo a partir de ese estado.
- `fechaLevante` no puede ser futura. `numeroLevante`: hasta 30 caracteres, letras, números, guiones y barras; es único entre despachos.

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos` | Falta o no es válido `numeroLevante` o `fechaLevante` |
| 403 `ACCESO_DENEGADO` | Perfil `CLIENTE` |
| 404 `NO_ENCONTRADO` | El despacho no existe |
| 409 `DESPACHO_EN_BORRADOR` | El despacho todavía es borrador |
| 409 `LEVANTE_YA_REGISTRADO` | El despacho ya tiene el levante registrado |
| 409 `LEVANTE_REQUISITOS_PENDIENTES` | Faltan requisitos y no hay confirmación; trae `pendientes` |
| 409 `LEVANTE_DUPLICADO` | El número ya está registrado en otro despacho |
| 413 `ARCHIVO_MUY_GRANDE` / 415 `FORMATO_NO_PERMITIDO` | La constancia pasa de 10 MB o no es un PDF |

## GET `/api/v1/despachos/{id}/levante`

Cualquier usuario autenticado. Siempre responde 200 con la misma forma que el POST; `numeroLevante`, `fechaLevante` y `constanciaNombre` vienen `null` mientras el despacho no tiene levante, y `retiroHabilitado` es `false`.

## GET `/api/v1/despachos/{id}/levante/constancia`

Descarga el PDF con `Content-Disposition: attachment`. 404 si el levante no tiene constancia.
