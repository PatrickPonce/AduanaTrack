# Reporte de revisión de vulnerabilidades — AduanaTrack

- **Repositorio:** `PatrickPonce/AduanaTrack` · **Rama:** `main` · **Commit:** `776bf2b`
- **Fecha (hora Perú):** 29/10/2026 12:23
- **Ejecución:** https://github.com/PatrickPonce/AduanaTrack/actions/runs/40920778997
- **Quality Gate (0 críticas / 0 altas):** ✅ APROBADO

## Resumen por herramienta

| Herramienta | Versión | Crítica | Alta | Media | Baja | Total |
|---|---|---:|---:|---:|---:|---:|
| gitleaks | v8.0.0 | 0 | 0 | 0 | 0 | 0 |
| Semgrep OSS | 1.182.1 | 0 | 0 | 27 | 0 | 27 |
| Trivy | 0.76.1 | 0 | 0 | 2 | 2 | 4 |
| **Total** | | **0** | **0** | **29** | **2** | **31** |

## Detalle de hallazgos

| # | Severidad | Herramienta | Regla | Ubicación | Descripción |
|---:|---|---|---|---|---|
| 1 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/ci.yml:27` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 2 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/ci.yml:43` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 3 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/ci.yml:54` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 4 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/ci.yml:65` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 5 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/codeql.yml:30` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 6 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/codeql.yml:31` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 7 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/codeql.yml:36` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 8 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:103` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 9 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:117` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 10 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:120` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 11 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:140` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 12 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:144` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 13 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:157` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 14 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:160` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 15 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:172` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 16 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:173` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 17 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:179` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 18 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:192` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 19 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:193` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 20 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:211` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 21 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:36` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 22 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:37` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 23 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:64` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 24 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:68` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 25 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:84` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 26 | Media | Semgrep OSS | `yaml.github-actions.security.github-actions-mutable-action-tag.github-actions-mutable-action-tag` | `.github/workflows/security-scan.yml:99` | GitHub Actions step uses a mutable tag or branch reference. Tags and branch names can be silently repointed by the action owner, enabling supply-chain attack... |
| 27 | Media | Semgrep OSS | `generic.nginx.security.request-host-used.request-host-used` | `frontend/nginx.conf.template:2` | '$http_host' and '$host' variables may contain a malicious value from attacker controlled 'Host' request header. Use an explicitly configured host value or a... |
| 28 | Media | Trivy | `CVE-2025-48924` | `backend/pom.xml:1` | Package: org.apache.commons:commons-lang3 Installed Version: 3.17.0 Vulnerability CVE-2025-48924 Severity: MEDIUM Fixed Version: 3.18.0 Link: [CVE-2025-48924... |
| 29 | Media | Trivy | `CVE-2026-49844` | `backend/pom.xml:1` | Package: org.apache.logging.log4j:log4j-api Installed Version: 2.24.3 Vulnerability CVE-2026-49844 Severity: MEDIUM Fixed Version: 2.25.5, 2.26.1 Link: [CVE-... |
| 30 | Baja | Trivy | `DS-0026` | `backend/Dockerfile:1` | Artifact: backend/Dockerfile Type: dockerfile Vulnerability DS-0026 Severity: LOW Message: Add HEALTHCHECK instruction in your Dockerfile Link: [DS-0026](htt... |
| 31 | Baja | Trivy | `DS-0026` | `frontend/Dockerfile:1` | Artifact: frontend/Dockerfile Type: dockerfile Vulnerability DS-0026 Severity: LOW Message: Add HEALTHCHECK instruction in your Dockerfile Link: [DS-0026](ht... |

## Hallazgos aceptados (suprimidos en el código)

No cuentan para el Quality Gate. Cada uno debe estar justificado en `docs/seguridad/sprint-N/02-hallazgos-y-correcciones.md`.

| # | Severidad | Herramienta | Regla | Ubicación | Descripción |
|---:|---|---|---|---|---|
| 1 | Alta | Semgrep OSS | `generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash` | `backend/src/main/resources/db/migration/V003__usuarios_demo.sql:10` | bcrypt hash detected |
| 2 | Alta | Semgrep OSS | `generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash` | `backend/src/main/resources/db/migration/V003__usuarios_demo.sql:12` | bcrypt hash detected |
| 3 | Alta | Semgrep OSS | `generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash` | `backend/src/main/resources/db/migration/V003__usuarios_demo.sql:14` | bcrypt hash detected |

> Registrar cada hallazgo Crítico/Alto en Jira (`fix/SCRUM-NN`), corregirlo y volver a ejecutar el escaneo para evidenciar la corrección.
