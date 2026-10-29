package pe.edu.usmp.aduanatrack.liquidacion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.liquidacion.entity.Liquidacion;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Long> {

    Optional<Liquidacion> findByDespachoId(Long despachoId);
}
