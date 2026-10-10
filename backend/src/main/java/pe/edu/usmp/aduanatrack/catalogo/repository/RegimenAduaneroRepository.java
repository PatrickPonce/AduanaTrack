package pe.edu.usmp.aduanatrack.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.catalogo.entity.RegimenAduanero;

public interface RegimenAduaneroRepository extends JpaRepository<RegimenAduanero, Long> {

    Optional<RegimenAduanero> findByIdAndActivoTrue(Long id);
}
