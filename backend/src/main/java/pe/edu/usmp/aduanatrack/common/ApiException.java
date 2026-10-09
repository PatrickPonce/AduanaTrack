package pe.edu.usmp.aduanatrack.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que el {@link GlobalExceptionHandler} convierte al formato único
 * {"codigo", "mensaje", "campos"}. {@code datos} viaja junto al error cuando el
 * frontend necesita información extra (p. ej. el cliente existente en un 409).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final transient Map<String, Object> datos;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        this(status, codigo, mensaje, null);
    }

    public ApiException(HttpStatus status, String codigo, String mensaje, Map<String, Object> datos) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.datos = datos;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, mensaje);
    }
}
