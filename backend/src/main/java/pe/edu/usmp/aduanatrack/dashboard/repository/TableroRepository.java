package pe.edu.usmp.aduanatrack.dashboard.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroFila;
import pe.edu.usmp.aduanatrack.dashboard.dto.IndicadoresResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

public interface TableroRepository extends Repository<Despacho, Long> {

    /**
     * Despachos no borradores con su BL y ETA, ordenados por ETA ascendente y sin ETA al final
     * (HU-02, CA2). Los filtros son opcionales y combinables (CA4).
     */
    @Query("""
            select new pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroFila(
                d.id, d.numero, c.id, c.razonSocial, a.codigo, a.nombre,
                e.numeroBl, e.fechaEta, e.fechaLimiteAlmacenaje, d.estado)
            from Despacho d
            join d.cliente c
            left join d.aduana a
            left join Embarque e on e.despacho = d
            where d.estado <> :borrador
              and (:aduanaId is null or a.id = :aduanaId)
              and (:clienteId is null or c.id = :clienteId)
            order by e.fechaEta asc nulls last, d.id asc
            """)
    List<DespachoTableroFila> despachos(@Param("aduanaId") Long aduanaId, @Param("clienteId") Long clienteId,
            @Param("borrador") EstadoDespacho borrador);

    /**
     * Indicadores del Dashboard contados en la base de datos, sobre las mismas filas y filtros
     * que {@link #despachos}. Alerta crítica: fecha límite de almacenaje hasta
     * {@code limiteAlmacenaje}, o carta poder del cliente (la de vencimiento más lejano) que
     * vence hasta {@code vencimientoCritico}; un cliente sin carta no cuenta.
     */
    @Query("""
            select new pe.edu.usmp.aduanatrack.dashboard.dto.IndicadoresResponse(
                count(case when d.fechaLevante is null then 1 end),
                count(case when upper(d.canalControl) in ('NARANJA', 'ROJO') then 1 end),
                count(case when d.fechaLevante is not null then 1 end),
                count(case when e.fechaLimiteAlmacenaje <= :limiteAlmacenaje
                             or (select max(cp.fechaVencimiento) from CartaPoder cp
                                 where cp.clienteId = c.id) <= :vencimientoCritico
                           then 1 end))
            from Despacho d
            join d.cliente c
            left join d.aduana a
            left join Embarque e on e.despacho = d
            where d.estado <> :borrador
              and (:aduanaId is null or a.id = :aduanaId)
              and (:clienteId is null or c.id = :clienteId)
            """)
    IndicadoresResponse indicadores(@Param("aduanaId") Long aduanaId, @Param("clienteId") Long clienteId,
            @Param("borrador") EstadoDespacho borrador, @Param("limiteAlmacenaje") LocalDate limiteAlmacenaje,
            @Param("vencimientoCritico") LocalDate vencimientoCritico);
}
