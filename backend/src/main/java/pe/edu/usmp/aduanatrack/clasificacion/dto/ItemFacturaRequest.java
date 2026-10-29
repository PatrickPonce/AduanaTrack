package pe.edu.usmp.aduanatrack.clasificacion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Fila de «Ítems de la factura comercial» en PUT /despachos/{id}/items (contrato C2 · T-07.2).
 * El PUT recibe la lista completa: las filas con {@code id} se actualizan, las nuevas llegan sin
 * {@code id} y las que no vienen se quitan. {@code partidaId} es opcional hasta clasificar (HU-07).
 */
public record ItemFacturaRequest(
        Long id,
        @NotBlank(message = "Ingrese la descripción del ítem")
        @Size(max = 500, message = "Máximo 500 caracteres")
        String descripcion,
        @NotNull(message = "Ingrese la cantidad")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 12, fraction = 3, message = "Cantidad no válida (máximo 3 decimales)")
        BigDecimal cantidad,
        @NotBlank(message = "Ingrese la unidad")
        @Size(max = 10, message = "Máximo 10 caracteres")
        String unidad,
        @NotNull(message = "Ingrese el valor FOB")
        @DecimalMin(value = "0.01", message = "El valor FOB debe ser mayor a 0")
        @Digits(integer = 13, fraction = 2, message = "Valor FOB no válido (USD, máximo 2 decimales)")
        BigDecimal valorFob,
        Long partidaId) {
}
