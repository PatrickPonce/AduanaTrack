package pe.edu.usmp.aduanatrack.config.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.config.entity.ParametroSistema;
import pe.edu.usmp.aduanatrack.config.repository.ParametroSistemaRepository;

/** Lee las reglas de negocio configurables de {@code parametro_sistema} (sin números fijos en el código). */
@Service
public class ParametroService {

    public static final String DIAS_ALMACENAJE = "DIAS_ALMACENAJE";
    public static final String DIAS_ALERTA_ALMACENAJE = "DIAS_ALERTA_ALMACENAJE";
    public static final String DIAS_ALERTA_CARTA_PODER = "DIAS_ALERTA_CARTA_PODER";

    /** Tasas de la liquidación referencial (HU-08), en porcentaje: {@code 16} es 16 %. */
    public static final String TASA_IGV = "TASA_IGV";
    public static final String TASA_IPM = "TASA_IPM";
    public static final String TASA_PERCEPCION = "TASA_PERCEPCION";

    private final ParametroSistemaRepository repositorio;

    public ParametroService(ParametroSistemaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public int entero(String clave) {
        String valor = valor(clave);
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El parámetro " + clave + " no es un número: " + valor, e);
        }
    }

    /** Parámetro numérico con decimales (p. ej. {@code 3.5}); el separador decimal es el punto. */
    @Transactional(readOnly = true)
    public BigDecimal decimal(String clave) {
        String valor = valor(clave);
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El parámetro " + clave + " no es un número: " + valor, e);
        }
    }

    private String valor(String clave) {
        return repositorio.findById(clave)
                .map(ParametroSistema::getValor)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro del sistema " + clave));
    }
}
