package pe.edu.usmp.aduanatrack.clasificacion.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.clasificacion.dto.PartidaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.repository.PartidaReferencialRepository;

/**
 * Buscador de partida arancelaria (referencial) de HU-07 · T-07.1: por código o por palabras de
 * la descripción, sin distinguir mayúsculas ni tildes, con un máximo de {@value #MAXIMO} resultados.
 */
@Service
public class PartidaService {

    static final int MAXIMO = 20;

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern SOLO_DIGITOS = Pattern.compile("[0-9]+");
    /** Los códigos son solo dígitos, así que este patrón nunca coincide. */
    private static final String SIN_CODIGO = "#";

    private final PartidaReferencialRepository repositorio;

    public PartidaService(PartidaReferencialRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * {@code q} vacío devuelve lista vacía. Las palabras se buscan en orden dentro de la
     * descripción; un texto de solo dígitos (con o sin puntos, p. ej. {@code 8482.10}) también busca
     * por el principio del código.
     */
    @Transactional(readOnly = true)
    public List<PartidaResponse> buscar(String q) {
        String texto = normalizar(q);
        if (texto.isEmpty()) {
            return List.of();
        }
        String patron = "%" + texto.replace(' ', '%') + "%";
        String soloDigitos = texto.replace(" ", "");
        String codigo = SOLO_DIGITOS.matcher(soloDigitos).matches() ? soloDigitos + "%" : SIN_CODIGO;
        return repositorio.buscar(patron, codigo, PageRequest.of(0, MAXIMO)).stream()
                .map(PartidaResponse::de)
                .toList();
    }

    /** Minúsculas, sin tildes y con cualquier signo (incluidos {@code %} y {@code _}) convertido en espacio. */
    static String normalizar(String q) {
        if (q == null) {
            return "";
        }
        String sinTildes = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(q, Normalizer.Form.NFD)).replaceAll("");
        return NO_ALFANUMERICO.matcher(sinTildes.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}
