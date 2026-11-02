package pe.edu.usmp.aduanatrack.cita.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.cita.entity.Deposito;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {

    List<Deposito> findByActivoTrueOrderByCodigoAsc();

    Optional<Deposito> findByIdAndActivoTrue(Long id);
}
