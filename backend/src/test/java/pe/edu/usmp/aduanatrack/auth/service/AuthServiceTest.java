package pe.edu.usmp.aduanatrack.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auditoria.AuditoriaAccesoService;
import pe.edu.usmp.aduanatrack.auditoria.ResultadoAcceso;
import pe.edu.usmp.aduanatrack.auth.dto.LoginRequest;
import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.SeguridadProperties;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;

class AuthServiceTest {

    private static final String CLAVE = "Clave-Correcta-123";
    private static final String IP = "10.0.0.5";
    private static final String AGENTE = "JUnit";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final RelojMutable reloj = new RelojMutable(Instant.parse("2026-10-06T14:00:00Z"));

    private UsuarioRepository repositorio;
    private AuditoriaAccesoService auditoria;
    private JwtService jwtService;
    private AuthService servicio;
    private Usuario agente;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        auditoria = mock(AuditoriaAccesoService.class);
        jwtService = new JwtService(new SeguridadProperties("secreto-de-pruebas-de-al-menos-32-caracteres", false),
                reloj);
        servicio = new AuthService(repositorio, encoder, jwtService, auditoria, reloj);

        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", encoder.encode(CLAVE),
                new Rol("AGENTE", "Agente de aduana"));
        ReflectionTestUtils.setField(agente, "id", 7L);
        when(repositorio.buscarPorUsuarioOCorreo(anyString())).thenReturn(Optional.empty());
        when(repositorio.buscarPorUsuarioOCorreo("agente")).thenReturn(Optional.of(agente));
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void loginCorrecto_emiteTokenDe2HorasYRegistraExito() {
        SesionIniciada sesion = servicio.login(new LoginRequest("agente", CLAVE, false), IP, AGENTE);

        assertThat(sesion.duracion()).isEqualTo(Duration.ofHours(2));
        assertThat(sesion.usuario().rol()).isEqualTo("AGENTE");
        assertThat(jwtService.validar(sesion.token())).hasValueSatisfying(u -> {
            assertThat(u.id()).isEqualTo(7L);
            assertThat(u.usuario()).isEqualTo("agente");
        });
        verify(auditoria).registrar("agente", 7L, IP, AGENTE, ResultadoAcceso.EXITO);
    }

    @Test
    void loginConRecordarme_emiteSesionDe8Horas() {
        SesionIniciada sesion = servicio.login(new LoginRequest("agente", CLAVE, true), IP, AGENTE);

        assertThat(sesion.duracion()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void contrasenaErronea_responde401GenericoYSumaIntento() {
        assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", "otra", false), IP, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS);
                    assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
                });

        assertThat(agente.getIntentosFallidos()).isEqualTo(1);
        verify(auditoria).registrar("agente", 7L, IP, AGENTE, ResultadoAcceso.FALLIDO);
    }

    @Test
    void usuarioInexistente_respondeElMismoMensajeQueContrasenaErronea() {
        assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie", CLAVE, false), IP, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
                });
        verify(auditoria).registrar(eq("nadie"), isNull(), eq(IP), eq(AGENTE), eq(ResultadoAcceso.FALLIDO));
    }

    @Test
    void tresIntentosFallidos_bloqueanLaCuentaYElCuartoConClaveCorrectaResponde423() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", "mal", false), IP, AGENTE))
                    .isInstanceOf(ApiException.class);
        }

        assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", CLAVE, false), IP, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.CUENTA_BLOQUEADA);
                });

        assertThat(agente.getBloqueadoHasta()).isNotNull();
        InOrder orden = inOrder(auditoria);
        orden.verify(auditoria, org.mockito.Mockito.times(3))
                .registrar("agente", 7L, IP, AGENTE, ResultadoAcceso.FALLIDO);
        orden.verify(auditoria).registrar("agente", 7L, IP, AGENTE, ResultadoAcceso.BLOQUEADO);
    }

    @Test
    void bloqueo_venceA15MinutosYElLoginExitosoReiniciaElContador() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", "mal", false), IP, AGENTE))
                    .isInstanceOf(ApiException.class);
        }

        reloj.avanzar(Duration.ofMinutes(14));
        assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", CLAVE, false), IP, AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.LOCKED));

        reloj.avanzar(Duration.ofMinutes(1));
        servicio.login(new LoginRequest("agente", CLAVE, false), IP, AGENTE);

        assertThat(agente.getIntentosFallidos()).isZero();
        assertThat(agente.getBloqueadoHasta()).isNull();
    }

    @Test
    void exitoEntreFallos_reiniciaElContadorDeIntentos() {
        assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", "mal", false), IP, AGENTE))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", "mal", false), IP, AGENTE))
                .isInstanceOf(ApiException.class);

        servicio.login(new LoginRequest("agente", CLAVE, false), IP, AGENTE);
        assertThatThrownBy(() -> servicio.login(new LoginRequest("agente", "mal", false), IP, AGENTE))
                .isInstanceOf(ApiException.class);

        assertThat(agente.getIntentosFallidos()).isEqualTo(1);
        assertThat(agente.getBloqueadoHasta()).isNull();
    }

    /** Reloj controlable para probar la ventana de bloqueo. */
    static final class RelojMutable extends Clock {
        private Instant ahora;

        RelojMutable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Lima");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
