package pe.edu.usmp.aduanatrack.notificacion.entity;

/**
 * Evento que origina la notificación (HU-14). Quien llame a {@code NotificacionService.notificar} con un
 * evento nuevo lo agrega aquí; la columna notificacion.tipo admite hasta 40 caracteres.
 */
public enum TipoNotificacion {
    /** HU-12: la inspección terminó con observaciones (severidad ALTA). */
    INSPECCION_CON_OBSERVACIONES,
    /** HU-12: la inspección terminó conforme y el levante quedó habilitado. */
    INSPECCION_CONFORME,
    /** HU-05: el plazo de almacenaje está por vencer. */
    PLAZO_ALMACENAJE_POR_VENCER,
    /** HU-04: la carta poder del cliente está por vencer. */
    CARTA_PODER_POR_VENCER,
    /** Cambio de estado del despacho que el usuario debe conocer. */
    CAMBIO_ESTADO
}
