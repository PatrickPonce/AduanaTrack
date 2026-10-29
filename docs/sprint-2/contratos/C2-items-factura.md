# Contrato C2 · Ítems de la factura comercial (HU-07)

Parte de ítems del contrato C2. Publica: Patrick (24/10) · Implementa: Patrick (T-07.2 · SCRUM-59) · Consume: Kiara (T-07.4 · SCRUM-61) y Fabio (T-07.3 · SCRUM-60, guardar clasificación).

El buscador de partidas (`GET /catalogos/partidas?q=`) y `POST /clasificacion` son la parte de Fabio del mismo contrato. La tabla `item_factura` la crea Fabio en V007 (T-07.1).

DTO en `backend/src/main/java/pe/edu/usmp/aduanatrack/clasificacion/dto/`. Rutas bajo `/api/v1/despachos/{id}`; la consulta es para los tres perfiles y el PUT solo para `ADMIN` y `AGENTE`, con la cabecera `X-XSRF-TOKEN`.

## GET `/items`

Ítems del despacho ordenados por `numero`. Lista vacía si todavía no se registraron.

```json
200
[
  { "id": 10, "numero": 1, "descripcion": "Rodamiento 6205", "cantidad": 200, "unidad": "UND",
    "valorFob": 4200.00, "partidaId": 1,
    "partida": { "id": 1, "codigoFormateado": "8482.10.00.00", "descripcion": "Rodamientos de bolas" } },
  { "id": 11, "numero": 2, "descripcion": "Filtro de aceite", "cantidad": 150, "unidad": "UND",
    "valorFob": 5800.00, "partidaId": null, "partida": null }
]
```

## PUT `/items` (lista completa)

El frontend manda siempre la tabla entera, en el orden en que se muestra:

- fila con `id`: se actualiza;
- fila sin `id`: se crea;
- ítem guardado que no viene en la lista: se quita.

`numero` lo asigna el backend según la posición (1, 2, 3…). `partidaId` puede ser `null` mientras se clasifica; al asignarla se guardan también `clasificado_por` y `clasificado_en` del ítem (CA2).

```json
PUT
[
  { "id": 10, "descripcion": "Rodamiento 6205", "cantidad": 200, "unidad": "UND", "valorFob": 4200.00, "partidaId": 1 },
  { "id": 11, "descripcion": "Filtro de aceite", "cantidad": 150, "unidad": "UND", "valorFob": 5800.00, "partidaId": null },
  { "descripcion": "Correa de distribución", "cantidad": 80, "unidad": "UND", "valorFob": 950.00, "partidaId": null }
]
```

`200` con la lista guardada, en la misma forma del GET.

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos` por fila (`items[2].valorFob`) | Descripción o unidad vacías, cantidad o valor FOB ≤ 0, más de 2 decimales en el FOB |
| 400 `VALIDACION` con `campos["items[i].partidaId"]` | La partida no existe en el catálogo |
| 404 `NO_ENCONTRADO` | El despacho no existe, o un `id` no es de este despacho |
| 409 `DESPACHO_EN_BORRADOR` | El despacho todavía es borrador |

Reglas de los campos: `descripcion` hasta 500 caracteres; `cantidad` > 0 con hasta 3 decimales; `unidad` hasta 10 caracteres (`UND`, `KG`, `M`…); `valorFob` en USD > 0 con 2 decimales.

> **Agregado respecto a la guía:** cada ítem trae `partida` (`id`, `codigoFormateado`, `descripcion`) además de `partidaId`, para que el Autocomplete muestre la partida ya asignada sin consultar el buscador.
