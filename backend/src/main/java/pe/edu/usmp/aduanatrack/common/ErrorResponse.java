package pe.edu.usmp.aduanatrack.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato único de error del API: {"codigo": "...", "mensaje": "...", "campos": {...}}.
 * Los {@code datos} adicionales se aplanan en la raíz del JSON.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String codigo, String mensaje, Map<String, String> campos,
        @JsonAnyGetter Map<String, Object> datos) {

    public static ErrorResponse of(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, null, null);
    }
}
