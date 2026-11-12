# Prueba de extremo a extremo del despacho (T-00.16 · SCRUM-103)

`flujo-despacho.mjs` recorre un despacho completo contra la aplicación levantada, por la misma API que usa la
pantalla: base de datos real, migraciones de Flyway, cookie de sesión y CSRF. No usa dependencias: basta con
Node 20 o superior (el mismo del frontend).

## Qué recorre

| Paso | Historia | Qué comprueba |
|---|---|---|
| Iniciar sesión | HU-01 | Cookie `AT_SESSION` y token CSRF |
| Importador y carta poder | HU-04 | Cliente con RUC válido y carta poder vigente |
| Apertura | HU-03 | Despacho en `APERTURA` con su número |
| Embarque y BL | HU-05 | Contenedor ISO 6346 válido |
| 4 documentos obligatorios | HU-06 | `DOCUMENTACION_COMPLETA` |
| Ítems y clasificación | HU-07 | `CLASIFICADO` |
| Liquidación y pago | HU-08 | `LIQUIDADO` y `TRIBUTOS_PAGADOS` |
| Numeración de la DAM | HU-09 | `DAM_NUMERADA` |
| Canal de control | HU-10 | `CANAL_ASIGNADO` y `levanteHabilitado` según el canal |
| Notificación del canal | HU-14 · CA2 | Sube el contador y la notificación está en la bandeja del agente |
| Retiro antes del levante | HU-13 · CA4 | 409 `CITA_RETIRO_SIN_LEVANTE` sin cambiar el estado |
| Cita de inspección (canal rojo) | HU-11 · CA2 | `INSPECCION_PROGRAMADA` |
| Inspección conforme (canal rojo) | HU-12 · CA3 | `CONFORME` y `levanteHabilitado` (si T-12.1 ya está publicada) |
| Levante con constancia | HU-13 · CA1 y CA3 | `LEVANTE_OTORGADO` y la constancia se descarga como PDF |
| Cita de retiro | HU-11 · CA3 | `RETIRO_PROGRAMADO` con los datos del transportista |
| Línea de tiempo | — | Todos los hitos, de `APERTURA` a `CITA_REGISTRADA` |
| Reporte de gestión | HU-15 · CA3 | El Excel y el PDF del día se descargan |

Hay dos escenarios:

- **verde**: sin inspección, el despacho pasa de `CANAL_ASIGNADO` a `LEVANTE_OTORGADO`.
- **rojo**: cita de inspección, resultado conforme y levante. Mientras la inspección (T-12.1 · SCRUM-82) no esté
  en `develop`, el script lo avisa, comprueba que el levante responde 409 con «Inspección conforme» pendiente
  (HU-13 · CA2) y registra el levante confirmando sin requisitos, con el hito `LEVANTE_CON_ADVERTENCIA`.

Cada corrida crea su propio importador y sus números (RUC, DAM, levante), así que se puede repetir sobre la
misma base sin chocar con datos anteriores.

## Cómo correrla

### Contra el docker compose local

```powershell
docker compose up --build -d
$env:E2E_PASSWORD = "<contraseña del usuario agente>"
node e2e/flujo-despacho.mjs
```

### Contra otro entorno (por ejemplo, el despliegue en Render antes de la demo)

```powershell
$env:E2E_URL = "https://<frontend>.onrender.com"
$env:E2E_USUARIO = "agente"
$env:E2E_PASSWORD = "<contraseña>"
node e2e/flujo-despacho.mjs
```

Variables:

| Variable | Por defecto | Uso |
|---|---|---|
| `E2E_URL` | `http://localhost` | URL del frontend (nginx reenvía `/api`) o del backend directo (`http://localhost:8080`) |
| `E2E_USUARIO` | `agente` | Usuario con rol AGENTE o ADMIN |
| `E2E_PASSWORD` | — | Obligatoria. La contraseña de demo se comparte fuera del repositorio |
| `E2E_ESCENARIOS` | `verde,rojo` | Para correr uno solo: `verde` o `rojo` |

La salida lista cada paso con ✔ o ✘ y termina con `N de N pasos correctos.`; si algún paso falla, el código de
salida es 1 y se muestra la respuesta de la API. Ojo: tres contraseñas incorrectas seguidas bloquean al usuario
15 minutos (HU-01).

## Registro de ejecuciones

| Fecha | Entorno | Escenarios | Resultado | Observaciones |
|---|---|---|---|---|
| 08/11/2026 | Local: backend de `develop` + PostgreSQL 18 | verde y rojo | 39 de 39 | Inspección (T-12.1) pendiente: el canal rojo se cerró con levante con advertencia |
