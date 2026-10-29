package pe.edu.usmp.aduanatrack.dam.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.dam.entity.DamHistorial;

public interface DamHistorialRepository extends JpaRepository<DamHistorial, Long> {

    /** Más reciente primero. */
    List<DamHistorial> findByDespachoIdOrderByRectificadoEnDescIdDesc(Long despachoId);
}
