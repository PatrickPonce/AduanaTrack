# Sprint 2 — Hallazgos y correcciones

Escaneo ejecutado automáticamente al hacer push a `main` (commit `776bf2b`, cierre del Sprint 2 + rediseño visual).
Ejecución: https://github.com/PatrickPonce/AduanaTrack/actions/runs/40920778997 · **Quality Gate (0 críticas / 0 altas): ✅ APROBADO**

No hubo hallazgos Críticos ni Altos. Se registran aquí los Medios y Bajos relevantes (dependencias con CVE y configuración), y se agrupan los hallazgos repetidos de una misma regla.

| # | Severidad | Herramienta | Regla / CVE | Archivo:línea | Descripción | Corrección propuesta | Jira | Estado |
|---:|---|---|---|---|---|---|---|---|
| 1 | Media | Trivy | CVE-2025-48924 | `backend/pom.xml` | `commons-lang3` 3.17.0: vulnerabilidad de denegación de servicio | Actualizar a `commons-lang3` 3.18.0 | SCRUM-NN | Pendiente |
| 2 | Media | Trivy | CVE-2026-49844 | `backend/pom.xml` | `log4j-api` 2.24.3 (dependencia transitiva) | Actualizar a 2.25.5 o 2.26.1 | SCRUM-NN | Pendiente |
| 3 | Media | Semgrep | `generic.nginx.security.request-host-used` | `frontend/nginx.conf.template:2` | `$http_host`/`$host` puede contener un valor manipulado por el atacante en el header `Host` | Usar un valor de host configurado explícitamente en vez de `$host`/`$http_host` | SCRUM-NN | Pendiente |
| 4 | Media | Semgrep | `yaml.github-actions.security.github-actions-mutable-action-tag` (×23) | `.github/workflows/ci.yml`, `codeql.yml`, `security-scan.yml` (varias líneas) | Los steps de GitHub Actions usan tags mutables (`@v3`, `@v4`, etc.) en vez de fijar el commit SHA; el dueño de la acción podría re-apuntar el tag (riesgo de cadena de suministro) | Fijar cada `uses:` al SHA del commit en vez del tag de versión | SCRUM-NN | Pendiente |
| 5 | Baja | Trivy | DS-0026 (×2) | `backend/Dockerfile:1`, `frontend/Dockerfile:1` | Falta la instrucción `HEALTHCHECK` en los Dockerfile | Agregar `HEALTHCHECK` a ambos Dockerfile | SCRUM-NN | Pendiente |
| 6 | Alta (suprimido) | Semgrep | `generic.secrets.security.detected-bcrypt-hash` (×3) | `backend/src/main/resources/db/migration/V003__usuarios_demo.sql:10,12,14` | Hashes BCrypt de los 3 usuarios de demostración (heredado del Sprint 1) | **Aceptado:** mismas cuentas de demo documentadas en `docs/seguridad/sprint-1/02-hallazgos-y-correcciones.md`, no deben existir en producción | SCRUM-24 | Aceptado (cuentas demo) |

**Resumen**

| | Crítica | Alta | Media | Baja |
|---|---:|---:|---:|---:|
| Escaneo (commit `776bf2b`, `main`) | 0 | 0 | 29 | 2 |

> Los 29 Medios se agrupan arriba en 4 filas (23 de ellas son la misma regla de tags mutables en Actions, repetida en distintas líneas). Detalle completo con todas las ubicaciones en [01-reporte-inicial.md](01-reporte-inicial.md).
> Ninguno de estos hallazgos bloquea el Quality Gate (solo cuenta Crítica/Alta). Quedan como mejora técnica para un próximo sprint; crear sus subtareas en Jira (reemplazar `SCRUM-NN`) si se van a programar.
