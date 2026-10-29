# Contrato C5 · Canal de control (HU-10)

Parte del canal del contrato C5. Publica e implementa: Fabio (T-10.2 · SCRUM-70) · Consume: Kiara (T-10.3 · SCRUM-71). La línea de tiempo (`GET /hitos`) es de Patrick (T-10.1).

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/despacho/` (`CanalControlController`, `CanalControlService`, DTO `CanalRequest` y `CanalResponse`, enum `CanalControl`). No hace falta migración: `despacho.canal_control` ya existe desde V001, así que no se usó V010.

## PUT `/api/v1/despachos/{id}/canal`

Solo `ADMIN` y `AGENTE`, con la cabecera `X-XSRF-TOKEN`. Guarda el canal que informó SUNAT.

```json
PUT { "canal": "ROJO" }          // VERDE | NARANJA | ROJO
200 { "canal": "ROJO", "estado": "CANAL_ASIGNADO",
      "levanteHabilitado": false,
      "hitoSugerido": "Programar inspección física" }
```

| Canal | `levanteHabilitado` | `hitoSugerido` |
|---|---|---|
| `VERDE` | `true` | `null` |
| `NARANJA` | `false` | `null` |
| `ROJO` | `false` | «Programar inspección física» |

Reglas:

- Exige que el despacho esté en `DAM_NUMERADA`. Una vez en `CANAL_ASIGNADO` el canal se puede corregir.
- Pasa el despacho a `CANAL_ASIGNADO` y registra el hito `CANAL_ASIGNADO` («Canal de control asignado: NARANJA» la primera vez; «Canal de control cambiado de NARANJA a ROJO» después). Guardar el mismo canal otra vez no repite el hito.
- En `ROJO` agrega además el hito sugerido (`sugerido: true` en `GET /hitos`), una sola vez por cambio a rojo.
- El canal queda en `canalControl` de `DespachoResponse` y de las filas del Dashboard, para el chip de la cabecera.

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos.canal` | Falta o no es `VERDE`, `NARANJA` o `ROJO` |
| 403 `ACCESO_DENEGADO` | Perfil `CLIENTE` |
| 404 `NO_ENCONTRADO` | El despacho no existe |
| 409 `DAM_NO_NUMERADA` | El despacho no está en `DAM_NUMERADA` ni en `CANAL_ASIGNADO` |

## Dashboard

El indicador «En canal naranja/rojo» (`enCanalNaranjaRojo`) ya cuenta los despachos con `canal_control` `NARANJA` o `ROJO` (`TableroRepository.indicadores`, desde HU-02); no cambió.
