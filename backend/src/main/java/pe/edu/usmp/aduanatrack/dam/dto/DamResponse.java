package pe.edu.usmp.aduanatrack.dam.dto;

import java.time.LocalDate;

/**
 * Numeración de la DAM del despacho (HU-09, contrato C4). {@code numeroDam} y
 * {@code fechaNumeracion} son nulos mientras no se registra; {@code nombreCargo} es nulo si no se
 * adjuntó el cargo. {@code estado} es el del despacho, como texto.
 */
public record DamResponse(String numeroDam, LocalDate fechaNumeracion, String nombreCargo, String estado) {
}
