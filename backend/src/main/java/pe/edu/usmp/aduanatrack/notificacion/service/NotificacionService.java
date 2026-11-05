package pe.edu.usmp.aduanatrack.notificacion.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.notificacion.dto.CantidadNoLeidasResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.NotificacionResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.PreferenciasResponse;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Notificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.repository.NotificacionRepository;

/**
 * Notificaciones del usuario (HU-14 · T-14.1, contrato C9). Los demás servicios avisan con
 * {@link #notificar} o {@link #notificarAgente}; cada usuario solo ve y marca las suyas.
 *
 * <p>La notificación siempre queda en la bandeja del sistema, aunque el usuario prefiera el correo: así el
 * historial está completo. El envío por correo se suma cuando exista CorreoService (T-15.3).</p>
 */
@Service
public class NotificacionService {

    static final int TAMANO_PAGINA = 20;
    static final int TAMANO_MAXIMO_PAGINA = 50;
    static final int MAX_TITULO = 150;
    static final int MAX_MENSAJE = 500;
    static final String ESTADO_NO_LEIDAS = "NO_LEIDAS";
    static final String ESTADO_TODAS = "TODAS";

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DespachoRepository despachoRepository;
    private final Clock clock;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository,
            DespachoRepository despachoRepository, Clock clock) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.despachoRepository = despachoRepository;
        this.clock = clock;
    }

    /**
     * Deja una notificación no leída para {@code usuarioId}. {@code despachoId} es opcional. Corre dentro de la
     * transacción de quien llama: si el cambio de estado se revierte, el aviso también. El título y el mensaje
     * se recortan al tamaño de la columna para que un texto largo nunca haga fallar al servicio que avisa.
     */
    @Transactional
    public Notificacion notificar(Long usuarioId, TipoNotificacion tipo, Severidad severidad, String titulo,
            String mensaje, Long despachoId) {
        if (usuarioId == null || tipo == null || severidad == null) {
            throw new IllegalArgumentException("usuarioId, tipo y severidad son obligatorios");
        }
        Despacho despacho = despachoId == null ? null : despachoRepository.getReferenceById(despachoId);
        Notificacion notificacion = new Notificacion(usuarioId, despacho, tipo, severidad,
                recortar(titulo, MAX_TITULO), recortar(mensaje, MAX_MENSAJE), LocalDateTime.now(clock));
        return notificacionRepository.save(notificacion);
    }

    /** Avisa al responsable del despacho, que es su agente ({@code despacho.agente}). */
    @Transactional
    public Notificacion notificarAgente(Despacho despacho, TipoNotificacion tipo, Severidad severidad,
            String titulo, String mensaje) {
        return notificar(despacho.getAgente().getId(), tipo, severidad, titulo, mensaje, despacho.getId());
    }

    /**
     * Bandeja del usuario, de la más reciente a la más antigua. {@code estado}: {@code NO_LEIDAS} o
     * {@code TODAS} (por defecto); otro valor responde 400.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listar(Long usuarioId, String estado, int pagina, int tamano) {
        boolean soloNoLeidas = soloNoLeidas(estado);
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA));
        return PaginaResponse.de(notificacionRepository.bandeja(usuarioId, soloNoLeidas, pageable),
                NotificacionResponse::de);
    }

    @Transactional(readOnly = true)
    public CantidadNoLeidasResponse cantidadNoLeidas(Long usuarioId) {
        return new CantidadNoLeidasResponse(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId));
    }

    /**
     * CA4: marca la notificación como leída. Es idempotente; 404 si no existe o es de otro usuario (no se
     * revela que exista).
     */
    @Transactional
    public NotificacionResponse marcarLeida(Long id, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.buscarDelUsuario(id, usuarioId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe la notificación solicitada."));
        notificacion.marcarLeida(LocalDateTime.now(clock));
        return NotificacionResponse.de(notificacion);
    }

    @Transactional(readOnly = true)
    public PreferenciasResponse preferencias(Long usuarioId) {
        return new PreferenciasResponse(buscarUsuario(usuarioId).getCanalNotificacion());
    }

    @Transactional
    public PreferenciasResponse actualizarPreferencias(Long usuarioId, CanalNotificacion canal) {
        Usuario usuario = buscarUsuario(usuarioId);
        usuario.setCanalNotificacion(canal);
        return new PreferenciasResponse(usuario.getCanalNotificacion());
    }

    private static boolean soloNoLeidas(String estado) {
        String valor = estado == null || estado.isBlank() ? ESTADO_TODAS : estado.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case ESTADO_NO_LEIDAS -> true;
            case ESTADO_TODAS -> false;
            default -> throw ApiException.validacion(Map.of("estado", "Use NO_LEIDAS o TODAS"));
        };
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El título y el mensaje de la notificación son obligatorios");
        }
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 1) + "…";
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario de la sesión."));
    }
}
