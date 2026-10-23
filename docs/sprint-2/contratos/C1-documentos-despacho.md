# Contrato C1 · Documentos del despacho (HU-06)

Publica: Patrick (T-06.1 · SCRUM-53) · Implementan: Patrick (T-06.2 · SCRUM-54) y Fabio (T-06.3 · SCRUM-55) · Consume: Kiara (T-06.4 · SCRUM-56)

DTO en `backend/src/main/java/pe/edu/usmp/aduanatrack/documento/dto/`. Todas las rutas cuelgan de `/api/v1/despachos/{id}`; las consultas son para los tres perfiles y la carga solo para `ADMIN` y `AGENTE` (403 `ACCESO_DENEGADO` para `CLIENTE`). La carga lleva la cabecera `X-XSRF-TOKEN` como el resto de POST (el cliente `http.ts` ya lo hace).

## Catálogo de documentos (V006, a confirmar con el PO)

| `tipo` | Nombre | Obligatorio |
|---|---|---|
| `FACTURA_COMERCIAL` | Factura comercial | Sí |
| `CONOCIMIENTO_EMBARQUE` | Conocimiento de embarque (BL) | Sí |
| `LISTA_EMPAQUE` | Lista de empaque | Sí |
| `POLIZA_SEGURO` | Póliza de seguro | Sí |
| `CERTIFICADO_ORIGEN` | Certificado de origen | No |
| `OTROS` | Otros documentos | No |

## GET `/documentos`

Un elemento por tipo de documento activo, en el orden de la tabla anterior. `estadoGeneral` es `COMPLETO` cuando todos los obligatorios tienen versión vigente.

```json
200
{
  "estadoGeneral": "PENDIENTE",
  "documentos": [
    { "tipo": "FACTURA_COMERCIAL", "nombre": "Factura comercial", "obligatorio": true,
      "estado": "CARGADO", "version": 2, "documentoId": 41, "archivo": "factura.pdf",
      "tamanoBytes": 482113, "cargadoPor": "Agente Demo", "cargadoEn": "2026-10-22T10:15:00" },
    { "tipo": "POLIZA_SEGURO", "nombre": "Póliza de seguro", "obligatorio": true,
      "estado": "PENDIENTE", "version": null, "documentoId": null, "archivo": null,
      "tamanoBytes": null, "cargadoPor": null, "cargadoEn": null }
  ]
}
```

404 `NO_ENCONTRADO` si el despacho no existe.

## POST `/documentos/{tipo}` (multipart: `archivo`)

Carga la primera versión o reemplaza la vigente. La versión anterior queda con `vigente = false` y se conserva (CA3).

```json
201 { "documentoId": 42, "tipo": "FACTURA_COMERCIAL", "version": 3, "estado": "CARGADO" }
```

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos.archivo` | No se envió archivo o está vacío |
| 404 `NO_ENCONTRADO` | El despacho o el tipo de documento no existen |
| 409 `DESPACHO_EN_BORRADOR` | El despacho todavía es borrador |
| 413 `ARCHIVO_MUY_GRANDE` | El archivo pasa de 10 MB |
| 415 `FORMATO_NO_PERMITIDO` | El contenido no empieza con `%PDF-` (p. ej. un .docx renombrado) |

> **Cambio respecto a la guía (avisado a Kiara):** la guía proponía `400 ARCHIVO_NO_PDF` y `400 ARCHIVO_EXCEDE_10MB`. Se usan los mismos códigos que la carta poder (HU-04) porque un archivo de más de 10 MB lo rechaza Spring antes de llegar al servicio con `413 ARCHIVO_MUY_GRANDE` (`GlobalExceptionHandler`). Así el frontend trata igual las dos cargas de PDF.

## GET `/documentos/{tipo}/versiones`

Todas las versiones del tipo, de la más reciente a la más antigua. Lista vacía si nunca se cargó.

```json
200
[
  { "documentoId": 42, "version": 3, "vigente": true,  "archivo": "factura-v3.pdf", "tamanoBytes": 501223,
    "cargadoPor": "Agente Demo", "cargadoEn": "2026-10-23T09:40:00" },
  { "documentoId": 41, "version": 2, "vigente": false, "archivo": "factura.pdf",    "tamanoBytes": 482113,
    "cargadoPor": "Agente Demo", "cargadoEn": "2026-10-22T10:15:00" }
]
```

## GET `/documentos/{documentoId}/archivo`

Descarga cualquier versión (vigente o histórica) como `application/pdf` con `Content-Disposition: attachment` y el nombre original. 404 si el documento es de otro despacho o el archivo no está en el volumen.

## Bloqueo usado por HU-07 (T-06.3 · Fabio)

```json
409 { "codigo": "DOCUMENTACION_INCOMPLETA", "mensaje": "…", "pendientes": ["POLIZA_SEGURO"] }
```
