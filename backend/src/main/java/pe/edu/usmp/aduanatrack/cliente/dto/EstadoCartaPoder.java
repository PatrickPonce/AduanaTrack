package pe.edu.usmp.aduanatrack.cliente.dto;

public enum EstadoCartaPoder {
    VIGENTE,
    /** Vence dentro de DIAS_ALERTA_CARTA_PODER (15 días por defecto). */
    POR_VENCER,
    VENCIDA,
    SIN_CARTA;

    /** Alerta crítica del Dashboard: carta vencida o por vencer (sección 2.6 de la guía). */
    public boolean esCritico() {
        return this == POR_VENCER || this == VENCIDA;
    }
}
