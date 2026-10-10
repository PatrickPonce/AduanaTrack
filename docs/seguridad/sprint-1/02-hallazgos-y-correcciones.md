# Sprint 1 — Hallazgos y correcciones

| # | Severidad | Herramienta | Regla / CVE | Archivo:línea | Descripción | Corrección aplicada | Commit / PR | Jira | Estado |
|---:|---|---|---|---|---|---|---|---|---|
| 1 | Crítica | Trivy | CVE-2026-65182 · CVE-2026-65905 · CVE-2026-68525 | backend/pom.xml | `tomcat-embed-core` 10.1.55: omisión de restricciones de seguridad y de autenticación | `tomcat.version` → 10.1.60 | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Corregido |
| 2 | Alta | Trivy | CVE-2026-89407 · CVE-2026-89425 · CVE-2026-68497 · CVE-2026-91776 · CVE-2026-91777 | backend/pom.xml | `jackson-core` / `jackson-databind` 2.21.4: denegación de servicio con JSON malformado | `jackson-bom.version` → 2.21.7 | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Corregido |
| 3 | Alta | Trivy | CVE-2026-54291 | backend/pom.xml | Driver `postgresql` 42.7.11: degradación de SCRAM-SHA-256-PLUS | `postgresql.version` → 42.7.14 | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Corregido |
| 4 | Alta | Trivy | DS-0002 | frontend/Dockerfile | El contenedor de nginx se ejecutaba como root | Imagen `nginxinc/nginx-unprivileged`, `USER 101`, puerto interno 8080 (Compose publica 80→8080) | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Corregido |
| 5 | Crítica | Trivy | CVE-2026-47884 · CVE-2026-47890 | backend/pom.xml | `spring-webmvc` 6.2.19: RCE vía `XsltView` y corrupción de flujo en vistas SSE. Solo se corrigen en Spring Framework 7 (Spring Boot 4) | **No aplica:** la API REST solo responde JSON y no usa `XsltView` ni vistas SSE. Migrar a Spring Boot 4 sale de la arquitectura aprobada (Spring Boot 3.5). Se registra en `.trivyignore` con vencimiento 31/12/2026 para revisarlo en cada sprint | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Aceptado (no aplicable) |
| 6 | — | Trivy | HTTP 429 de Maven Central | .github/workflows/security-scan.yml | Trivy terminaba con error al resolver `backend/pom.xml` contra Maven Central (límite de peticiones) y el escaneo fallaba en todos los PR | El job descarga antes las dependencias (`dependency:go-offline`, con caché) y Trivy usa ese repositorio local `~/.m2` | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Corregido |
| 7 | Alta | Gitleaks | generic-api-key | .env.example:6 | Gitleaks marca `JWT_SECRET` como clave expuesta en el commit 5583e73 | **Falso positivo:** el valor es un texto de ejemplo (`cambiar-por-un-secreto-aleatorio-...`); el secreto real va en `.env`, que está en `.gitignore`. Se registra la huella en `.gitleaksignore` | `fix/SCRUM-26-vulnerabilidades-dependencias` | SCRUM-26 | Aceptado (falso positivo) |
| 8 | Alta | Semgrep | detected-bcrypt-hash (×3) | backend/src/main/resources/db/migration/V003__usuarios_demo.sql:10, 12, 14 | Semgrep marca los hashes BCrypt de los tres usuarios de demostración (`admin`, `agente`, `cliente`) | **Aceptado:** T-00.4 pide sembrar un usuario por rol con hash BCrypt; son cuentas de demostración para el entorno local, sin datos reales, y la contraseña no está en el repositorio. Se suprime la regla solo en esas tres líneas con `nosemgrep`; el reporte consolidado las lista aparte como "Hallazgos aceptados" y no las cuenta en el Quality Gate. Estas cuentas no deben existir en un entorno productivo | PR #26 | SCRUM-24 | Aceptado (cuentas demo) |

**Resumen**

| | Crítica | Alta | Media | Baja |
|---|---:|---:|---:|---:|
| Escaneo inicial | | | | |
| Escaneo final | | | | |

> Completar los totales con los artefactos `reporte-vulnerabilidades` de los escaneos `sprint-1-inicial` y `sprint-1-correcciones` (T-00.6).
