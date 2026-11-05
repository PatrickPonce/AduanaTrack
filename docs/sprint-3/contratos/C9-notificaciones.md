# Contrato C9 · Notificaciones (HU-14)

Publica e implementa: Fabio (T-14.1 · SCRUM-90) · Consumen: Fabio (T-14.3 · SCRUM-92, centro de notificaciones) y Patrick (T-14.2, alertas que llaman al servicio).

Código en `backend/src/main/java/pe/edu/usmp/aduanatrack/notificacion/` (`NotificacionController`, `PreferenciasController`, `NotificacionService`, DTO `NotificacionResponse`, `CantidadNoLeidasResponse`, `PreferenciasRequest` y `PreferenciasResponse`). Migración **V015** con la tabla `notificacion` y la columna `usuario.canal_notificacion`: la guía reservaba V014, pero la numeración se corrió una posición porque V011 la ocupa el Arancel de Aduanas 2022 en `develop`.

Todas las rutas sirven a **cualquier usuario autenticado** (`ADMIN`, `AGENTE` y `CLIENTE`) y solo trabajan con las notificaciones del usuario de la sesión. `PATCH` y `PUT` llevan la cabecera `X-XSRF-TOKEN`.

## Cómo avisar desde otro servicio (backend)

```java
notificacionService.notificar(usuarioId, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
        "Inspección con observaciones", "DESP-2026-0001: rotulado incompleto", despachoId);

// Atajo: avisa al responsable del despacho, que es despacho.agente.
notificacionService.notificarAgente(despacho, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
        "Inspección con observaciones", "DESP-2026-0001: rotulado incompleto");
```

- `despachoId` es opcional (`null` en avisos que no son de un despacho, como una carta poder por vencer).
- Corre dentro de la transacción de quien llama: si el cambio de estado se revierte, el aviso también.
- `titulo` (150) y `mensaje` (500) se recortan con «…» si se pasan; vacíos lanzan `IllegalArgumentException`.
- `severidad`: `CRITICA`, `ALTA` o `INFO`. `tipo` (enum `TipoNotificacion`): `INSPECCION_CON_OBSERVACIONES`, `INSPECCION_CONFORME`, `PLAZO_ALMACENAJE_POR_VENCER`, `CARTA_PODER_POR_VENCER` y `CAMBIO_ESTADO`. Un evento nuevo se agrega al enum.
- La notificación siempre queda en la bandeja, aunque el usuario prefiera `CORREO`. El envío por correo se suma cuando exista `CorreoService` (T-15.3).

## GET `/api/v1/notificaciones?estado=NO_LEIDAS&pagina=0&tamano=20`

`estado`: `NO_LEIDAS` o `TODAS` (por defecto). `pagina` empieza en 0; `tamano` por defecto 20, máximo 50. Ordena de la más reciente a la más antigua.

```json
200 { "contenido": [
        { "id": 41, "tipo": "INSPECCION_CON_OBSERVACIONES", "severidad": "ALTA",
          "titulo": "Inspección con observaciones",
          "mensaje": "DESP-2026-0001: rotulado incompleto",
          "despachoId": 5, "numeroDespacho": "DESP-2026-0001",
          "leida": false, "creadaEn": "2026-11-06T09:00:00", "leidaEn": null } ],
      "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1 }
```

`despachoId` y `numeroDespacho` vienen `null` cuando el aviso no es de un despacho; con ellos la pantalla arma el enlace a `/despachos/{despachoId}`.

## GET `/api/v1/notificaciones/no-leidas/cantidad`

Contador del Badge de la campana (la pantalla lo consulta cada 60 s).

```json
200 { "cantidad": 3 }
```

## PATCH `/api/v1/notificaciones/{id}/leida`

Sin cuerpo. Marca la notificación como leída (CA4) y responde la notificación con `leida = true` y `leidaEn`. Es idempotente: repetirla no cambia `leidaEn`.

## GET y PUT `/api/v1/usuarios/me/preferencias`

```json
PUT { "canalNotificacion": "AMBOS" }
200 { "canalNotificacion": "AMBOS" }
```

`canalNotificacion`: `SISTEMA` (por defecto, «Dentro del sistema»), `CORREO` o `AMBOS`.

| Respuesta | Cuándo |
|---|---|
| 400 `VALIDACION` con `campos.estado` | `estado` no es `NO_LEIDAS` ni `TODAS` |
| 400 `VALIDACION` con `campos.canalNotificacion` | Falta el canal en el `PUT` |
| 400 `VALIDACION` | El canal no es uno de los tres valores |
| 401 `NO_AUTENTICADO` | Sin sesión |
| 403 `ACCESO_DENEGADO` | `PATCH` o `PUT` sin la cabecera `X-XSRF-TOKEN` |
| 404 `NO_ENCONTRADO` | La notificación no existe o es de otro usuario (no se revela cuál) |
