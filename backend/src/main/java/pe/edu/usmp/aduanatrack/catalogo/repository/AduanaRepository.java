package pe.edu.usmp.aduanatrack.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.catalogo.entity.Aduana;

public interface AduanaRepository extends JpaRepository<Aduana, Long> {

    Optional<Aduana> findByIdAndActivoTrue(Long id);
}
