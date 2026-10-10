# database

Las migraciones versionadas del esquema las ejecuta Flyway al arrancar el backend y viven en [`backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/) (T-00.4 · SCRUM-24):

| Script | Contenido |
|---|---|
| `V001__esquema_inicial.sql` | Las 13 tablas: `rol`, `usuario`, `auditoria_acceso`, `parametro_sistema`, `regimen_aduanero`, `aduana`, `cliente`, `cliente_historial`, `carta_poder`, `despacho`, `correlativo_despacho`, `embarque`, `contenedor` |
| `V002__datos_maestros.sql` | Roles, parámetros (`DIAS_ALMACENAJE` 30, `DIAS_ALERTA_ALMACENAJE` 3, `DIAS_ALERTA_CARTA_PODER` 15) y catálogos de regímenes y aduanas |
| `V003__usuarios_demo.sql` | Un usuario por rol (`admin`, `agente`, `cliente`) con hash BCrypt |

Convención de nombres: `V001__esquema_inicial.sql`, `V002__...sql`. Una migración ya integrada en `develop` no se edita: los cambios van en una nueva versión.
