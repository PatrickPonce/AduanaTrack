package pe.edu.usmp.aduanatrack.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.notificacion.dto.NotificacionResponse;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Notificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.repository.NotificacionRepository;

/** Notificaciones de HU-14 · T-14.1 (SCRUM-90); los comentarios CAn son los criterios de HU-14. */
class NotificacionServiceTest {

    private static final Long USUARIO = 9L;
    private static final Long OTRO_USUARIO = 10L;
    private static final Long DESPACHO = 5L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 6, 10, 0);

    private NotificacionRepository notificacionRepository;
    private UsuarioRepository usuarioRepository;
    private DespachoRepository despachoRepository;
    private Despacho despacho;
    private NotificacionService servicio;

    @BeforeEach
    void preparar() {
        notificacionRepository = mock(NotificacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        despachoRepository = mock(DespachoRepository.class);
        when(notificacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        despacho = mock(Despacho.class);
        when(despacho.getId()).thenReturn(DESPACHO);
        when(despacho.getNumero()).thenReturn("DESP-2026-0001");
        when(despachoRepository.getReferenceById(DESPACHO)).thenReturn(despacho);

        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new NotificacionService(notificacionRepository, usuarioRepository, despachoRepository, reloj);
    }

    private Notificacion notificacionDe(Long usuarioId) {
        return new Notificacion(usuarioId, despacho, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
                "Inspección con observaciones", "DESP-2026-0001: rotulado incompleto", AHORA.minusHours(1));
    }

    @Test
    void notificar_guardaNoLeidaParaElUsuarioConElDespachoYLaFecha() {
        servicio.notificar(USUARIO, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
                "Inspección con observaciones", "DESP-2026-0001: rotulado incompleto", DESPACHO);

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        Notificacion n = guardada.getValue();
        assertThat(n.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(n.getDespacho()).isSameAs(despacho);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.INSPECCION_CON_OBSERVACIONES);
        assertThat(n.getSeveridad()).isEqualTo(Severidad.ALTA);
        assertThat(n.isLeida()).isFalse();
        assertThat(n.getCreadaEn()).isEqualTo(AHORA);
        assertThat(n.getLeidaEn()).isNull();
    }

    @Test
    void notificarSinDespacho_noBuscaElDespacho() {
        Notificacion n = servicio.notificar(USUARIO, TipoNotificacion.CARTA_PODER_POR_VENCER, Severidad.INFO,
                "Carta poder por vencer", "La carta poder vence el 20/11/2026", null);

        assertThat(n.getDespacho()).isNull();
        verify(despachoRepository, never()).getReferenceById(any());
        assertThat(NotificacionResponse.de(n).despachoId()).isNull();
    }

    @Test
    void notificarAgente_avisaAlResponsableDelDespacho() {
        Usuario agente = mock(Usuario.class);
        when(agente.getId()).thenReturn(USUARIO);
        when(despacho.getAgente()).thenReturn(agente);

        Notificacion n = servicio.notificarAgente(despacho, TipoNotificacion.INSPECCION_CON_OBSERVACIONES,
                Severidad.ALTA, "Inspección con observaciones", "Rotulado incompleto");

        assertThat(n.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(n.getDespacho()).isSameAs(despacho);
    }

    @Test
    void notificarConMensajeLargo_loRecortaAlTamanoDeLaColumna() {
        Notificacion n = servicio.notificar(USUARIO, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO,
                "  Título  ", "x".repeat(800), null);

        assertThat(n.getTitulo()).isEqualTo("Título");
        assertThat(n.getMensaje()).hasSize(NotificacionService.MAX_MENSAJE).endsWith("…");
    }

    @Test
    void notificarSinMensaje_esUnErrorDeProgramacion() {
        assertThatThrownBy(() -> servicio.notificar(USUARIO, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO,
                "Título", "  ", null)).isInstanceOf(IllegalArgumentException.class);
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void listarNoLeidas_filtraPorElUsuarioYLasNoLeidas() {
        when(notificacionRepository.bandeja(eq(USUARIO), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(notificacionDe(USUARIO)), PageRequest.of(0, 20), 1));

        PaginaResponse<NotificacionResponse> pagina = servicio.listar(USUARIO, "no_leidas", 0, 20);

        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).numeroDespacho()).isEqualTo("DESP-2026-0001");
        assertThat(pagina.contenido().get(0).severidad()).isEqualTo(Severidad.ALTA);
    }

    @Test
    void listarSinEstado_devuelveTodasYLimitaElTamanoDePagina() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(notificacionRepository.bandeja(eq(USUARIO), eq(false), pageable.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.listar(USUARIO, null, -3, 500);

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(NotificacionService.TAMANO_MAXIMO_PAGINA);
    }

    @Test
    void listarConEstadoDesconocido_responde400() {
        assertThatThrownBy(() -> servicio.listar(USUARIO, "ARCHIVADAS", 0, 20))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ex = (ApiException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCampos()).containsKey("estado");
                });
        verify(notificacionRepository, never()).bandeja(anyLong(), anyBoolean(), any());
    }

    @Test
    void cantidadNoLeidas_cuentaSoloLasDelUsuario() {
        when(notificacionRepository.countByUsuarioIdAndLeidaFalse(USUARIO)).thenReturn(3L);

        assertThat(servicio.cantidadNoLeidas(USUARIO).cantidad()).isEqualTo(3);
    }

    // CA4: marcar como leída

    @Test
    void marcarLeida_registraLaFechaDeLectura() {
        Notificacion n = notificacionDe(USUARIO);
        when(notificacionRepository.buscarDelUsuario(1L, USUARIO)).thenReturn(Optional.of(n));

        NotificacionResponse respuesta = servicio.marcarLeida(1L, USUARIO);

        assertThat(respuesta.leida()).isTrue();
        assertThat(respuesta.leidaEn()).isEqualTo(AHORA);
    }

    @Test
    void marcarLeidaDosVeces_conservaLaPrimeraFecha() {
        Notificacion n = notificacionDe(USUARIO);
        n.marcarLeida(AHORA.minusMinutes(30));
        when(notificacionRepository.buscarDelUsuario(1L, USUARIO)).thenReturn(Optional.of(n));

        assertThat(servicio.marcarLeida(1L, USUARIO).leidaEn()).isEqualTo(AHORA.minusMinutes(30));
    }

    @Test
    void marcarLeidaDeOtroUsuario_responde404() {
        when(notificacionRepository.buscarDelUsuario(1L, OTRO_USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.marcarLeida(1L, OTRO_USUARIO))
                .isInstanceOf(ApiException.class)
                .extracting("codigo").isEqualTo(CodigoError.NO_ENCONTRADO);
    }

    @Test
    void actualizarPreferencias_guardaElCanalDelUsuario() {
        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(usuario.getCanalNotificacion()).thenReturn(CanalNotificacion.AMBOS);

        assertThat(servicio.actualizarPreferencias(USUARIO, CanalNotificacion.AMBOS).canalNotificacion())
                .isEqualTo(CanalNotificacion.AMBOS);
        verify(usuario).setCanalNotificacion(CanalNotificacion.AMBOS);
    }

    @Test
    void canal_soloSistemaNoIncluyeCorreo() {
        assertThat(CanalNotificacion.SISTEMA.incluyeCorreo()).isFalse();
        assertThat(CanalNotificacion.CORREO.incluyeCorreo()).isTrue();
        assertThat(CanalNotificacion.AMBOS.incluyeCorreo()).isTrue();
    }
}
