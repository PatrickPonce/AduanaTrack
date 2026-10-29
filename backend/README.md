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

Variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (≥ 32 caracteres), `COOKIE_SECURE`, `CARTAS_PODER_DIR` y `DOCUMENTOS_DIR` (carpetas de los PDF; en Docker son volúmenes).

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
| GET | `/clientes/{id}/carta-poder` | HU-04 (prototipo) | `[{id, fechaEmision, fechaVencimiento, nombreArchivo, tamanoBytes, estado}]`, vencimiento más lejano primero; `estado` según la fecha de cada carta |
| GET | `/clientes/{id}/carta-poder/{cartaId}/archivo` | HU-04 · T-04.2 | PDF (`Content-Disposition: attachment` con el nombre original); 404 si la carta es de otro cliente o el archivo no está |
| POST | `/clientes/{id}/carta-poder` multipart `archivo`, `fechaEmision`, `fechaVencimiento` | HU-04 · T-04.2 | 201 con la carta y `estadoCartaPoder`; 400 con los `campos`; 413 `ARCHIVO_MUY_GRANDE` (> 10 MB); 415 `FORMATO_NO_PERMITIDO` (no empieza con `%PDF-`). Se guarda en `/data/cartas-poder/{uuid}.pdf`; alerta diaria a las 7:00 (Lima) |
| POST / PUT | `/despachos`, `/despachos/{id}` `{clienteId, regimenId, aduanaId, referenciaCliente, descripcionMercancia, borrador, tipoOperacion, modalidadDespacho}` | HU-03 · T-03.1 / T-03.3 | `borrador=true` → BORRADOR sin número; `false` → `DESP-AAAA-####` y APERTURA; 400 con los `campos` vacíos; 422 `CARTA_PODER_NO_VIGENTE` |
| GET | `/despachos/{id}` | HU-03 · T-03.1 | Detalle con cliente, régimen, aduana, estado, número y agente |
| GET | `/catalogos/regimenes`, `/catalogos/aduanas` | HU-03 · T-03.2 | Solo activos, por código: `[{id, codigo, descripcion}]` y `[{id, codigo, nombre}]` |
| GET / PUT | `/despachos/{id}/embarque` | HU-05 · T-05.1 / T-05.3 | BL (con `numeroViaje`) + contenedores (con `pesoBrutoKg`) + `fechaLimiteAlmacenaje`, `diasRestantes`, `alertaAlmacenaje`, `etaModificada`, `etaAnterior`; 400 con la fila del contenedor (`contenedores[i].numero`); 409 `DESPACHO_EN_BORRADOR` |
| GET | `/despachos/{id}/hitos` | HU-10 · T-10.1 | Línea de tiempo en orden cronológico: `[{tipo, descripcion, sugerido, usuario, fecha}]`; `usuario` = `Sistema` en los hitos sugeridos; 404 si el despacho no existe |
| GET | `/despachos/{id}/documentos` | HU-06 · T-06.2 | `{estadoGeneral, documentos: [{tipo, nombre, obligatorio, estado, version, documentoId, archivo, tamanoBytes, cargadoPor, cargadoEn}]}`; `estadoGeneral` = `COMPLETO` cuando todos los obligatorios tienen versión vigente (contrato C1) |
| POST | `/despachos/{id}/documentos/{tipo}` multipart `archivo` | HU-06 · T-06.2 | 201 `{documentoId, tipo, version, estado}`; reemplazar crea la versión siguiente y la anterior queda con `vigente=false`; 400 `campos.archivo`; 404 despacho o tipo; 409 `DESPACHO_EN_BORRADOR` o `CARGA_SIMULTANEA`; 413 `ARCHIVO_MUY_GRANDE`; 415 `FORMATO_NO_PERMITIDO`. Se guarda en `/data/documentos/{despachoId}/{uuid}.pdf` |
| GET | `/despachos/{id}/documentos/{tipo}/versiones` | HU-06 · T-06.2 | `[{documentoId, version, vigente, archivo, tamanoBytes, cargadoPor, cargadoEn}]`, la más reciente primero |
| GET | `/despachos/{id}/documentos/{documentoId}/archivo` | HU-06 · T-06.2 | PDF de cualquier versión (`Content-Disposition: attachment`); 404 si es de otro despacho o el archivo no está |
| GET | `/dashboard/despachos?aduanaId=&clienteId=` | HU-02 · T-02.2 | Despachos no borradores por ETA ascendente (sin ETA al final) con `contenedor`, `totalContenedores`, `canalControl`, `diasAlmacenaje`, `alertaCritica` |
| GET | `/dashboard/despachos/excel?aduanaId=&clienteId=` | HU-02 · T-02.2 | `despachos-AAAAMMDD.xlsx` con las mismas filas |
| GET | `/dashboard/indicadores?aduanaId=&clienteId=` | HU-02 · T-02.1 | `{despachosActivos, enCanalNaranjaRojo, conLevante, conAlertasCriticas}` sobre los despachos no borradores; activos = sin levante; alertas críticas = filas con `alertaCritica` |

