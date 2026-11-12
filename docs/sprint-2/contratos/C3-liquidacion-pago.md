# Contrato C3 · Liquidación y pago (HU-08)

Publica: Fabio (T-08.1 · SCRUM-63) · Implementa: Fabio (cálculo) y Patrick (T-08.2 · SCRUM-64, rutas, estados y tablas) · Consume: Kiara (T-08.3 · SCRUM-65).

DTO en `backend/src/main/java/pe/edu/usmp/aduanatrack/liquidacion/dto/`. Rutas bajo `/api/v1/despachos/{id}`; solo `ADMIN` y `AGENTE` escriben, con la cabecera `X-XSRF-TOKEN`. Todos los montos son USD con 2 decimales.

## Cálculo (T-08.1)

`CalculoTributosService.calcular(List<ItemCalculo> items, BigDecimal flete, BigDecimal seguro)` devuelve un `ResultadoCalculo`. Cada `ItemCalculo` lleva el `valorFob` del ítem y la `tasaAdValorem` de su partida (porcentaje; `PartidaReferencial.getTasaAdValorem()`). No toca la base de datos salvo para leer las tasas de `parametro_sistema`.

| Concepto | Fórmula (HALF_UP a 2 decimales en cada concepto) |
|---|---|
| CIF | FOB + flete + seguro |
| Ad Valorem | suma por ítem de CIF del ítem × tasa de su partida |
| IGV | (CIF + Ad Valorem) × `TASA_IGV` |
| IPM | (CIF + Ad Valorem) × `TASA_IPM` |
| Percepción | (CIF + Ad Valorem + IGV + IPM) × `TASA_PERCEPCION` |
| Total | Ad Valorem + IGV + IPM + percepción (los tributos, sin el CIF) |

El flete y el seguro se reparten entre los ítems según su FOB; la diferencia de redondeo la absorbe el último ítem para que el reparto sume exactamente el monto.

Tasas en `parametro_sistema` (V008, valores de referencia que confirma el PO): `TASA_IGV` = 16, `TASA_IPM` = 2, `TASA_PERCEPCION` = 3.5. `ParametroService.decimal(clave)` las lee.

`V008__tasas_liquidacion.sql` solo trae las tasas. **Patrick:** agrega en ese mismo archivo (después de mergear esta rama) las tablas `liquidacion` y `pago_tributos`, para respetar el V008 reservado en la guía.

Caso calculado a mano (una partida al 6 %): FOB 10 000,00 + flete 800,00 + seguro 150,00 = CIF 10 950,00 → Ad Valorem 657,00 → IGV 1 857,12 → IPM 232,14 → percepción 479,37 → **total 3 225,63**.

## GET `/liquidacion`

Para los tres perfiles. Devuelve la liquidación guardada y el pago registrado, para volver a verlos después de generarla (también con el despacho ya pagado). Los campos vienen `null` mientras no existan; `liquidacion.estado` es el estado actual del despacho.

```json
GET
200 { "liquidacion": { "fob": 10000.00, "flete": 800.00, "seguro": 150.00, "cif": 10950.00,
                       "adValorem": 657.00, "igv": 1857.12, "ipm": 232.14, "percepcion": 479.37,
                       "total": 3225.63, "moneda": "USD",
                       "tasas": { "igv": 16, "ipm": 2, "percepcion": 3.5 }, "estado": "TRIBUTOS_PAGADOS" },
      "generadoPor": "Agente Demo", "generadoEn": "2026-10-28T10:00:00",
      "pago": { "fecha": "2026-10-28", "monto": 3225.63, "medioPago": "VENTANILLA",
                "numeroOperacion": "00123456", "registradoPor": "Agente Demo",
                "registradoEn": "2026-10-28T11:00:00" } }
```

Sin liquidación: `200 { "liquidacion": null, "generadoPor": null, "generadoEn": null, "pago": null }`. 404 `NO_ENCONTRADO` si el despacho no existe.

## POST `/liquidacion`

Exige `CLASIFICADO`; pasa el despacho a `LIQUIDADO` y registra el hito. Volver a generarla reemplaza la anterior mientras no haya pago.

```json
POST { "flete": 800.00, "seguro": 150.00 }
201 { "fob": 10000.00, "flete": 800.00, "seguro": 150.00, "cif": 10950.00,
      "adValorem": 657.00, "igv": 1857.12, "ipm": 232.14,
      "percepcion": 479.37, "total": 3225.63, "moneda": "USD",
      "tasas": { "igv": 16, "ipm": 2, "percepcion": 3.5 },
      "estado": "LIQUIDADO" }
```

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos` (`flete`, `seguro`) | Falta, es negativo o tiene más de 2 decimales |
| 404 `NO_ENCONTRADO` | El despacho no existe |
| 409 `DESPACHO_NO_CLASIFICADO` | El despacho no está en `CLASIFICADO` (ni en `LIQUIDADO` sin pago) |
| 409 `LIQUIDACION_PENDIENTE` | Se intenta pagar sin liquidación (ver abajo) |

## POST `/pago`

Registra un pago hecho fuera del sistema. Exige `LIQUIDADO`; pasa a `TRIBUTOS_PAGADOS` y registra el hito.

```json
POST { "fecha": "2026-10-28", "monto": 3225.63,
       "medioPago": "TRANSFERENCIA", "numeroOperacion": "00123456" }
201 { "estado": "TRIBUTOS_PAGADOS" }
```

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos` | Fecha, monto (> 0, 2 decimales), medio de pago o número de operación vacíos |
| 409 `LIQUIDACION_PENDIENTE` | El despacho no está en `LIQUIDADO` |

DTO: `LiquidacionRequest`, `LiquidacionResponse`, `LiquidacionConsultaResponse`, `PagoRequest` y `PagoResponse`. `estado` va como texto para no depender del enum del despacho.
