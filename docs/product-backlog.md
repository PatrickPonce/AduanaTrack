# Product Backlog — AduanaTrack

Fuente: `Product_Backlog_AduanaTrack.xlsx` (PO: Altamirano Zuñiga, Juan Diego). 15 historias · **85 SP**. Sprint 1: 23 SP · Sprint 2: 34 SP · Sprint 3: 28 SP.

| Código | Historia de usuario | Prioridad | SP | Dependencia | Sprint | Jira |
|---|---|---|---:|---|---|---|
| HU-01 | Inicio de sesión al sistema | Crítica | 3 | Ninguna | Sprint 1 | [SCRUM-2](https://grupo2dis.atlassian.net/browse/SCRUM-2) |
| HU-02 | Panel de seguimiento de despachos (Dashboard) | Alta | 5 | HU-01 (obligatoria) | Sprint 1 | [SCRUM-11](https://grupo2dis.atlassian.net/browse/SCRUM-11) |
| HU-03 | Registro de nuevo despacho de importación | Crítica | 5 | HU-04 (obligatoria) | Sprint 1 | [SCRUM-10](https://grupo2dis.atlassian.net/browse/SCRUM-10) |
| HU-04 | Registro de datos del importador (cliente) | Alta | 5 | Ninguna | Sprint 1 | [SCRUM-9](https://grupo2dis.atlassian.net/browse/SCRUM-9) |
| HU-05 | Registro de información del embarque marítimo (BL) | Alta | 5 | HU-03 (obligatoria) | Sprint 1 | [SCRUM-12](https://grupo2dis.atlassian.net/browse/SCRUM-12) |
| HU-06 | Carga y gestión de documentos del despacho | Crítica | 8 | HU-05 (obligatoria) | Sprint 2 | [SCRUM-13](https://grupo2dis.atlassian.net/browse/SCRUM-13) |
| HU-07 | Registro de clasificación arancelaria de mercancías | Crítica | 8 | HU-06 (obligatoria) | Sprint 2 | [SCRUM-14](https://grupo2dis.atlassian.net/browse/SCRUM-14) |
| HU-08 | Cálculo de tributos y liquidación aduanera | Crítica | 8 | HU-07 (obligatoria) | Sprint 2 | [SCRUM-15](https://grupo2dis.atlassian.net/browse/SCRUM-15) |
| HU-09 | Registro manual de la numeración de la DAM | Crítica | 5 | HU-08 (obligatoria) | Sprint 2 | [SCRUM-16](https://grupo2dis.atlassian.net/browse/SCRUM-16) |
| HU-10 | Registro y seguimiento del canal de control asignado | Alta | 5 | HU-09 (obligatoria) | Sprint 2 | [SCRUM-17](https://grupo2dis.atlassian.net/browse/SCRUM-17) |
| HU-11 | Registro de citas en depósito temporal o almacén | Alta | 5 | HU-10 (obligatoria) | Sprint 3 | [SCRUM-18](https://grupo2dis.atlassian.net/browse/SCRUM-18) |
| HU-12 | Registro del resultado de la inspección aduanera | Alta | 5 | HU-11 (obligatoria) | Sprint 3 | [SCRUM-19](https://grupo2dis.atlassian.net/browse/SCRUM-19) |
| HU-13 | Registro manual del levante de mercancía | Crítica | 5 | HU-12 (obligatoria) | Sprint 3 | [SCRUM-20](https://grupo2dis.atlassian.net/browse/SCRUM-20) |
| HU-14 | Notificaciones y alertas de estado del despacho | Alta | 5 | Ninguna | Sprint 3 | [SCRUM-21](https://grupo2dis.atlassian.net/browse/SCRUM-21) |
| HU-15 | Reportes e indicadores de gestión aduanera | Media | 8 | HU-02, HU-10 (discrecional) | Sprint 3 | [SCRUM-22](https://grupo2dis.atlassian.net/browse/SCRUM-22) |

## Declaraciones

- **HU-01 · Inicio de sesión al sistema** — Como usuario del sistema (agente de aduana, cliente o administrador), deseo iniciar sesión con mis credenciales corporativas, para acceder de forma segura a la información de los despachos que gestiono.
- **HU-02 · Panel de seguimiento de despachos (Dashboard)** — Como agente de aduana, deseo visualizar un panel general con el estado de todos los despachos, para priorizar mi gestión diaria según estado y urgencia.
- **HU-03 · Registro de nuevo despacho de importación** — Como agente de aduana, deseo registrar un nuevo despacho de importación marítima para un cliente, para iniciar formalmente la trazabilidad interna del despacho.
- **HU-04 · Registro de datos del importador (cliente)** — Como administrador o agente de aduana, deseo registrar y mantener actualizados los datos del cliente importador, para contar con información fiscal y de contacto válida.
- **HU-05 · Registro de información del embarque marítimo (BL)** — Como agente de aduana, deseo registrar la información del embarque marítimo y el BL asociado al despacho, para contar con los datos logísticos necesarios para la clasificación.
- **HU-06 · Carga y gestión de documentos del despacho** — Como agente de aduana, deseo cargar y gestionar los documentos digitales requeridos para el despacho, para asegurar el sustento documentario completo.
- **HU-07 · Registro de clasificación arancelaria de mercancías** — Como analista de comercio exterior, deseo registrar la clasificación arancelaria de cada ítem, para dejar constancia interna de la partida aplicable.
- **HU-08 · Cálculo de tributos y liquidación aduanera** — Como agente de aduana, deseo calcular referencialmente los tributos aplicables, para contar con una liquidación estimada antes del pago externo.
- **HU-09 · Registro manual de la numeración de la DAM** — Como agente de aduana, deseo registrar manualmente el número de DAM obtenido en el sistema externo, para dejar constancia del número oficial.
- **HU-10 · Registro y seguimiento del canal de control asignado** — Como agente de aduana, deseo registrar el canal de control informado por SUNAT y dar seguimiento a la línea de tiempo, para anticipar acciones según el canal.
- **HU-11 · Registro de citas en depósito temporal o almacén** — Como agente de aduana, deseo registrar los datos de la cita de inspección o retiro confirmada en la plataforma del depósito, para mantener trazabilidad.
- **HU-12 · Registro del resultado de la inspección aduanera** — Como agente de aduana, deseo registrar el resultado de la inspección realizada en el depósito, para documentar el desenlace y habilitar el levante.
- **HU-13 · Registro manual del levante de mercancía** — Como agente de aduana, deseo registrar el levante obtenido manualmente en SUNAT Operaciones en Línea, para habilitar la coordinación de retiro.
- **HU-14 · Notificaciones y alertas de estado del despacho** — Como agente de aduana y cliente/importador, deseo recibir notificaciones sobre eventos relevantes de mis despachos, para actuar oportunamente ante vencimientos o riesgos.
- **HU-15 · Reportes e indicadores de gestión aduanera** — Como jefe de operaciones o administrador, deseo visualizar reportes e indicadores de gestión, para tomar decisiones basadas en datos.

Los criterios de aceptación (Dado/Cuando/Entonces) de cada historia están en su incidencia de Jira.
