package pe.edu.usmp.aduanatrack.liquidacion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ItemCalculo;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo.Tasas;

/**
 * Cálculo referencial del CIF y los tributos (HU-08 · T-08.1). Todo en {@link BigDecimal}, con
 * 2 decimales y {@link RoundingMode#HALF_UP} en cada concepto:
 *
 * <pre>
 * CIF        = FOB + flete + seguro
 * Ad Valorem = suma, por ítem, de (CIF del ítem × tasa de su partida)
 * IGV        = (CIF + Ad Valorem) × TASA_IGV
 * IPM        = (CIF + Ad Valorem) × TASA_IPM
 * Percepción = (CIF + Ad Valorem + IGV + IPM) × TASA_PERCEPCION
 * </pre>
 *
 * El flete y el seguro se reparten entre los ítems según su FOB, porque cada partida tiene su
 * propia tasa Ad Valorem. Las tasas salen de {@code parametro_sistema}; no hay tasas fijas aquí.
 */
@Service
public class CalculoTributosService {

    private static final int DECIMALES = 2;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ParametroService parametros;

    public CalculoTributosService(ParametroService parametros) {
        this.parametros = parametros;
    }

    /** Calcula con las tasas vigentes de {@code parametro_sistema}. */
    public ResultadoCalculo calcular(List<ItemCalculo> items, BigDecimal flete, BigDecimal seguro) {
        return calcular(items, flete, seguro, new Tasas(
                parametros.decimal(ParametroService.TASA_IGV),
                parametros.decimal(ParametroService.TASA_IPM),
                parametros.decimal(ParametroService.TASA_PERCEPCION)));
    }

    /**
     * Calcula con las tasas indicadas (porcentajes). 400 {@code VALIDACION} si el flete o el seguro
     * son negativos; {@link IllegalArgumentException} si no hay ítems o algún ítem no tiene FOB
     * positivo ni tasa Ad Valorem, porque son errores de programación del llamador (HU-07 ya los
     * impide).
     */
    public ResultadoCalculo calcular(List<ItemCalculo> items, BigDecimal flete, BigDecimal seguro, Tasas tasas) {
        validar(items, flete, seguro);
        BigDecimal fleteRedondeado = redondear(flete);
        BigDecimal seguroRedondeado = redondear(seguro);

        BigDecimal fob = items.stream().map(i -> redondear(i.valorFob())).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> fletes = repartir(fleteRedondeado, items, fob);
        List<BigDecimal> seguros = repartir(seguroRedondeado, items, fob);

        BigDecimal adValorem = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            ItemCalculo item = items.get(i);
            BigDecimal cifItem = redondear(item.valorFob()).add(fletes.get(i)).add(seguros.get(i));
            adValorem = adValorem.add(porcentaje(cifItem, item.tasaAdValorem()));
        }

        BigDecimal cif = fob.add(fleteRedondeado).add(seguroRedondeado);
        BigDecimal baseIgv = cif.add(adValorem);
        BigDecimal igv = porcentaje(baseIgv, tasas.igv());
        BigDecimal ipm = porcentaje(baseIgv, tasas.ipm());
        BigDecimal percepcion = porcentaje(baseIgv.add(igv).add(ipm), tasas.percepcion());
        BigDecimal total = adValorem.add(igv).add(ipm).add(percepcion);
        return new ResultadoCalculo(fob, fleteRedondeado, seguroRedondeado, cif, adValorem, igv, ipm, percepcion,
                total, tasas);
    }

    /**
     * Reparte {@code monto} entre los ítems en proporción a su FOB, con 2 decimales. La diferencia de
     * redondeo se suma al último ítem para que el reparto sume exactamente {@code monto}.
     */
    private static List<BigDecimal> repartir(BigDecimal monto, List<ItemCalculo> items, BigDecimal fobTotal) {
        BigDecimal[] partes = new BigDecimal[items.size()];
        BigDecimal repartido = BigDecimal.ZERO;
        for (int i = 0; i < items.size() - 1; i++) {
            partes[i] = redondear(monto.multiply(items.get(i).valorFob()).divide(fobTotal, 10, RoundingMode.HALF_UP));
            repartido = repartido.add(partes[i]);
        }
        partes[items.size() - 1] = monto.subtract(repartido);
        return List.of(partes);
    }

    /** {@code base × porcentaje / 100}, con 2 decimales HALF_UP. */
    private static BigDecimal porcentaje(BigDecimal base, BigDecimal porcentaje) {
        return redondear(base.multiply(porcentaje).divide(CIEN, 10, RoundingMode.HALF_UP));
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    private static void validar(List<ItemCalculo> items, BigDecimal flete, BigDecimal seguro) {
        if (flete == null || flete.signum() < 0) {
            throw ApiException.validacion(Map.of("flete", "El flete no puede ser negativo"));
        }
        if (seguro == null || seguro.signum() < 0) {
            throw ApiException.validacion(Map.of("seguro", "El seguro no puede ser negativo"));
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El cálculo necesita al menos un ítem");
        }
        for (ItemCalculo item : items) {
            if (item.valorFob() == null || redondear(item.valorFob()).signum() <= 0) {
                throw new IllegalArgumentException("Todos los ítems deben tener un valor FOB mayor a 0");
            }
            if (item.tasaAdValorem() == null) {
                throw new IllegalArgumentException("Todos los ítems deben tener la tasa Ad Valorem de su partida");
            }
        }
    }
}
