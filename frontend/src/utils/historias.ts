/** Nombres oficiales de las historias de usuario (docs/product-backlog.md). Se muestran como título de cada pantalla. */
export const HISTORIAS = {
  'HU-01': 'Inicio de sesión al sistema',
  'HU-02': 'Panel de seguimiento de despachos (Dashboard)',
  'HU-03': 'Registro de nuevo despacho de importación',
  'HU-04': 'Registro de datos del importador (cliente)',
  'HU-05': 'Registro de información del embarque marítimo (BL)',
} as const

export type CodigoHu = keyof typeof HISTORIAS

/** Módulos de los Sprints 2 y 3: se muestran en el menú como próximos, sin enlace. */
export const PROXIMAS = [
  { hu: 'HU-06', texto: 'Documentos', sprint: 2 },
  { hu: 'HU-07', texto: 'Clasificación', sprint: 2 },
  { hu: 'HU-08', texto: 'Tributos', sprint: 2 },
  { hu: 'HU-09', texto: 'DAM (Registro)', sprint: 2 },
  { hu: 'HU-10', texto: 'Canal de Control', sprint: 2 },
  { hu: 'HU-11', texto: 'Depósitos / Citas', sprint: 3 },
  { hu: 'HU-12', texto: 'Inspecciones', sprint: 3 },
  { hu: 'HU-13', texto: 'Levante', sprint: 3 },
  { hu: 'HU-14', texto: 'Notificaciones', sprint: 3 },
  { hu: 'HU-15', texto: 'Reportes', sprint: 3 },
] as const
