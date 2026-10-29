package pe.edu.usmp.aduanatrack.liquidacion.dto;

import java.math.BigDecimal;

/**
 * Resultado de {@code CalculoTributosService} (HU-08 · T-08.1), en USD con 2 decimales.
 * {@code total} es la suma de los tributos (Ad Valorem + IGV + IPM + percepción), sin el CIF.
 * {@code tasas} son las que se usaron, para guardarlas con la liquidación.
 */
public record ResultadoCalculo(
        BigDecimal fob,
        BigDecimal flete,
        BigDecimal seguro,
        BigDecimal cif,
        BigDecimal adValorem,
        BigDecimal igv,
        BigDecimal ipm,
        BigDecimal percepcion,
        BigDecimal total,
        Tasas tasas) {

    /** Tasas en porcentaje: {@code 16} es 16 %. */
    public record Tasas(BigDecimal igv, BigDecimal ipm, BigDecimal percepcion) {
    }
}
