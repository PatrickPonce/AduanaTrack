# Guía de contribución — AduanaTrack

## Modelo de ramas (GitFlow)

| Rama | Propósito | Se crea desde | Se integra en |
|---|---|---|---|
| `main` | Versión estable entregada al cierre de cada sprint (tags `v0.X.0`) | — | — |
| `develop` | Integración continua del sprint en curso | `main` | `main` (cierre de sprint) |
| `feature/HU-XX-descripcion` | Una rama por historia de usuario | `develop` | `develop` |
| `hotfix/descripcion` | Correcciones urgentes sobre `main` (p. ej. vulnerabilidades críticas) | `main` | `main` y `develop` |
| `fix/SCRUM-NN-descripcion` | Corrección de un hallazgo del escaneo de seguridad | `develop` | `develop` |

- `main` y `develop` están protegidas: no se hace push directo, solo Pull Requests.
- Antes de abrir el PR, actualiza tu rama: `git pull --rebase origin develop`.

## Commits

Formato: `SCRUM-NN <verbo en presente> <qué>` — la clave de Jira permite trazar el commit con la tarea.

```
SCRUM-27 agrega registro de intentos de acceso en log de auditoría
SCRUM-31 valida RUC de 11 dígitos al guardar cliente
```

## Pull Requests

- Título: `HU-XX · SCRUM-NN · resumen`.
- Debe pasar **CI** y **Security Scan** (0 hallazgos críticos/altos nuevos).
- Mínimo **1 aprobación** de otro integrante.
- Al hacer merge, mueve la subtarea en Jira a **En revisión** / **Finalizado**.

## Definición de Hecho (DoD)

- [ ] Cumple los 4 criterios de aceptación de la HU (Dado/Cuando/Entonces)
- [ ] Pruebas de la HU ejecutadas por QA
- [ ] Security Scan sin hallazgos críticos ni altos
- [ ] PR aprobado e integrado en `develop`
- [ ] Subtarea actualizada en Jira
