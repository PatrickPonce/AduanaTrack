package pe.edu.usmp.aduanatrack.liquidacion.dto;

import java.math.BigDecimal;

/**
 * Liquidación referencial en POST /despachos/{id}/liquidacion (contrato C3 · T-08.2), con el
 * desglose FOB → CIF → cada tributo → total. {@code estado} es el del despacho tras generarla
 * ({@code LIQUIDADO}).
 */
public record LiquidacionResponse(
        BigDecimal fob,
        BigDecimal flete,
        BigDecimal seguro,
        BigDecimal cif,
        BigDecimal adValorem,
        BigDecimal igv,
        BigDecimal ipm,
        BigDecimal percepcion,
        BigDecimal total,
        String moneda,
        ResultadoCalculo.Tasas tasas,
        String estado) {

    public static final String MONEDA = "USD";

    public static LiquidacionResponse de(ResultadoCalculo calculo, String estado) {
        return new LiquidacionResponse(calculo.fob(), calculo.flete(), calculo.seguro(), calculo.cif(),
                calculo.adValorem(), calculo.igv(), calculo.ipm(), calculo.percepcion(), calculo.total(), MONEDA,
                calculo.tasas(), estado);
    }
}
