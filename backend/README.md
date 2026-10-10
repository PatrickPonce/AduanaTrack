# backend

API de AduanaTrack (autenticación, clientes/importadores, despachos, embarques/BL, indicadores).

**Stack:** Java 21 · Spring Boot 3.5 (Web, Security, Data JPA, Validation) · Flyway · PostgreSQL 16 · JWT (jjwt) · Maven.
Paquete base `pe.edu.usmp.aduanatrack`, un subpaquete por módulo (`auth`, `seguridad`, `auditoria`, `cliente`, `despacho`, …) con `entity`, `repository`, `service`, `controller` y `dto`.

## Ejecutar

```bash
# Todo el sistema (desde la raíz del repo): copiar .env.example a .env y completar
docker compose up --build          # http://localhost

# Solo pruebas
cd backend && mvn -B verify
```

Variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (≥ 32 caracteres), `COOKIE_SECURE`.

## Autenticación (HU-01 · T-01.1)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/api/v1/auth/login` `{usuario, password, recordarme}` | 200 + cookie `AT_SESSION`; 401 `CREDENCIALES_INVALIDAS`; 423 `CUENTA_BLOQUEADA` |
| POST | `/api/v1/auth/logout` | 204 y borra la cookie |
| GET | `/api/v1/auth/me` | `{usuario, nombre, rol}` o 401 `NO_AUTENTICADO` |

- `usuario` acepta el nombre de usuario o el correo corporativo.
- 3 intentos fallidos consecutivos → bloqueo de 15 min (423 aunque la contraseña sea correcta). Un login exitoso reinicia el contador.
- Cookie `HttpOnly`, `SameSite=Strict`, `Path=/`; `Max-Age` 7200 (2 h) o 28800 (8 h con «Recordarme»).
- CSRF activo con cookie `XSRF-TOKEN`: Axios la reenvía sola en `X-XSRF-TOKEN`. Solo `/auth/login` está exceptuado.
- Errores en formato único: `{"codigo", "mensaje", "campos"}` (`common/GlobalExceptionHandler`).

## Contratos con otras tareas

- **T-00.4 (migraciones):** las entidades usan `ddl-auto=validate`, así que las tablas de V001 deben coincidir con ellas. `rol(id, codigo, nombre)` y `usuario(id, usuario, correo, nombre, password_hash, rol_id, activo, intentos_fallidos, bloqueado_hasta)`, con `id` `BIGINT GENERATED … AS IDENTITY` (o `BIGSERIAL`), textos `VARCHAR`, `activo BOOLEAN`, `intentos_fallidos INTEGER` y `bloqueado_hasta TIMESTAMP` (sin zona horaria: la app trabaja en hora de Lima). Ruta: `src/main/resources/db/migration/`.
- **T-01.2 (auditoría):** el login llama a la interfaz `auditoria.AuditoriaAccesoService#registrar(usuarioIngresado, usuarioId, ip, userAgent, ResultadoAcceso)` en cada intento (`usuarioId` es nulo si el usuario no existe). Falta su implementación (`@Service`, `REQUIRES_NEW`). Hasta que exista, la aplicación no arranca.
