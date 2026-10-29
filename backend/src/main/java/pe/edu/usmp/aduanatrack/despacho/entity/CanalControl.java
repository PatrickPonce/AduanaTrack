package pe.edu.usmp.aduanatrack.despacho.entity;

/** Canal de control que informa SUNAT tras numerar la DAM (HU-10). Se guarda como texto en {@code despacho.canal_control}. */
public enum CanalControl {
    VERDE,
    NARANJA,
    ROJO;

    /** Solo en canal verde se puede ir a levante (HU-13, Sprint 3). */
    public boolean levanteHabilitado() {
        return this == VERDE;
    }
}
