package pe.edu.usmp.aduanatrack.cliente.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.cliente.entity.ClienteHistorial;

public interface ClienteHistorialRepository extends JpaRepository<ClienteHistorial, Long> {

    @Query("""
            select h from ClienteHistorial h join fetch h.usuario
            where h.clienteId = :clienteId
            order by h.fecha desc, h.id desc
            """)
    List<ClienteHistorial> historial(@Param("clienteId") Long clienteId);
}
