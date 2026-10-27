# Contrato C2 · Buscador de partida arancelaria (HU-07)

Parte de partidas del contrato C2. Publica e implementa: Fabio (T-07.1 · SCRUM-58) · Consume: Kiara (T-07.4 · SCRUM-61) y Fabio (T-08.1, tasa Ad Valorem por partida).

La parte de ítems (`GET/PUT /despachos/{id}/items`) es de Patrick (`C2-items-factura.md`). `POST /clasificacion` es de Fabio (T-07.3).

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/clasificacion/`. Migración `V007__partidas_e_items.sql` (crea `partida_referencial` con 20 partidas de la demo e `item_factura`). Es un catálogo interno referencial: las tasas son de ejemplo, las valida el PO, y no se conecta al Arancel de SUNAT.

## GET `/api/v1/catalogos/partidas?q=`

Para los tres perfiles. Máximo 20 resultados, ordenados por código.

```json
GET /api/v1/catalogos/partidas?q=rodam
200
[ { "id": 1, "codigo": "8482100000", "codigoFormateado": "8482.10.00.00",
    "descripcion": "Rodamientos de bolas", "tasaAdValorem": 6.00 } ]
```

- `tasaAdValorem` es un porcentaje: `6.00` es 6 %.
- La búsqueda no distingue mayúsculas ni tildes: `rodamiento`, `RODAMIENTO` y `cónicos`/`conicos` dan el mismo resultado.
- Varias palabras se buscan en orden dentro de la descripción (`rodamientos bolas`).
- Un texto de solo dígitos, con o sin puntos (`8482`, `8482.10`), busca también por el principio del código.
- `q` vacío o ausente responde `200 []`.

## Tabla `item_factura` (V007, para T-07.2)

| Columna | Tipo | Nota |
|---|---|---|
| `id` | identidad | |
| `despacho_id` | FK `despacho` | |
| `numero` | entero > 0 | único por despacho (restricción diferida, para renumerar en una transacción) |
| `descripcion` | texto 500 | |
| `cantidad` | `NUMERIC(15,3)` > 0 | |
| `unidad` | texto 10 | |
| `valor_fob` | `NUMERIC(15,2)` > 0 | USD |
| `partida_id` | FK `partida_referencial`, nulo | |
| `clasificado_por` | FK `usuario`, nulo | |
| `clasificado_en` | timestamp, nulo | |

Entidad de partidas para T-07.2: `clasificacion.entity.PartidaReferencial` (`id`, `codigo`, `codigoFormateado`, `descripcion`, `tasaAdValorem`).
