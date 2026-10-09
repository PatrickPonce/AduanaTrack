package pe.edu.usmp.aduanatrack.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auditoria.AuditoriaAccesoService;
import pe.edu.usmp.aduanatrack.auditoria.ResultadoAcceso;
import pe.edu.usmp.aduanatrack.auth.dto.LoginRequest;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Inicio de sesión con bloqueo tras 3 intentos fallidos consecutivos (HU-01 · T-01.1). */
@Service
public class AuthService {

    static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos";
    static final String MENSAJE_BLOQUEADA = "Cuenta bloqueada temporalmente. Intente nuevamente en 15 minutos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaAccesoService auditoria;
    private final Clock clock;
    /**
     * Hash BCrypt de referencia: si el usuario no existe se compara igual contra él, para que
     * el tiempo de respuesta no revele qué usuarios existen (CA2).
     */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuditoriaAccesoService auditoria, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
        this.clock = clock;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Las excepciones de negocio no deshacen la transacción: el contador de intentos y el
     * bloqueo deben quedar guardados aunque el login responda 401 o 423.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public SesionIniciada login(LoginRequest solicitud, String ip, String userAgent) {
        String ingresado = solicitud.usuario().trim();
        LocalDateTime ahora = LocalDateTime.now(clock);

        Usuario usuario = usuarioRepository.buscarPorUsuarioOCorreo(ingresado).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            passwordEncoder.matches(solicitud.password(), hashFicticio);
            auditoria.registrar(ingresado, usuario == null ? null : usuario.getId(), ip, userAgent,
                    ResultadoAcceso.FALLIDO);
            throw credencialesInvalidas();
        }

        // CA3: bloqueada responde 423 aunque la contraseña sea correcta.
        if (usuario.estaBloqueado(ahora)) {
            auditoria.registrar(ingresado, usuario.getId(), ip, userAgent, ResultadoAcceso.BLOQUEADO);
            throw new ApiException(HttpStatus.LOCKED, CodigoError.CUENTA_BLOQUEADA, MENSAJE_BLOQUEADA);
        }

        if (!passwordEncoder.matches(solicitud.password(), usuario.getPasswordHash())) {
            usuario.registrarIntentoFallido(ahora);
            usuarioRepository.save(usuario);
            auditoria.registrar(ingresado, usuario.getId(), ip, userAgent, ResultadoAcceso.FALLIDO);
            throw credencialesInvalidas();
        }

        usuario.registrarAccesoExitoso();
        usuarioRepository.save(usuario);
        auditoria.registrar(ingresado, usuario.getId(), ip, userAgent, ResultadoAcceso.EXITO);

        UsuarioAutenticado autenticado = new UsuarioAutenticado(
                usuario.getId(), usuario.getUsuario(), usuario.getNombre(), usuario.getRol().getCodigo());
        Duration duracion = SesionCookieService.duracion(solicitud.recordarme());
        return new SesionIniciada(autenticado, jwtService.generar(autenticado, duracion), duracion);
    }

    private static ApiException credencialesInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, CodigoError.CREDENCIALES_INVALIDAS, MENSAJE_CREDENCIALES);
    }
}
