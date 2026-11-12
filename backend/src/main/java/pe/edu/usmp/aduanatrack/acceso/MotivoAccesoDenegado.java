package pe.edu.usmp.aduanatrack.acceso;

/** Por qué se bloqueó el acceso de un usuario CLIENTE a un despacho (HU-16). */
public enum MotivoAccesoDenegado {
    /** El despacho existe pero es de otra empresa. */
    OTRA_EMPRESA,
    /** El usuario CLIENTE todavía no tiene empresa asignada. */
    SIN_EMPRESA,
    /** El despacho es de su empresa pero sigue en borrador: el cliente no lo ve. */
    BORRADOR
}
