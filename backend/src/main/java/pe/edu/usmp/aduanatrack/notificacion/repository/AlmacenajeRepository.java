package pe.edu.usmp.aduanatrack.notificacion.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.notificacion.dto.AlmacenajePorVencer;

public interface AlmacenajeRepository extends Repository<Despacho, Long> {

    /**
     * Despachos abiertos sin levante cuya fecha límite de almacenaje es {@code limite} o anterior (incluye
     * los que ya vencieron), del más urgente al menos urgente (HU-14 · T-14.2, CA1).
     */
    @Query("""
            select new pe.edu.usmp.aduanatrack.notificacion.dto.AlmacenajePorVencer(d, e.fechaLimiteAlmacenaje)
            from Embarque e
            join e.despacho d
            where d.estado <> :borrador
              and d.fechaLevante is null
              and e.fechaLimiteAlmacenaje <= :limite
            order by e.fechaLimiteAlmacenaje asc, d.id asc
            """)
    List<AlmacenajePorVencer> sinLevanteConPlazoHasta(@Param("limite") LocalDate limite,
            @Param("borrador") EstadoDespacho borrador);
}
