# Contrato C6 · Citas en depósito temporal o almacén (HU-11)

Publica e implementa: Patrick (T-11.1 · SCRUM-78 y T-11.2 · SCRUM-79) · Consume: Kiara (T-11.3 · SCRUM-80).

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/cita/` (`CitaDespachoController`, `DepositoController`, `CitaDespachoService`, DTO `CitaRequest`, `CitaResponse`, `TransportistaRequest` y `DepositoResponse`). Migración **V012** (`deposito` y `cita_despacho`): la guía reservaba V011, pero esa versión ya la ocupa el Arancel de Aduanas 2022 en `develop`.

## GET `/api/v1/catalogos/depositos`

Cualquier usuario autenticado. Solo los depósitos activos, por código. Hay 5 de ejemplo (`DT-01` a `DT-03` y `AL-01` y `AL-02`) hasta que el PO confirme el catálogo real.

```json
200 [ { "id": 1, "codigo": "DT-01", "nombre": "Depósito temporal 1 (ejemplo)",
        "tipo": "DEPOSITO_TEMPORAL" } ]          // tipo: DEPOSITO_TEMPORAL | ALMACEN
```

## POST `/api/v1/despachos/{id}/citas`

Solo `ADMIN` y `AGENTE`, con la cabecera `X-XSRF-TOKEN`. Cuerpo JSON:

```json
{
  "depositoId": 1,
  "numeroCita": "TK-2026-004512",
  "tipo": "INSPECCION",                 // o "RETIRO"
  "fechaHoraConfirmada": "2026-11-12T09:30:00",
  "transportista": null                 // obligatorio solo en RETIRO:
  // { "ruc": "20123456786", "razonSocial": "Transportes Demo SAC",
  //   "placa": "ABC-123", "conductor": "Juan Pérez" }
}
```

```json
201 { "id": 7, "depositoId": 1, "deposito": "Depósito temporal 1 (ejemplo)",
      "numeroCita": "TK-2026-004512", "tipo": "INSPECCION",
      "fechaHoraConfirmada": "2026-11-12T09:30:00", "estado": "PROGRAMADA",
      "transportista": null, "registradaPor": "Agente Demo",
      "registradaEn": "2026-11-04T10:00:00", "estadoDespacho": "INSPECCION_PROGRAMADA" }
```

Reglas:

- **Inspección** (CA2): exige el despacho en `CANAL_ASIGNADO` con canal naranja o rojo y lo pasa a `INSPECCION_PROGRAMADA`. Si la inspección tuvo observaciones (el despacho sigue en `INSPECCION_PROGRAMADA`) se puede registrar otra cita.
- **Retiro** (CA3): exige que el despacho haya llegado a `LEVANTE_OTORGADO` y los cuatro datos del transportista (el RUC se valida con el dígito verificador y la placa se guarda en mayúsculas); lo pasa a `RETIRO_PROGRAMADO`. Una segunda cita de retiro no repite el cambio de estado.
- Cada cita registra el hito `CITA_REGISTRADA`; el cambio de estado registra además su propio hito (`INSPECCION_PROGRAMADA` o `RETIRO_PROGRAMADO`).
- El número de cita admite hasta 30 caracteres.

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos` | Falta un dato obligatorio (CA4), por ejemplo `campos.fechaHoraConfirmada`; en una cita de retiro, los campos del transportista llegan como `transportista.ruc`, `transportista.razonSocial`, `transportista.placa` y `transportista.conductor` |
| 403 `ACCESO_DENEGADO` | Perfil `CLIENTE` |
| 404 `NO_ENCONTRADO` | El despacho no existe |
| 409 `DESPACHO_EN_BORRADOR` | El despacho todavía es borrador |
| 409 `CITA_INSPECCION_NO_APLICA` | No tiene canal asignado, el canal es verde o la inspección ya fue conforme |
| 409 `CITA_RETIRO_SIN_LEVANTE` | El despacho todavía no tiene el levante otorgado |

## GET `/api/v1/despachos/{id}/citas`

Cualquier usuario autenticado. Lista ordenada por `fechaHoraConfirmada` (y por `id` si coinciden). Cada fila tiene la misma forma que la respuesta del POST; `estadoDespacho` es el estado actual del despacho.

## Estados nuevos del despacho

T-11.1 agrega juntos y en este orden, a continuación de `CANAL_ASIGNADO`: `INSPECCION_PROGRAMADA`, `INSPECCION_CONFORME`, `LEVANTE_OTORGADO` y `RETIRO_PROGRAMADO`. Los tipos de hito `INSPECCION_PROGRAMADA`, `INSPECCION_CONFORME`, `LEVANTE_OTORGADO`, `RETIRO_PROGRAMADO`, `CITA_REGISTRADA`, `INSPECCION_CON_OBSERVACIONES` y `LEVANTE_CON_ADVERTENCIA` también quedan disponibles. En el frontend se agregan en `utils/estados.ts`, `EstadoDespachoChip.tsx` y el tipo `EstadoDespacho`.
