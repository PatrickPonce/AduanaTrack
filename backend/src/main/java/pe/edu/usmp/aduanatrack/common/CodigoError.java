package pe.edu.usmp.aduanatrack.common;

/** Códigos del formato único de error (Guía Sprint 1, sección 2.4). */
public final class CodigoError {

    public static final String VALIDACION = "VALIDACION";
    public static final String CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS";
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    public static final String CLIENTE_DUPLICADO = "CLIENTE_DUPLICADO";
    public static final String ARCHIVO_MUY_GRANDE = "ARCHIVO_MUY_GRANDE";
    public static final String FORMATO_NO_PERMITIDO = "FORMATO_NO_PERMITIDO";
    public static final String CARTA_PODER_NO_VIGENTE = "CARTA_PODER_NO_VIGENTE";
    public static final String CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA";
    public static final String DESPACHO_EN_BORRADOR = "DESPACHO_EN_BORRADOR";
    /** HU-06: otra carga del mismo documento terminó primero; se vuelve a intentar. */
    public static final String CARGA_SIMULTANEA = "CARGA_SIMULTANEA";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    private CodigoError() {
    }
}
