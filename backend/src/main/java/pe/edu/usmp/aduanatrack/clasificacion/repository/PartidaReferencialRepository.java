package pe.edu.usmp.aduanatrack.clasificacion.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;

public interface PartidaReferencialRepository extends JpaRepository<PartidaReferencial, Long> {

    /**
     * Partidas activas cuya descripción normalizada coincide con {@code patron} (con {@code %}) o
     * cuyo código coincide con {@code codigo}, ordenadas por código.
     */
    @Query("""
            select p from PartidaReferencial p
            where p.activo = true and (p.busqueda like :patron or p.codigo like :codigo)
            order by p.codigo""")
    List<PartidaReferencial> buscar(@Param("patron") String patron, @Param("codigo") String codigo,
            Pageable limite);
}
