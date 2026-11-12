package pe.edu.usmp.aduanatrack.inspeccion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.inspeccion.entity.InspeccionDespacho;

public interface InspeccionDespachoRepository extends JpaRepository<InspeccionDespacho, Long> {

    /** La inspección más reciente del despacho (contrato C7), con quien la registró. */
    @Query("""
            select i from InspeccionDespacho i
            join fetch i.registradaPor
            where i.despachoId = :despachoId
            order by i.registradaEn desc, i.id desc
            limit 1
            """)
    Optional<InspeccionDespacho> ultima(@Param("despachoId") Long despachoId);
}
