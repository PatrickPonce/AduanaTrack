package pe.edu.usmp.aduanatrack.cliente.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;

public interface CartaPoderRepository extends JpaRepository<CartaPoder, Long> {

    boolean existsByClienteIdAndFechaVencimientoGreaterThanEqual(Long clienteId, LocalDate fecha);

    /** Vencimiento más lejano entre las cartas del cliente (la que define su estado), o nulo si no tiene. */
    @Query("select max(c.fechaVencimiento) from CartaPoder c where c.clienteId = :clienteId")
    LocalDate maximoVencimiento(@Param("clienteId") Long clienteId);

    /** Igual que {@link #maximoVencimiento} para varios clientes: filas [clienteId, vencimiento]. */
    @Query("select c.clienteId, max(c.fechaVencimiento) from CartaPoder c where c.clienteId in :clienteIds group by c.clienteId")
    List<Object[]> maximoVencimientoPorCliente(@Param("clienteIds") Collection<Long> clienteIds);
}
