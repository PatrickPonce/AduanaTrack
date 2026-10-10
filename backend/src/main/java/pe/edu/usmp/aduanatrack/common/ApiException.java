package pe.edu.usmp.aduanatrack.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que el {@link GlobalExceptionHandler} convierte al formato único
 * {"codigo", "mensaje", "campos"}. {@code campos} marca los campos con error (400) y
 * {@code datos} viaja junto al error cuando el frontend necesita información extra
 * (p. ej. el cliente existente en un 409).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final transient Map<String, String> campos;
    private final transient Map<String, Object> datos;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        this(status, codigo, mensaje, null, null);
    }

    public ApiException(HttpStatus status, String codigo, String mensaje, Map<String, Object> datos) {
        this(status, codigo, mensaje, null, datos);
    }

    private ApiException(HttpStatus status, String codigo, String mensaje, Map<String, String> campos,
            Map<String, Object> datos) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.campos = campos;
        this.datos = datos;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, String> getCampos() {
        return campos;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, mensaje);
    }

    /** 400 VALIDACION con los campos que el frontend debe resaltar. */
    public static ApiException validacion(Map<String, String> campos) {
        return new ApiException(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, "Revise los campos marcados.",
                Map.copyOf(campos), null);
    }
}
