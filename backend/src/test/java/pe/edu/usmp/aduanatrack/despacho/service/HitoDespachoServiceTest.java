package pe.edu.usmp.aduanatrack.despacho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.dto.HitoResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.HitoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.repository.HitoDespachoRepository;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

class HitoDespachoServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 21, 10, 0);

    private HitoDespachoRepository hitoRepository;
    private DespachoRepository despachoRepository;
    private Usuario agente;
    private HitoDespachoService servicio;

    @BeforeEach
    void preparar() {
        hitoRepository = mock(HitoDespachoRepository.class);
        despachoRepository = mock(DespachoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        when(usuarioRepository.getReferenceById(9L)).thenReturn(agente);
        when(hitoRepository.save(any(HitoDespacho.class))).thenAnswer(i -> i.getArgument(0));

        Clock reloj = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new HitoDespachoService(hitoRepository, despachoRepository, usuarioRepository, reloj);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrar_guardaTipoDescripcionUsuarioDeLaSesionYFecha() {
        iniciarSesion();

        HitoDespacho hito = servicio.registrar(5L, TipoHito.CLASIFICADO, "Clasificación arancelaria guardada");

        assertThat(hito.getDespachoId()).isEqualTo(5L);
        assertThat(hito.getTipo()).isEqualTo(TipoHito.CLASIFICADO);
        assertThat(hito.getDescripcion()).isEqualTo("Clasificación arancelaria guardada");
        assertThat(hito.isSugerido()).isFalse();
        assertThat(hito.getUsuario()).isSameAs(agente);
        assertThat(hito.getFecha()).isEqualTo(AHORA);
    }

    @Test
    void registrarSinSesion_quedaComoSistema() {
        HitoDespacho hito = servicio.registrar(5L, TipoHito.APERTURA, "Despacho abierto");

        assertThat(hito.getUsuario()).isNull();
        assertThat(HitoResponse.de(hito).usuario()).isEqualTo(HitoResponse.SISTEMA);
    }

    @Test
    void registrarSugerido_marcaElHitoComoSugeridoYDelSistema() {
        iniciarSesion();

        HitoDespacho hito = servicio.registrarSugerido(5L, "Programar inspección física");

        assertThat(hito.getTipo()).isEqualTo(TipoHito.HITO_SUGERIDO);
        assertThat(hito.isSugerido()).isTrue();
        assertThat(hito.getUsuario()).isNull();
    }

    @Test
    void listar_devuelveLaLineaDeTiempoConElNombreDelUsuario() {
        when(despachoRepository.existsById(5L)).thenReturn(true);
        when(hitoRepository.lineaDeTiempo(5L)).thenReturn(List.of(
                new HitoDespacho(5L, TipoHito.APERTURA, "Despacho abierto", false, agente, AHORA),
                new HitoDespacho(5L, TipoHito.HITO_SUGERIDO, "Programar inspección física", true, null,
                        AHORA.plusHours(1))));

        List<HitoResponse> linea = servicio.listar(5L);

        assertThat(linea).extracting(HitoResponse::tipo).containsExactly(TipoHito.APERTURA, TipoHito.HITO_SUGERIDO);
        assertThat(linea).extracting(HitoResponse::usuario).containsExactly("Agente Demo", "Sistema");
        assertThat(linea.get(1).sugerido()).isTrue();
    }

    @Test
    void listarDespachoInexistente_responde404() {
        when(despachoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.listar(99L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static void iniciarSesion() {
        var principal = new UsuarioAutenticado(9L, "agente", "Agente Demo", "AGENTE");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
