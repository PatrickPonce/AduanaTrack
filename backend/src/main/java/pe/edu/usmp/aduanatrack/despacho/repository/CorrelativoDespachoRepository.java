package pe.edu.usmp.aduanatrack.despacho.repository;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Correlativo anual de despachos en la tabla {@code correlativo_despacho}. Una sola sentencia
 * atómica (UPSERT ... RETURNING) bloquea la fila del año, así que dos usuarios simultáneos
 * nunca obtienen el mismo número. Participa en la transacción del despacho: si el guardado
 * falla, el número no se consume.
 */
@Repository
public class CorrelativoDespachoRepository {

    private static final String SIGUIENTE = """
            INSERT INTO correlativo_despacho (anio, ultimo_numero) VALUES (:anio, 1)
            ON CONFLICT (anio) DO UPDATE SET ultimo_numero = correlativo_despacho.ultimo_numero + 1
            RETURNING ultimo_numero
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CorrelativoDespachoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int siguiente(int anio) {
        Integer numero = jdbc.queryForObject(SIGUIENTE, Map.of("anio", anio), Integer.class);
        if (numero == null) {
            throw new IllegalStateException("No se pudo generar el correlativo del año " + anio);
        }
        return numero;
    }
}
