package pe.edu.usmp.aduanatrack.clasificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;

public interface ItemFacturaRepository extends JpaRepository<ItemFactura, Long> {

    /** Ítems del despacho con su partida, ordenados por posición en la factura. */
    @Query("""
            select i from ItemFactura i
            left join fetch i.partida
            where i.despachoId = :despachoId
            order by i.numero""")
    List<ItemFactura> deDespacho(@Param("despachoId") Long despachoId);
}
