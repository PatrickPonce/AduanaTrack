# Contrato C7 · Resultado de la inspección aduanera (HU-12)

Publica e implementa: Fabio (T-12.1 y T-12.2) · Consume: Kiara (T-12.3, pestaña «Inspección», ya hecha en `frontend/src/api/inspeccion.ts`).

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/inspeccion/` (`InspeccionController`, `ItemsInspeccionController`, `InspeccionService`, DTO `InspeccionRequest` e `InspeccionResponse`). Migración **V016** con las tablas `inspeccion_despacho` e `inspeccion_item`: V013 no se usa porque en Neon ya corrieron V014 y V015 y Flyway rechaza una versión menor.

Cada inspección queda guardada: si una termina con observaciones y se repite, el historial conserva ambas. El `GET` devuelve la última.

## GET `/api/v1/catalogos/items-inspeccion`

Cualquier usuario autenticado. Catálogo fijo, en el orden en que se muestra el checklist (lo confirma el PO):

```json
200 [ "Cantidad de bultos", "Peso y volumen declarados", "Descripción de la mercancía",
      "Estado y embalaje de la mercancía", "Rotulado y marcas", "Precintos y contenedor" ]
```

## POST `/api/v1/despachos/{id}/inspeccion`

Solo `ADMIN` y `AGENTE`, con la cabecera `X-XSRF-TOKEN`. Es `multipart/form-data` con dos partes:

- `datos`: JSON (la parte debe llevar `Content-Type: application/json`).
- `acta`: archivo PDF de hasta 10 MB, opcional.

```json
datos = { "items": [
            { "descripcion": "Cantidad de bultos", "conforme": true, "observacion": null },
            { "descripcion": "Rotulado y marcas", "conforme": false, "observacion": "Rotulado incompleto" } ],
          "observaciones": "Se reinspecciona el viernes" }
```

`items` debe traer **todos** los ítems del catálogo, sin repetir ni agregar otros (el orden lo fija el catálogo). `observacion` es obligatoria (máx. 255) en el ítem no conforme y se ignora en el conforme. `observaciones` es opcional (máx. 500).

```json
201 { "resultado": "CON_OBSERVACIONES", "estadoDespacho": "INSPECCION_PROGRAMADA",
      "levanteHabilitado": false, "actaNombre": "acta.pdf",
      "observaciones": "Se reinspecciona el viernes",
      "items": [ { "descripcion": "Cantidad de bultos", "conforme": true, "observacion": null } ],
      "registradaPor": "Agente Demo", "registradaEn": "2026-11-12T10:30:00" }
```

Reglas:

- **Resultado** (CA1): lo calcula el sistema, no el cliente: `CONFORME` si todos los ítems son conformes y `CON_OBSERVACIONES` si alguno no lo es.
- **Con observaciones** (CA2): registra el hito `INSPECCION_CON_OBSERVACIONES` y avisa al agente responsable (notificación `INSPECCION_CON_OBSERVACIONES`, severidad `ALTA`, contrato C9). **El estado no cambia**: el despacho sigue en `INSPECCION_PROGRAMADA` y se puede registrar otra cita de inspección (C6) y una nueva inspección.
- **Conforme** (CA3): pasa el despacho a `INSPECCION_CONFORME`, registra el hito del mismo nombre y avisa (`INSPECCION_CONFORME`, severidad `INFO`). `levanteHabilitado` es `true` desde ese estado y el levante (C8) deja de tener la inspección como requisito pendiente.
- **Acta** (CA4): PDF opcional, validado por su firma (`%PDF`) y de máximo 10 MB, guardado en `{documentos}/{despachoId}/acta-inspeccion/{uuid}.pdf`.

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos` | Faltan ítems o sobran, hay uno repetido, un ítem no conforme sin observación, o un texto pasa del máximo (`item-{posición}`, `items`, `observaciones`) |
| 403 `ACCESO_DENEGADO` | Perfil `CLIENTE` |
| 404 `NO_ENCONTRADO` | El despacho no existe |
| 409 `INSPECCION_NO_PROGRAMADA` | El despacho no está en `INSPECCION_PROGRAMADA` (borrador, sin cita de inspección, o ya con inspección conforme) |
| 413 `ARCHIVO_MUY_GRANDE` / 415 `FORMATO_NO_PERMITIDO` | El acta pasa de 10 MB o no es un PDF |

## GET `/api/v1/despachos/{id}/inspeccion`

Cualquier usuario autenticado. Última inspección, con la misma forma que el POST; `levanteHabilitado` y `estadoDespacho` reflejan el estado actual del despacho. 404 `NO_ENCONTRADO` si el despacho no existe o todavía no tiene ninguna inspección.

## GET `/api/v1/despachos/{id}/inspeccion/acta`

Descarga el acta de la última inspección con `Content-Disposition: attachment`. 404 si no tiene acta.
