# AduanaTrack

**Sistema de Seguimiento Aduanero de Importaciones Marítimas** · Código de proyecto `PROY-ADU-2026-01`

Universidad de San Martín de Porres — Facultad de Ingeniería y Arquitectura
Diseño e Implementación de Sistemas · **Grupo 2** · Docente: Mg. Juan Manuel Huapalla García

[![CI](../../actions/workflows/ci.yml/badge.svg?branch=develop)](../../actions/workflows/ci.yml)
[![Security Scan](../../actions/workflows/security-scan.yml/badge.svg?branch=develop)](../../actions/workflows/security-scan.yml)
[![CodeQL](../../actions/workflows/codeql.yml/badge.svg?branch=develop)](../../actions/workflows/codeql.yml)

---

## Equipo Scrum

| Integrante | Rol |
|---|---|
| Altamirano Zuñiga, Juan Diego | Product Owner |
| Tipian Jacobo, Alvaro Gustavo | Scrum Master |
| Linares Estivariz, Kiara Leticia | Developer · Frontend |
| Ponce Simeón, Patrick Vossler | Developer · Backend |
| Saavedra García, Fabio Enrique | Developer · Backend / DevOps |

## Herramientas CASE

| Herramienta | Uso | Enlace |
|---|---|---|
| Jira Software | Tablero Kanban/Scrum, backlog y asignación de tareas | [grupo2dis.atlassian.net · SCRUM](https://grupo2dis.atlassian.net/jira/software/projects/SCRUM/boards/1) |
| GitHub | Repositorio con GitFlow (`main`, `develop`, `feature/HU-XX-*`) | este repositorio |
| GitHub Actions | CI + revisión de vulnerabilidades (CodeQL, Semgrep, Gitleaks, Trivy) | pestaña **Actions** |

## Sistema en línea

| Componente | Servicio | Dirección |
|---|---|---|
| Aplicación (frontend + API) | Render, plan gratuito | https://aduanatrack-frontend.onrender.com |
| Base de datos | Neon (PostgreSQL), plan gratuito | no es pública |

Usuarios demo: `agente`, `admin` y `cliente`. La contraseña se comparte por el grupo, no se guarda en el repositorio.
Para la demostración hay un script de datos de ejemplo (despachos en cada etapa, dos empresas con su usuario cliente y notificaciones): ver «Datos de demostración» en [`docs/despliegue.md`](docs/despliegue.md).

- Se despliega desde la rama `develop`: cada push la vuelve a publicar.
- Se duerme tras 15 minutos sin uso; la primera visita puede tardar hasta un minuto.
- Los PDF subidos (cartas de poder, documentos, cargo de la DAM) se pierden cuando el servicio se reinicia.

Guía de configuración: [`docs/despliegue.md`](docs/despliegue.md).

## Sprint actual

**Sprint 3 (S14–S16, 02/11 – 21/11) · 28 SP** — *Completar el ciclo del despacho después del canal de control: citas en depósito, inspección, levante, notificaciones y alertas, reportes de gestión y portal del cliente con acceso por empresa.*

| HU | Historia | SP | Jira |
|---|---|---|---|
| HU-11 | Registro de citas en depósito temporal o almacén | 5 | SCRUM-18 |
| HU-12 | Registro del resultado de la inspección aduanera | 5 | SCRUM-19 |
| HU-13 | Registro manual del levante de mercancía | 5 | SCRUM-20 |
| HU-14 | Notificaciones y alertas de estado del despacho | 5 | SCRUM-21 |
| HU-15 | Reportes e indicadores de gestión aduanera | 8 | SCRUM-22 |

También se trabaja en este sprint la HU-16 · Portal del cliente y acceso por empresa (contrato C11).

Detalle: [`docs/product-backlog.md`](docs/product-backlog.md) · contratos del sprint en [`docs/sprint-3/contratos/`](docs/sprint-3/contratos/). Los sprints 1 y 2 están cerrados: [`docs/sprint-1/sprint-backlog.md`](docs/sprint-1/sprint-backlog.md) · [`docs/sprint-2/contratos/`](docs/sprint-2/contratos/).

### Calendario de sprints

| Sprint | Semanas | Fechas | Sprint Planning | Review · Retro · Demo |
|---|---|---|---|---|
| Sprint 1 | S7, S9, S10 (S8 = parciales) | 14/09 – 10/10/2026 | S7 | S10 |
| Sprint 2 | S11 – S13 | 12/10 – 31/10/2026 | S11 | S13 |
| Sprint 3 | S14 – S16 | 02/11 – 21/11/2026 | S14 | S16 |

Daily Scrum: un video por semana.

## Estructura del repositorio

```
AduanaTrack/
├── backend/            # API (stack por definir por el equipo)
├── frontend/           # Interfaz web
├── database/           # Scripts de esquema y migraciones
├── e2e/                # Prueba de extremo a extremo del despacho (node e2e/flujo-despacho.mjs)
├── docs/
│   ├── product-backlog.md
│   ├── sprint-1/       # Sprint backlog y evidencias del sprint
│   └── seguridad/      # Reportes de vulnerabilidades y correcciones por sprint
└── .github/
    ├── workflows/      # ci.yml · security-scan.yml · codeql.yml
    └── scripts/        # Conversión de reportes SARIF a Markdown
```

## Flujo de trabajo (GitFlow)

1. Toma tu subtarea en Jira y muévela a **En curso**.
2. Trabaja en la rama de la historia: `git checkout feature/HU-XX-...` (creada desde `develop`).
3. Commits con la clave de Jira: `SCRUM-27 agrega log de auditoría de accesos`.
4. Pull Request `feature/HU-XX-*` → `develop` (pasa CI + Security Scan + 1 revisión).
5. Al cerrar el sprint: PR `develop` → `main` y tag `v0.1.0` (Sprint 1).

Reglas completas en [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Revisión de vulnerabilidades

El workflow **Security Scan** se ejecuta en cada push/PR a `develop` y `main`, cada lunes y manualmente (*Actions → Security Scan → Run workflow*). Genera el artefacto **`reporte-vulnerabilidades`** (Markdown + SARIF) y publica los hallazgos en *Security → Code scanning*.

Procedimiento de cierre de sprint (reporte → correcciones → re-escaneo): [`docs/seguridad/README.md`](docs/seguridad/README.md).