Permisos por rol: `POST` y `PUT` sobre `/clientes/**` y `/despachos/**` solo para `ADMIN` y `AGENTE` (403 `ACCESO_DENEGADO` para `CLIENTE`); las consultas son para los tres perfiles.

Reglas configurables leídas de `parametro_sistema`: `DIAS_ALMACENAJE` (30), `DIAS_ALERTA_ALMACENAJE` (3), `DIAS_ALERTA_CARTA_PODER` (15).

## Contratos con otras tareas

### Sprint 2

- **Estados y línea de tiempo (T-10.1):** `EstadoDespacho` ya tiene los estados del Sprint 2 en orden (`APERTURA` → `DOCUMENTACION_COMPLETA` → `CLASIFICADO` → `LIQUIDADO` → `TRIBUTOS_PAGADOS` → `DAM_NUMERADA` → `CANAL_ASIGNADO`). Cada servicio valida el estado anterior, llama a `despacho.cambiarEstado(nuevo)` y, en la misma transacción, a `HitoDespachoService.registrar(despachoId, TipoHito.X, descripcion)`, que guarda el usuario de la sesión y la hora de Lima. Para el canal rojo (T-10.2) está `registrarSugerido(despachoId, "Programar inspección física")`. `EstadoDespacho.alcanzo(otro)` dice si el despacho ya llegó a un estado (p. ej. para habilitar una pestaña). Migración `V005__hito_despacho.sql`: crea la tabla y el hito de apertura de los despachos que ya estaban abiertos.
- **Documentos del despacho (T-06.1 y T-06.2):** contrato en [`docs/sprint-2/contratos/C1-documentos-despacho.md`](../docs/sprint-2/contratos/C1-documentos-despacho.md). Catálogo en `tipo_documento` (V006) y versiones en `documento_despacho`, con una sola vigente por tipo (índice único parcial). La validación y el guardado del PDF están en `common.AlmacenPdf`, compartido con la carta poder; T-09.1 (cargo de la DAM) puede reutilizar `DocumentoDespachoService.cargar`. Para T-06.3 (Fabio): el estado general ya se calcula en `listar`; falta el paso a `DOCUMENTACION_COMPLETA` con su hito al cargar el último obligatorio (marcado en `cargar`) y `exigirDocumentacionCompleta(despachoId)` → 409 `DOCUMENTACION_INCOMPLETA` con `pendientes`.

### Sprint 1

- **T-00.4 (migraciones, `src/main/resources/db/migration/`):** las entidades usan `ddl-auto=validate`, así que V001 debe crear las 13 tablas de la sección 2.5 de la guía con estos tipos: `id` `BIGSERIAL`/`BIGINT IDENTITY`; textos `VARCHAR`; fechas de calendario (`fecha_emision`, `fecha_vencimiento`, `fecha_levante`, `fecha_embarque`, `fecha_eta`, `fecha_limite_almacenaje`) `DATE`; marcas de tiempo (`bloqueado_hasta`, `creado_en`, `actualizado_en`, `fecha`, `alerta_generada_en`) `TIMESTAMP` sin zona (la app trabaja en hora de Lima); `tamano_bytes BIGINT`; `intentos_fallidos INTEGER`; `correlativo_despacho(anio INTEGER PK, ultimo_numero INTEGER)`; `contenedor` con `UNIQUE (embarque_id, numero)`; `despacho.estado`, `embarque.tipo_bl` y `auditoria_acceso.resultado` como `VARCHAR` (enums en texto).
- **T-01.2 (auditoría):** implementar la interfaz `auditoria.AuditoriaAccesoService` (`@Service`, `REQUIRES_NEW`). El login la llama en cada intento; `usuarioId` es nulo si el usuario no existe. **Hasta que exista, la aplicación no arranca.**
- **T-04.2 (carta poder):** la entidad `cliente.entity.CartaPoder` y su repositorio ya existen; agregar ahí la carga del PDF y la alerta programada. `CartaPoderService` (vigencia, T-03.3) y `CartaPoderEstadoService` (estado) se pueden ampliar.
- **T-03.2 (catálogos):** las entidades `catalogo.entity.RegimenAduanero` y `Aduana` y sus repositorios ya existen; falta el controlador `GET /catalogos/regimenes` y `/catalogos/aduanas` y los datos semilla.
- **T-05.2 (ISO 6346):** `embarque.dto.ContenedorRequest.numero` valida hoy solo la forma (`AAAU1234567`); agregar ahí la anotación `@ContenedorIso6346` con el dígito de control.
- **T-02.1 (indicadores):** agregar `GET /dashboard/indicadores` en `dashboard.controller.DashboardController`, con los mismos filtros. `EstadoCartaPoder.esCritico()` y `PlazoAlmacenajeService` ya implementan la regla de alerta crítica.
