# Despliegue en la nube (Render + Neon, planes gratuitos)

La base de datos está en **Neon**; el backend y el frontend corren como dos servicios Docker en **Render** (plan Free).
El navegador solo habla con el frontend: nginx reenvía `/api` al backend, así la cookie de sesión y el token CSRF siguen siendo del mismo origen.

## Limitaciones del plan Free

- El servicio **se duerme tras 15 minutos sin tráfico**; la primera visita tarda cerca de un minuto. Antes de una demostración, abrir la página 2 minutos antes.
- **No hay disco persistente**: los PDF (cartas de poder, documentos del despacho, cargo de la DAM) se pierden cuando el servicio se reinicia o se duerme. Los datos de las tablas se conservan en Neon.
- Memoria de ~512 MB para el backend (Spring Boot). Si el servicio se reinicia por falta de memoria, revisar los logs en Render.

## Variables

| Servicio | Variable | Valor |
|---|---|---|
| backend | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST-DE-NEON/neondb?sslmode=require` (conexión directa, sin `-pooler`) |
| backend | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
| backend | `SPRING_DATASOURCE_PASSWORD` | contraseña de Neon |
| backend | `JWT_SECRET` | secreto aleatorio de al menos 32 caracteres (el blueprint lo genera) |
| backend | `COOKIE_SECURE` | `true` |
| backend | `PORT` | `8080` |
| frontend | `BACKEND_URL` | URL pública del backend sin barra final, p. ej. `https://aduanatrack-backend.onrender.com` |
| frontend | `PORT` | `8080` |

Las claves se escriben solo en el panel de Render. Nunca se suben al repositorio.

## Crear los servicios

Opción 1, con el blueprint (`render.yaml`):

1. En Render: **New → Blueprint** y elegir el repositorio `PatrickPonce/AduanaTrack` y la rama a desplegar.
2. Render pide los valores marcados `sync: false`: las tres variables de Neon del backend y `BACKEND_URL` del frontend.
3. Para `BACKEND_URL`, si todavía no se conoce, poner un valor provisional y corregirlo luego (ver paso de abajo).

Opción 2, a mano (si el blueprint pide tarjeta o falla):

1. **New → Web Service**, repositorio de GitHub, **Language: Docker**, **Root Directory:** `backend`, **Instance type: Free**. Agregar las variables del backend.
2. Repetir con **Root Directory:** `frontend` y la variable `BACKEND_URL`.

## Después de crear

1. Copiar la URL pública del backend (arriba, en su página: `https://....onrender.com`).
2. En el servicio del frontend: **Environment → `BACKEND_URL`** con esa URL, sin barra final. Guardar; el servicio se vuelve a desplegar.
3. Abrir la URL del frontend e iniciar sesión.
4. Primer arranque del backend: Flyway crea las tablas en Neon (tarda más de un minuto).

## Datos de demostración

El script [`backend/src/main/resources/demo/datos-demo.sql`](../backend/src/main/resources/demo/datos-demo.sql) llena la base para la demo (19/11/2026). **No es una migración**: Flyway no lo ejecuta y no corre solo en ningún arranque; se carga a mano cuando hace falta.

Qué deja:

- 2 empresas con su usuario cliente ya asignado: **Importadora Andina S.A.C.** (`jperez`) y **Pacífico Trade E.I.R.L.** (`mrojas`). Con el usuario `cliente` original no cambia nada.
- 12 despachos, uno por cada etapa (apertura, documentación, clasificado, liquidado, tributos pagados, DAM, canal asignado, inspección programada, inspección conforme, levante otorgado y retiro programado) y 1 borrador. Incluyen embarque, línea de tiempo, liquidación y pago, canales verde/naranja/rojo, citas, inspecciones (una con observaciones, dos conformes) y levante.
- Un despacho con el almacenaje por vencer en 2 días y una carta poder que vence en 10 días, para que se vean las alertas del dashboard.
- Notificaciones de ejemplo (leídas y sin leer) para `jperez`, `mrojas` y el agente.

Cómo cargarlo (necesita `psql` y las migraciones ya aplicadas, es decir, el backend arrancó al menos una vez):

```
psql "postgresql://USUARIO:CLAVE@HOST-DE-NEON/neondb?sslmode=require" -v ON_ERROR_STOP=1 -f backend/src/main/resources/demo/datos-demo.sql
```

En local con Docker Compose, la base va en el contenedor de PostgreSQL:

```
docker compose exec -T db psql -U aduanatrack -d aduanatrack -v ON_ERROR_STOP=1 < backend/src/main/resources/demo/datos-demo.sql
```

(ajustar el nombre del servicio y el usuario a los de `docker-compose.yml` si son otros). La cadena de conexión con la clave se escribe solo en la terminal; no se guarda en el repositorio.

Notas:

- Se puede ejecutar más de una vez: si las empresas ya existen, avisa y no hace nada. Todo se carga en una sola transacción, así que si falla no queda nada a medias.
- **No hay contraseñas en el script.** `jperez` y `mrojas` copian el hash del usuario `cliente`, así que entran con la misma contraseña de demo que ya se comparte por el grupo.
- Los PDF (cartas poder, documentos, actas) no se cargan: sus botones de descarga responden «no disponible» hasta que se suban desde la aplicación. En Render Free esos archivos se pierden al reiniciar de todos modos.
- Los números de despacho son `DESP-2026-0901` a `0912`; el correlativo queda en 912 para que los despachos nuevos continúen desde `0913`.
- Las fechas (ETA, plazo de almacenaje, citas, carta poder) se calculan respecto del día en que se carga el script: conviene cargarlo el día anterior a la demo, no semanas antes.
- Para limpiar y volver a cargar en una base de pruebas, borrar los datos de las dos empresas y sus usuarios (o recrear la base); el script no incluye borrado a propósito.

## Solución de problemas

- **502 o página en blanco al entrar:** el backend sigue arrancando o durmiendo. Esperar un minuto y recargar.
- **`BACKEND_URL` con barra final o sin `https://`:** nginx no arranca. Debe verse como `https://nombre.onrender.com`.
- **Login que no se queda iniciado:** comprobar `COOKIE_SECURE=true` y que se entra por la URL del frontend, no por la del backend.
- **Error de conexión a la base:** revisar que la URL de Neon sea la directa y termine en `?sslmode=require`.
