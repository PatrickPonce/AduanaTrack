package pe.edu.usmp.aduanatrack.liquidacion.dto;

import java.math.BigDecimal;

/**
 * Dato mínimo de un ítem para el cálculo (HU-08 · T-08.1): su valor FOB y la tasa Ad Valorem de su
 * partida en porcentaje ({@code 6.00} es 6 %). El cálculo no conoce entidades ni despachos.
 */
public record ItemCalculo(BigDecimal valorFob, BigDecimal tasaAdValorem) {
}
