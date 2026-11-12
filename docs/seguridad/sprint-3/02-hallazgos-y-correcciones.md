# Sprint 3 — Hallazgos y correcciones (SCRUM-99)

Escaneo `sprint-3-inicial` ejecutado sobre `develop` (commit `5bbadec`, 08/11/2026).
Ejecución: https://github.com/PatrickPonce/AduanaTrack/actions/runs/42529554539 · **Quality Gate (0 críticas / 0 altas): ✅ APROBADO**

Reporte completo: [01-reporte-inicial.md](01-reporte-inicial.md).

| | Crítica | Alta | Media | Baja |
|---|---:|---:|---:|---:|
| Escaneo inicial | 0 | 0 | 30 | 2 |

| # | Severidad | Herramienta | Regla / CVE | Archivo | Corrección | Estado |
|---:|---|---|---|---|---|---|
| 1 | Media | Trivy | CVE-2025-48924 | `backend/pom.xml` | `commons-lang3` 3.17.0 → 3.18.0 (propiedad `commons-lang3.version`) | Corregido |
| 2 | Media | Trivy | CVE-2026-49844 | `backend/pom.xml` | `log4j-api` 2.24.3 → 2.25.5 (propiedad `log4j2.version`) | Corregido |
| 3 | Media (HIGH en Trivy) | Trivy | CVE-2026-116689 | `frontend/package-lock.json` | `follow-redirects` 1.16.1 → 1.16.3 (`npm update`) | Corregido |
| 4 | Media | Semgrep | `request-host-used` | `frontend/nginx.conf.template:2` | Falso positivo: solo era un comentario que nombraba `$host`; se reescribió el comentario | Corregido |
| 5 | Baja | Trivy | DS-0026 (×2) | `backend/Dockerfile`, `frontend/Dockerfile` | Se agregó `HEALTHCHECK` a ambos | Corregido |
| 6 | Media (×23) | Semgrep | `github-actions-mutable-action-tag` | `.github/workflows/*.yml` | Fijar `uses:` a SHA de commit | Pendiente (mejora técnica, no bloquea el Quality Gate) |
| 7 | Alta (suprimido) | Semgrep | `detected-bcrypt-hash` (×3) | `V003__usuarios_demo.sql` | Aceptado: cuentas demo documentadas en el Sprint 1 | Aceptado |

Verificación local: las imágenes `backend` y `frontend` construyen y el `HEALTHCHECK` del frontend responde. El re-escaneo (`sprint-3-correcciones`) se corre tras fusionar este PR y debe quedar en `03-reporte-final.md`.
