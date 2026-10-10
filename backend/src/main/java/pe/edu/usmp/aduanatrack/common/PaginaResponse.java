package pe.edu.usmp.aduanatrack.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Página de resultados con un JSON estable (no se expone la clase interna de Spring Data). */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapeo) {
        return new PaginaResponse<>(pagina.getContent().stream().map(mapeo).toList(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
