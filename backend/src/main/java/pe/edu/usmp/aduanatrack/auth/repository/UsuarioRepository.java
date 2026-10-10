package pe.edu.usmp.aduanatrack.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** El login acepta el nombre de usuario o el correo corporativo. */
    @Query("select u from Usuario u where lower(u.usuario) = lower(:valor) or lower(u.correo) = lower(:valor)")
    Optional<Usuario> buscarPorUsuarioOCorreo(@Param("valor") String valor);
}
