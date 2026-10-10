package pe.edu.usmp.aduanatrack.config.service;

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

    private final ParametroSistemaRepository repositorio;

    public ParametroService(ParametroSistemaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public int entero(String clave) {
        String valor = repositorio.findById(clave)
                .map(ParametroSistema::getValor)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro del sistema " + clave));
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El parámetro " + clave + " no es un número: " + valor, e);
        }
    }
}
