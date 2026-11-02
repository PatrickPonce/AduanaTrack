package pe.edu.usmp.aduanatrack.cita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.cita.entity.CitaDespacho;

public interface CitaDespachoRepository extends JpaRepository<CitaDespacho, Long> {

    /** Citas del despacho ordenadas por la fecha y hora confirmadas (contrato C6). */
    @Query("""
            select c from CitaDespacho c
            join fetch c.deposito
            join fetch c.registradoPor
            where c.despachoId = :despachoId
            order by c.fechaHoraConfirmada asc, c.id asc
            """)
    List<CitaDespacho> listar(@Param("despachoId") Long despachoId);
}
