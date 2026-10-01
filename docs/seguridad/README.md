# Revisión de vulnerabilidades — procedimiento por sprint

Directiva del docente: *ejecutar la herramienta para que revise el código al finalizar el sprint, mostrar el reporte generado y luego las correcciones realizadas.*

## Herramientas (workflow `Security Scan` + `CodeQL`)

| Herramienta | Tipo | Qué detecta |
|---|---|---|
| CodeQL | SAST | Inyecciones, XSS, validaciones faltantes, workflows inseguros |
| Semgrep | SAST | Reglas OWASP Top 10 y malas prácticas por lenguaje |
| Gitleaks | Secretos | Contraseñas, tokens y claves en todo el historial de Git |
| Trivy | SCA / IaC | Dependencias con CVE, Dockerfile/compose mal configurados, secretos |

Los resultados quedan en **Security → Code scanning** y en el artefacto **`reporte-vulnerabilidades-*`** de cada ejecución (pestaña **Actions**).

## Pasos al cierre de cada sprint (tarea T-00.6 en Jira)

1. **Escaneo inicial** — *Actions → Security Scan → Run workflow*, rama `develop`, etiqueta `sprint-N-inicial`.
2. Descargar el artefacto y copiar `reporte-vulnerabilidades.md` a `docs/seguridad/sprint-N/01-reporte-inicial.md`.
3. Registrar cada hallazgo Crítico/Alto (y los Medios relevantes) en `docs/seguridad/sprint-N/02-hallazgos-y-correcciones.md` y crear su subtarea en Jira.
4. Corregir en ramas `fix/SCRUM-NN-descripcion` → PR a `develop`.
5. **Re-escaneo** — ejecutar de nuevo con etiqueta `sprint-N-correcciones` y guardar `03-reporte-final.md`.
6. Presentar en el Sprint Review: reporte inicial → correcciones (commits/PR) → reporte final con Quality Gate aprobado.
