# Sprint 1 — Sprint Backlog

- **Duración:** S7–S10 (14/09/2026 – 10/10/2026) · Sprint Planning 14/09/2026
- **Semanas de trabajo:** S7 (14–19/09), S9 (28/09–03/10) y S10 (05–10/10). La S8 (21–26/09) es semana de exámenes parciales: sin trabajo de sprint.
- **Sprint Goal:** Habilitar el flujo base de apertura y seguimiento inicial de despachos de importación marítima: acceso seguro al sistema, registro de clientes/importadores, apertura de un nuevo despacho y registro de la información de embarque (BL), con visibilidad centralizada del estado de los despachos en el Dashboard.
- **Compromiso:** 5 HU · 23 SP · 31 tareas · 109 h estimadas (capacidad neta ≈ 101 h)
- **Tablero:** https://grupo2dis.atlassian.net/jira/software/projects/SCRUM/boards/1

| Tarea | HU | Tipo | Descripción | Horas | Responsable | Jira |
|---|---|---|---|---:|---|---|
| T-00.1 | SETUP | DevOps | Configuración del repositorio Git (GitHub/GitLab), convención de ramas por historia, acceso del docente (jhuapalla@usmp.pe) | 2 | Fabio Saavedra | [SCRUM-1](https://grupo2dis.atlassian.net/browse/SCRUM-1) |
| T-00.2 | SETUP | CASE | Configuración del tablero Kanban (To Do / In Progress / Review / Done), acceso del docente (jhuapallag@usmp.pe) | 1 | Alvaro Tipian | [SCRUM-3](https://grupo2dis.atlassian.net/browse/SCRUM-3) |
| T-00.3 | SETUP | DevOps | Configuración de contenedores (app + base de datos) | 4 | Patrick Ponce | [SCRUM-23](https://grupo2dis.atlassian.net/browse/SCRUM-23) |
| T-00.4 | SETUP | Base Datos | Creación de la base de datos relacional (esquema inicial: usuarios, clientes, despachos, embarques) | 5 | Fabio Saavedra | [SCRUM-24](https://grupo2dis.atlassian.net/browse/SCRUM-24) |
| T-00.5 | SETUP | Calidad | Pipeline básico de CI (build + pruebas automáticas) | 3 | Patrick Ponce | [SCRUM-25](https://grupo2dis.atlassian.net/browse/SCRUM-25) |
| T-00.6 | SETUP | Calidad | Ejecución de herramienta de escaneo de vulnerabilidades sobre el código del sprint + informe de hallazgos y correcciones | 2 | Fabio Saavedra | [SCRUM-26](https://grupo2dis.atlassian.net/browse/SCRUM-26) |
| T-01.1 | HU-01 | Backend | Endpoint de autenticación + bloqueo tras 3 intentos fallidos (15 min) | 4 | Patrick Ponce | [SCRUM-4](https://grupo2dis.atlassian.net/browse/SCRUM-4) |
| T-01.2 | HU-01 | Backend | Registro de intentos de acceso en log de auditoría | 3 | Fabio Saavedra | [SCRUM-27](https://grupo2dis.atlassian.net/browse/SCRUM-27) |
| T-01.3 | HU-01 | Frontend | Pantalla de login: validaciones, checkbox “Recordarme” (sesión 8h) | 4 | Kiara Linares | [SCRUM-28](https://grupo2dis.atlassian.net/browse/SCRUM-28) |
| T-01.4 | HU-01 | QA | Pruebas de los 4 criterios de aceptación de HU-01 | 1 | Fabio Saavedra | [SCRUM-29](https://grupo2dis.atlassian.net/browse/SCRUM-29) |
| T-04.1 | HU-04 | Backend | Modelo de datos Cliente/Importador + validación de RUC (11 dígitos) | 5 | Patrick Ponce | [SCRUM-30](https://grupo2dis.atlassian.net/browse/SCRUM-30) |
| T-04.2 | HU-04 | Backend | Carga de carta poder (PDF) + alerta automática 15 días antes del vencimiento | 5 | Fabio Saavedra | [SCRUM-31](https://grupo2dis.atlassian.net/browse/SCRUM-31) |
| T-04.3 | HU-04 | Backend | Historial de cambios del cliente (auditoría) | 3 | Patrick Ponce | [SCRUM-32](https://grupo2dis.atlassian.net/browse/SCRUM-32) |
| T-04.4 | HU-04 | Frontend | Formulario de alta/edición de cliente + detección de duplicados | 5 | Kiara Linares | [SCRUM-33](https://grupo2dis.atlassian.net/browse/SCRUM-33) |
| T-04.5 | HU-04 | QA | Pruebas de los 4 criterios de aceptación de HU-04 | 2 | Kiara Linares | [SCRUM-34](https://grupo2dis.atlassian.net/browse/SCRUM-34) |
| T-03.1 | HU-03 | Backend | Generación automática de N° de despacho (formato DESP-AAAA-####) | 4 | Patrick Ponce | [SCRUM-35](https://grupo2dis.atlassian.net/browse/SCRUM-35) |
| T-03.2 | HU-03 | Backend | Listas maestras de régimen aduanero y aduana de destino | 3 | Fabio Saavedra | [SCRUM-36](https://grupo2dis.atlassian.net/browse/SCRUM-36) |
| T-03.3 | HU-03 | Backend | Validación de carta poder vigente del cliente al guardar | 3 | Patrick Ponce | [SCRUM-37](https://grupo2dis.atlassian.net/browse/SCRUM-37) |
| T-03.4 | HU-03 | Frontend | Formulario “Datos generales del despacho” + “Guardar como borrador” | 6 | Kiara Linares | [SCRUM-38](https://grupo2dis.atlassian.net/browse/SCRUM-38) |
| T-03.5 | HU-03 | Frontend | Estado inicial “Apertura” visible en la UI del despacho | 2 | Kiara Linares | [SCRUM-39](https://grupo2dis.atlassian.net/browse/SCRUM-39) |
| T-03.6 | HU-03 | QA | Pruebas de los 4 criterios de aceptación de HU-03 | 2 | Fabio Saavedra | [SCRUM-40](https://grupo2dis.atlassian.net/browse/SCRUM-40) |
| T-02.1 | HU-02 | Backend | Endpoints de indicadores (activos, canal naranja/rojo, levante, alertas) | 5 | Fabio Saavedra | [SCRUM-41](https://grupo2dis.atlassian.net/browse/SCRUM-41) |
| T-02.2 | HU-02 | Backend | Filtro por aduana/cliente + exportar tabla a Excel | 4 | Patrick Ponce | [SCRUM-42](https://grupo2dis.atlassian.net/browse/SCRUM-42) |
| T-02.3 | HU-02 | Frontend | Panel de indicadores + tabla “Despachos recientes” ordenada por ETA | 6 | Kiara Linares | [SCRUM-43](https://grupo2dis.atlassian.net/browse/SCRUM-43) |
| T-02.4 | HU-02 | Frontend | Navegación desde la tabla al detalle del despacho | 3 | Kiara Linares | [SCRUM-44](https://grupo2dis.atlassian.net/browse/SCRUM-44) |
| T-02.5 | HU-02 | QA | Pruebas de los 4 criterios de aceptación de HU-02 | 2 | Fabio Saavedra | [SCRUM-45](https://grupo2dis.atlassian.net/browse/SCRUM-45) |
| T-05.1 | HU-05 | Backend | Modelo Embarque/BL + asociación al despacho | 4 | Patrick Ponce | [SCRUM-46](https://grupo2dis.atlassian.net/browse/SCRUM-46) |
| T-05.2 | HU-05 | Backend | Validación de formato de contenedor (estándar ISO 6346) | 4 | Fabio Saavedra | [SCRUM-47](https://grupo2dis.atlassian.net/browse/SCRUM-47) |
| T-05.3 | HU-05 | Backend | Recálculo automático del plazo de almacenaje al cambiar la fecha ETA + alerta | 4 | Patrick Ponce | [SCRUM-48](https://grupo2dis.atlassian.net/browse/SCRUM-48) |
| T-05.4 | HU-05 | Frontend | Formulario de embarque + tabla de contenedores (múltiples por despacho) | 6 | Kiara Linares | [SCRUM-49](https://grupo2dis.atlassian.net/browse/SCRUM-49) |
| T-05.5 | HU-05 | QA | Pruebas de los 4 criterios de aceptación de HU-05 | 2 | Fabio Saavedra | [SCRUM-50](https://grupo2dis.atlassian.net/browse/SCRUM-50) |
| | | | **Total** | **109** | | |
