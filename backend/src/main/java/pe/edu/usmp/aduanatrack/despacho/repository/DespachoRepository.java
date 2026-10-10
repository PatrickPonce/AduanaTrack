package pe.edu.usmp.aduanatrack.despacho.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    /** Detalle del despacho con sus relaciones en una sola consulta. */
    @Query("""
            select d from Despacho d
            join fetch d.cliente
            join fetch d.agente
            left join fetch d.regimen
            left join fetch d.aduana
            where d.id = :id
            """)
    Optional<Despacho> buscarDetalle(@Param("id") Long id);
}
