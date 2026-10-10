package pe.edu.usmp.aduanatrack.embarque.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.embarque.entity.Embarque;

public interface EmbarqueRepository extends JpaRepository<Embarque, Long> {

    @Query("select e from Embarque e left join fetch e.contenedores where e.despacho.id = :despachoId")
    Optional<Embarque> buscarPorDespacho(@Param("despachoId") Long despachoId);
}
