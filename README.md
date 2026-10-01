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

## Sprint actual

**Sprint 1 (S7–S10, 14/09 – 10/10; S8 parciales) · 23 SP** — *Habilitar el flujo base de apertura y seguimiento inicial de despachos de importación marítima: acceso seguro al sistema, registro de clientes/importadores, apertura de un nuevo despacho y registro de la información de embarque (BL), con visibilidad centralizada del estado de los despachos en el Dashboard.*

| HU | Historia | SP | Rama | Jira |
|---|---|---|---|---|
| HU-01 | Inicio de sesión al sistema | 3 | `feature/HU-01-inicio-sesion` | SCRUM-2 |
| HU-04 | Registro de datos del importador (cliente) | 5 | `feature/HU-04-registro-importador` | SCRUM-9 |
| HU-03 | Registro de nuevo despacho de importación | 5 | `feature/HU-03-registro-despacho` | SCRUM-10 |
| HU-02 | Panel de seguimiento de despachos (Dashboard) | 5 | `feature/HU-02-dashboard` | SCRUM-11 |
| HU-05 | Registro de información del embarque marítimo (BL) | 5 | `feature/HU-05-registro-embarque-bl` | SCRUM-12 |

Detalle: [`docs/product-backlog.md`](docs/product-backlog.md) · [`docs/sprint-1/sprint-backlog.md`](docs/sprint-1/sprint-backlog.md)

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
