# Política de seguridad

## Herramientas de revisión de vulnerabilidades

| Herramienta | Qué revisa | Dónde se ejecuta |
|---|---|---|
| **CodeQL** (GitHub) | Análisis estático (SAST) del código fuente y de los workflows | `codeql.yml` |
| **Semgrep** | SAST con reglas OWASP Top 10 / buenas prácticas por lenguaje | `security-scan.yml` |
| **Gitleaks** | Secretos expuestos (claves, tokens, contraseñas) en todo el historial | `security-scan.yml` |
| **Trivy** | Dependencias vulnerables (SCA), Dockerfiles/IaC mal configurados y secretos | `security-scan.yml` |
| **Dependabot** | Alertas y PRs automáticos de actualización de dependencias | `dependabot.yml` |
| SonarQube Cloud *(opcional)* | Calidad + vulnerabilidades, Quality Gate | se activa al crear el secreto `SONAR_TOKEN` |
| Snyk *(opcional)* | SCA de dependencias | se activa al crear el secreto `SNYK_TOKEN` |

## Criterio de aceptación (Quality Gate del equipo)

- 0 hallazgos **críticos** y 0 **altos** abiertos al cierre de cada sprint.
- Los hallazgos medios/bajos se registran en Jira como `fix/SCRUM-NN` y se planifican.

## Reportar una vulnerabilidad

Crea una incidencia en Jira (proyecto SCRUM) con la etiqueta `seguridad` o avisa al Scrum Master. No publiques detalles de la vulnerabilidad en issues públicos.
