package pe.edu.usmp.aduanatrack.reporte.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;

public interface ReporteExportRepository extends Repository<Despacho, Long> {

    /**
     * Despachos no borradores abiertos entre {@code desde} (incluido) y {@code hastaExclusivo}, del más
     * antiguo al más reciente (HU-15 · T-15.2).
     */
    @Query("""
            select new pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte(
                d.numero, c.razonSocial, a.nombre, d.estado, d.canalControl, d.creadoEn, d.fechaLevante)
            from Despacho d
            join d.cliente c
            left join d.aduana a
            where d.estado <> :borrador
              and d.creadoEn >= :desde
              and d.creadoEn < :hastaExclusivo
            order by d.creadoEn asc, d.id asc
            """)
    List<FilaReporte> despachosEnRango(@Param("desde") LocalDateTime desde,
            @Param("hastaExclusivo") LocalDateTime hastaExclusivo, @Param("borrador") EstadoDespacho borrador);
}
