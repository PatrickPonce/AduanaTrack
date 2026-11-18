package pe.edu.usmp.aduanatrack.cliente.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByRuc(String ruc);

    /** Clientes activos por razón social; la búsqueda por RUC o nombre (sin tildes) se resuelve en ClienteService. */
    List<Cliente> findByActivoTrueOrderByRazonSocialAsc();
}
