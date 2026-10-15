package pe.edu.usmp.aduanatrack.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.catalogo.entity.Aduana;

public interface AduanaRepository extends JpaRepository<Aduana, Long> {

    List<Aduana> findByActivoTrueOrderByCodigoAsc();

    Optional<Aduana> findByIdAndActivoTrue(Long id);
}
