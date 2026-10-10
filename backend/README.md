# backend

API de AduanaTrack (autenticación, clientes/importadores, despachos, embarques/BL, Dashboard).

**Stack:** Java 21 · Spring Boot 3.5 (Web, Security, Data JPA, Validation) · Flyway · PostgreSQL 16 · JWT (jjwt) · Apache POI · Maven.
Paquete base `pe.edu.usmp.aduanatrack`, un subpaquete por módulo (`auth`, `seguridad`, `auditoria`, `cliente`, `catalogo`, `despacho`, `embarque`, `dashboard`, `config`, `common`) con `entity`, `repository`, `service`, `controller` y `dto`.

## Ejecutar

```bash
# Todo el sistema (desde la raíz del repo): copiar .env.example a .env y completar
docker compose up --build          # http://localhost

# Solo pruebas
cd backend && mvn -B verify
```

Variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (≥ 32 caracteres), `COOKIE_SECURE`.

## API (prefijo `/api/v1`)

Todo exige sesión salvo `POST /auth/login`. Las peticiones que modifican datos (POST/PUT) llevan la cabecera `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN` (Axios lo hace solo).
Errores en formato único: `{"codigo", "mensaje", "campos"}`.

| Método | Ruta | HU · Tarea | Respuesta |
|---|---|---|---|
| POST | `/auth/login` `{usuario, password, recordarme}` | HU-01 · T-01.1 | 200 + cookie `AT_SESSION`; 401 `CREDENCIALES_INVALIDAS`; 423 `CUENTA_BLOQUEADA` |
| POST | `/auth/logout` | HU-01 · T-01.1 | 204 y borra la cookie |
| GET | `/auth/me` | HU-01 · T-01.1 | `{usuario, nombre, rol}` o 401 `NO_AUTENTICADO` |
| GET | `/clientes?q=&page=&size=` | HU-04 · T-04.1 | Página `{contenido, pagina, tamano, totalElementos, totalPaginas}`; busca por inicio de RUC o parte de la razón social |
| GET | `/clientes/existe?ruc=` | HU-04 · T-04.1 | `{existe, clienteId, razonSocial}` |
| GET | `/clientes/{id}` | HU-04 · T-04.1 | Cliente con `estadoCartaPoder` (`VIGENTE`, `POR_VENCER`, `VENCIDA`, `SIN_CARTA`) |
| POST / PUT | `/clientes`, `/clientes/{id}` | HU-04 · T-04.1 | 201 / 200; 400 `VALIDACION` (RUC 11 dígitos + módulo 11); 409 `CLIENTE_DUPLICADO` con `clienteId` y `razonSocial` |
| GET | `/clientes/{id}/historial` | HU-04 · T-04.3 | `[{campo, valorAnterior, valorNuevo, usuario, fecha}]`, más reciente primero |
| POST / PUT | `/despachos`, `/despachos/{id}` `{clienteId, regimenId, aduanaId, referenciaCliente, descripcionMercancia, borrador}` | HU-03 · T-03.1 / T-03.3 | `borrador=true` → BORRADOR sin número; `false` → `DESP-AAAA-####` y APERTURA; 400 con los `campos` vacíos; 422 `CARTA_PODER_NO_VIGENTE` |
| GET | `/despachos/{id}` | HU-03 · T-03.1 | Detalle con cliente, régimen, aduana, estado, número y agente |
| GET | `/catalogos/regimenes`, `/catalogos/aduanas` | HU-03 · T-03.2 | Solo activos, por código: `[{id, codigo, descripcion}]` y `[{id, codigo, nombre}]` |
| GET / PUT | `/despachos/{id}/embarque` | HU-05 · T-05.1 / T-05.3 | BL + contenedores + `fechaLimiteAlmacenaje`, `diasRestantes`, `alertaAlmacenaje`, `etaModificada`, `etaAnterior`; 400 con la fila del contenedor (`contenedores[i].numero`); 409 `DESPACHO_EN_BORRADOR` |
| GET | `/dashboard/despachos?aduanaId=&clienteId=` | HU-02 · T-02.2 | Despachos no borradores por ETA ascendente (sin ETA al final) con `diasAlmacenaje`, `alertaCritica` |
| GET | `/dashboard/despachos/excel?aduanaId=&clienteId=` | HU-02 · T-02.2 | `despachos-AAAAMMDD.xlsx` con las mismas filas |
| GET | `/dashboard/indicadores?aduanaId=&clienteId=` | HU-02 · T-02.1 | `{despachosActivos, enCanalNaranjaRojo, conLevante, conAlertasCriticas}` sobre los despachos no borradores; activos = sin levante; alertas críticas = filas con `alertaCritica` |

Reglas configurables leídas de `parametro_sistema`: `DIAS_ALMACENAJE` (30), `DIAS_ALERTA_ALMACENAJE` (3), `DIAS_ALERTA_CARTA_PODER` (15).

## Contratos con otras tareas

- **T-00.4 (migraciones, `src/main/resources/db/migration/`):** las entidades usan `ddl-auto=validate`, así que V001 debe crear las 13 tablas de la sección 2.5 de la guía con estos tipos: `id` `BIGSERIAL`/`BIGINT IDENTITY`; textos `VARCHAR`; fechas de calendario (`fecha_emision`, `fecha_vencimiento`, `fecha_levante`, `fecha_embarque`, `fecha_eta`, `fecha_limite_almacenaje`) `DATE`; marcas de tiempo (`bloqueado_hasta`, `creado_en`, `actualizado_en`, `fecha`, `alerta_generada_en`) `TIMESTAMP` sin zona (la app trabaja en hora de Lima); `tamano_bytes BIGINT`; `intentos_fallidos INTEGER`; `correlativo_despacho(anio INTEGER PK, ultimo_numero INTEGER)`; `contenedor` con `UNIQUE (embarque_id, numero)`; `despacho.estado`, `embarque.tipo_bl` y `auditoria_acceso.resultado` como `VARCHAR` (enums en texto).
- **T-01.2 (auditoría):** implementar la interfaz `auditoria.AuditoriaAccesoService` (`@Service`, `REQUIRES_NEW`). El login la llama en cada intento; `usuarioId` es nulo si el usuario no existe. **Hasta que exista, la aplicación no arranca.**
- **T-04.2 (carta poder):** la entidad `cliente.entity.CartaPoder` y su repositorio ya existen; agregar ahí la carga del PDF y la alerta programada. `CartaPoderService` (vigencia, T-03.3) y `CartaPoderEstadoService` (estado) se pueden ampliar.
- **T-03.2 (catálogos):** las entidades `catalogo.entity.RegimenAduanero` y `Aduana` y sus repositorios ya existen; falta el controlador `GET /catalogos/regimenes` y `/catalogos/aduanas` y los datos semilla.
- **T-05.2 (ISO 6346):** `embarque.dto.ContenedorRequest.numero` valida hoy solo la forma (`AAAU1234567`); agregar ahí la anotación `@ContenedorIso6346` con el dígito de control.
- **T-02.1 (indicadores):** agregar `GET /dashboard/indicadores` en `dashboard.controller.DashboardController`, con los mismos filtros. `EstadoCartaPoder.esCritico()` y `PlazoAlmacenajeService` ya implementan la regla de alerta crítica.
