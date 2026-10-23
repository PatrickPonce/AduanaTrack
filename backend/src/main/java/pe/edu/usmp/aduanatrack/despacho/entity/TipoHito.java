package pe.edu.usmp.aduanatrack.despacho.entity;

/**
 * Tipos de hito de la línea de tiempo (HU-10 · T-10.1). Los de cambio de estado se llaman
 * igual que el {@link EstadoDespacho} al que llega el despacho.
 */
public enum TipoHito {
    APERTURA,
    DOCUMENTACION_COMPLETA,
    CLASIFICADO,
    LIQUIDADO,
    TRIBUTOS_PAGADOS,
    DAM_NUMERADA,
    CANAL_ASIGNADO,
    /** Acción que el sistema propone al agente; no es un cambio de estado. */
    HITO_SUGERIDO
}
