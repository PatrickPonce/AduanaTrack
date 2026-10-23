package pe.edu.usmp.aduanatrack.despacho.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.dto.HitoResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.HitoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.repository.HitoDespachoRepository;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/**
 * Línea de tiempo del despacho (HU-10 · T-10.1). Todo cambio de estado de HU-06 a HU-10 llama a
 * {@link #registrar} dentro de su propia transacción, así el estado y el hito se guardan juntos.
 */
@Service
public class HitoDespachoService {

    private final HitoDespachoRepository hitoRepository;
    private final DespachoRepository despachoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public HitoDespachoService(HitoDespachoRepository hitoRepository, DespachoRepository despachoRepository,
            UsuarioRepository usuarioRepository, Clock clock) {
        this.hitoRepository = hitoRepository;
        this.despachoRepository = despachoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    /** Registra el hito con el usuario de la sesión (o «Sistema» si no hay sesión) y la hora actual. */
    @Transactional
    public HitoDespacho registrar(Long despachoId, TipoHito tipo, String descripcion) {
        return hitoRepository.save(new HitoDespacho(despachoId, tipo, descripcion, false, usuarioActual(),
                LocalDateTime.now(clock)));
    }

    /** Acción que el sistema propone al agente (p. ej. «Programar inspección física» en canal rojo). */
    @Transactional
    public HitoDespacho registrarSugerido(Long despachoId, String descripcion) {
        return hitoRepository.save(new HitoDespacho(despachoId, TipoHito.HITO_SUGERIDO, descripcion, true, null,
                LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<HitoResponse> listar(Long despachoId) {
        if (!despachoRepository.existsById(despachoId)) {
            throw ApiException.noEncontrado("No existe el despacho solicitado.");
        }
        return hitoRepository.lineaDeTiempo(despachoId).stream().map(HitoResponse::de).toList();
    }

    private Usuario usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuarioRepository.getReferenceById(usuario.id());
        }
        return null;
    }
}
