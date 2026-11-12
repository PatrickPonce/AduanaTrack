package pe.edu.usmp.aduanatrack.portal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse;

public interface PortalRepository extends Repository<Despacho, Long> {

    /**
     * Fila del portal: BL y ETA del embarque (nulos si no tiene) y fecha del último hito. La comparten
     * la lista y el detalle para que ambos devuelvan exactamente lo mismo.
     */
    String FILA_PORTAL = """
            select new pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse(
                d.id, d.numero, e.numeroBl, e.fechaEta, d.estado, d.canalControl,
                (select max(h.fecha) from HitoDespacho h where h.despachoId = d.id))
            from Despacho d
            left join Embarque e on e.despacho = d
            where d.cliente.id = :clienteId and d.estado <> :borrador
            """;

    /** Despachos no borradores de la empresa, del más reciente al más antiguo. */
    @Query(FILA_PORTAL + " order by d.id desc")
    List<PortalDespachoResponse> despachosDe(@Param("clienteId") Long clienteId,
            @Param("borrador") EstadoDespacho borrador);

    /** Un despacho no borrador de la empresa, o vacío si no cumple. */
    @Query(FILA_PORTAL + " and d.id = :id")
    Optional<PortalDespachoResponse> despachoDe(@Param("clienteId") Long clienteId, @Param("id") Long id,
            @Param("borrador") EstadoDespacho borrador);
}
