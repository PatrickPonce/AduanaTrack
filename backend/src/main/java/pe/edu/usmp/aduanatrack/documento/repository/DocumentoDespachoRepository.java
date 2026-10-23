package pe.edu.usmp.aduanatrack.documento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.documento.entity.DocumentoDespacho;

public interface DocumentoDespachoRepository extends JpaRepository<DocumentoDespacho, Long> {

    /** Versión vigente de cada tipo cargado en el despacho, con su tipo y usuario. */
    @Query("""
            select d from DocumentoDespacho d
            join fetch d.tipo
            join fetch d.usuario
            where d.despachoId = :despachoId and d.vigente = true
            """)
    List<DocumentoDespacho> vigentes(@Param("despachoId") Long despachoId);

    Optional<DocumentoDespacho> findByDespachoIdAndTipoIdAndVigenteTrue(Long despachoId, Long tipoId);

    @Query("""
            select coalesce(max(d.version), 0) from DocumentoDespacho d
            where d.despachoId = :despachoId and d.tipo.id = :tipoId
            """)
    int ultimaVersion(@Param("despachoId") Long despachoId, @Param("tipoId") Long tipoId);

    /** Historial de versiones de un tipo, de la más reciente a la más antigua (CA3). */
    @Query("""
            select d from DocumentoDespacho d
            join fetch d.usuario
            where d.despachoId = :despachoId and d.tipo.id = :tipoId
            order by d.version desc
            """)
    List<DocumentoDespacho> versiones(@Param("despachoId") Long despachoId, @Param("tipoId") Long tipoId);

    Optional<DocumentoDespacho> findByIdAndDespachoId(Long id, Long despachoId);
}
