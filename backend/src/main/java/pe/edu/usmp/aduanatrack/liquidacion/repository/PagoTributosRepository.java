package pe.edu.usmp.aduanatrack.liquidacion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.liquidacion.entity.PagoTributos;

public interface PagoTributosRepository extends JpaRepository<PagoTributos, Long> {

    Optional<PagoTributos> findByDespachoId(Long despachoId);
}
