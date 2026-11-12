package pe.edu.usmp.aduanatrack.portal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse;

public interface PortalRepository extends Repository<Despacho, Long> {

    /** Despachos no borradores de la empresa, del más reciente al más antiguo. */
    @Query("""
            select new pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse(
                d.id, d.numero, e.numeroBl, e.fechaEta, d.estado, d.canalControl,
                (select max(h.fecha) from HitoDespacho h where h.despachoId = d.id))
            from Despacho d
            left join Embarque e on e.despacho = d
            where d.cliente.id = :clienteId and d.estado <> :borrador
            order by d.id desc
            """)
    List<PortalDespachoResponse> despachosDe(@Param("clienteId") Long clienteId,
            @Param("borrador") EstadoDespacho borrador);
}
