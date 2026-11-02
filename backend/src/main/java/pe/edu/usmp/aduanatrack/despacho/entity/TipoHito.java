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
    INSPECCION_PROGRAMADA,
    INSPECCION_CONFORME,
    LEVANTE_OTORGADO,
    RETIRO_PROGRAMADO,
    /** Cita en depósito registrada (HU-11); el cambio de estado que provoca tiene su propio hito. */
    CITA_REGISTRADA,
    /** Inspección con ítems no conformes (HU-12); no cambia el estado. */
    INSPECCION_CON_OBSERVACIONES,
    /** Levante registrado sin los requisitos previos, con confirmación del agente (HU-13). */
    LEVANTE_CON_ADVERTENCIA,
    /** Corrección del N° de DAM o de su fecha de numeración (HU-09); no cambia el estado. */
    DAM_RECTIFICADA,
    /** Acción que el sistema propone al agente; no es un cambio de estado. */
    HITO_SUGERIDO
}
