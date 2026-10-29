package pe.edu.usmp.aduanatrack.despacho.entity;

/**
 * Estados del despacho en el orden en que avanza. Sprint 1: BORRADOR y APERTURA; Sprint 2:
 * desde DOCUMENTACION_COMPLETA (HU-06) hasta CANAL_ASIGNADO (HU-10). Cada servicio valida el
 * estado anterior antes de avanzar y registra el hito con {@code HitoDespachoService}.
 */
public enum EstadoDespacho {
    BORRADOR,
    APERTURA,
    /** HU-06: todos los documentos obligatorios tienen versión vigente. */
    DOCUMENTACION_COMPLETA,
    /** HU-07: todos los ítems de la factura tienen partida arancelaria. */
    CLASIFICADO,
    /** HU-08: liquidación referencial generada. */
    LIQUIDADO,
    /** HU-08: pago de tributos registrado. */
    TRIBUTOS_PAGADOS,
    /** HU-09: N° de DAM registrado. */
    DAM_NUMERADA,
    /** HU-10: canal de control guardado (el color queda en {@code despacho.canal_control}). */
    CANAL_ASIGNADO;

    /** {@code true} si el despacho ya llegó a {@code otro} o lo pasó (p. ej. para habilitar una pestaña). */
    public boolean alcanzo(EstadoDespacho otro) {
        return ordinal() >= otro.ordinal();
    }
}
