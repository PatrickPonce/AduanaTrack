package pe.edu.usmp.aduanatrack.cliente.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByRuc(String ruc);

    /** Búsqueda por inicio de RUC o por parte de la razón social (autocompletar de HU-03, CA4 de HU-04). */
    @Query("""
            select c from Cliente c
            where c.activo = true
              and (:q = '' or c.ruc like concat(:q, '%') or lower(c.razonSocial) like lower(concat('%', :q, '%')))
            """)
    Page<Cliente> buscar(@Param("q") String q, Pageable pageable);
}
