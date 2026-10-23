package pe.edu.usmp.aduanatrack.despacho.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.despacho.entity.HitoDespacho;

public interface HitoDespachoRepository extends JpaRepository<HitoDespacho, Long> {

    /** Línea de tiempo en orden cronológico; el id desempata los hitos del mismo instante. */
    @Query("""
            select h from HitoDespacho h
            left join fetch h.usuario
            where h.despachoId = :despachoId
            order by h.fecha, h.id
            """)
    List<HitoDespacho> lineaDeTiempo(@Param("despachoId") Long despachoId);
}
