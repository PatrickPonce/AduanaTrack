package pe.edu.usmp.aduanatrack.documento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.edu.usmp.aduanatrack.documento.entity.TipoDocumento;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    List<TipoDocumento> findByActivoTrueOrderByOrden();

    Optional<TipoDocumento> findByCodigoAndActivoTrue(String codigo);
}
