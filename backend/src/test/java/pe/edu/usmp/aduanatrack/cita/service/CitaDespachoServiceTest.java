package pe.edu.usmp.aduanatrack.cita.service;

import pe.edu.usmp.aduanatrack.notificacion.service.AvisoClienteService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.http.HttpStatus;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cita.dto.CitaRequest;
import pe.edu.usmp.aduanatrack.cita.dto.CitaResponse;
import pe.edu.usmp.aduanatrack.cita.dto.TransportistaRequest;
import pe.edu.usmp.aduanatrack.cita.entity.CitaDespacho;
import pe.edu.usmp.aduanatrack.cita.entity.Deposito;
import pe.edu.usmp.aduanatrack.cita.entity.EstadoCita;
import pe.edu.usmp.aduanatrack.cita.entity.TipoCita;
import pe.edu.usmp.aduanatrack.cita.entity.TipoDeposito;
import pe.edu.usmp.aduanatrack.cita.repository.CitaDespachoRepository;
import pe.edu.usmp.aduanatrack.cita.repository.DepositoRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;

/** Citas en depósito de HU-11 · T-11.1 y T-11.2 (SCRUM-78 y SCRUM-79); los comentarios CAn son los criterios de HU-11. */
class CitaDespachoServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long USUARIO = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 10, 0);
    private static final LocalDateTime FECHA_CITA = LocalDateTime.of(2026, 11, 12, 9, 30);

    private DespachoRepository despachoRepository;
    private DepositoRepository depositoRepository;
    private CitaDespachoRepository citaRepository;
    private HitoDespachoService hitos;
    private Deposito deposito;
    private Despacho despacho;
    private AvisoClienteService avisosCliente;
    private CitaDespachoService servicio;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        depositoRepository = mock(DepositoRepository.class);
        citaRepository = mock(CitaDespachoRepository.class);
        hitos = mock(HitoDespachoService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        Usuario agente = mock(Usuario.class);
        when(agente.getNombre()).thenReturn("Agente Demo");
        when(usuarioRepository.getReferenceById(USUARIO)).thenReturn(agente);

        deposito = mock(Deposito.class);
        when(deposito.getId()).thenReturn(1L);
        when(deposito.getNombre()).thenReturn("Depósito temporal 1 (ejemplo)");
        when(deposito.getTipo()).thenReturn(TipoDeposito.DEPOSITO_TEMPORAL);
        when(depositoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(deposito));

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        despacho.asignarCanal(CanalControl.ROJO);
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));

        avisosCliente = mock(AvisoClienteService.class);
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new CitaDespachoService(despachoRepository, depositoRepository, citaRepository, usuarioRepository,
                hitos, avisosCliente, reloj);
    }

    private static CitaRequest inspeccion(LocalDateTime fecha) {
        return new CitaRequest(1L, "TK-2026-004512", TipoCita.INSPECCION, fecha, null);
    }

    private static TransportistaRequest transportista() {
        return new TransportistaRequest("20123456786", "Transportes Demo SAC", "abc-123", "Juan Pérez");
    }

    // CA1: una cita con depósito y número queda asociada al despacho

    @Test
    void citaConDepositoYNumero_quedaAsociadaAlDespachoYDejaElHito() {
        CitaResponse respuesta = servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO);

        ArgumentCaptor<CitaDespacho> guardada = ArgumentCaptor.forClass(CitaDespacho.class);
        verify(citaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getDespachoId()).isEqualTo(DESPACHO);
        assertThat(guardada.getValue().getDeposito()).isSameAs(deposito);
        assertThat(guardada.getValue().getNumeroCita()).isEqualTo("TK-2026-004512");
        assertThat(respuesta.depositoId()).isEqualTo(1L);
        assertThat(respuesta.numeroCita()).isEqualTo("TK-2026-004512");
        verify(hitos).registrar(DESPACHO, TipoHito.CITA_REGISTRADA,
                "Cita de inspección registrada: TK-2026-004512 el 12/11/2026 09:30 en Depósito temporal 1 (ejemplo)");
    }

    // CA2: una cita de inspección queda «Programada» y el despacho pasa a INSPECCION_PROGRAMADA

    @Test
    void citaDeInspeccion_quedaProgramadaYElDespachoPasaAInspeccionProgramada() {
        CitaResponse respuesta = servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO);

        assertThat(respuesta.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(respuesta.transportista()).isNull();
        assertThat(respuesta.estadoDespacho()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
        verify(hitos).registrar(eq(DESPACHO), eq(TipoHito.INSPECCION_PROGRAMADA), anyString());
    }

    @Test
    void canalNaranja_tambienPermiteLaCitaDeInspeccion() {
        despacho.asignarCanal(CanalControl.NARANJA);

        servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
    }

    @Test
    void conObservaciones_sePuedeRegistrarUnaNuevaCitaSinRepetirElHitoDeEstado() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_PROGRAMADA);

        servicio.registrar(DESPACHO, inspeccion(FECHA_CITA.plusDays(3)), USUARIO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
        verify(hitos).registrar(any(), eq(TipoHito.CITA_REGISTRADA), anyString());
        verify(hitos, never()).registrar(any(), eq(TipoHito.INSPECCION_PROGRAMADA),
                anyString());
    }

    @Test
    void canalVerde_responde409PorqueNoRequiereInspeccion() {
        despacho.asignarCanal(CanalControl.VERDE);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.CITA_INSPECCION_NO_APLICA);
                });
        verify(citaRepository, never()).save(any());
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CANAL_ASIGNADO);
    }

    @Test
    void sinCanalAsignado_laCitaDeInspeccionResponde409() {
        despacho.cambiarEstado(EstadoDespacho.DAM_NUMERADA);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CITA_INSPECCION_NO_APLICA));
    }

    @Test
    void conInspeccionYaConforme_laCitaDeInspeccionResponde409() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_CONFORME);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CITA_INSPECCION_NO_APLICA));
    }

    // CA3: una cita de retiro guarda los datos del transportista con la fecha y hora confirmadas

    @Test
    void citaDeRetiro_guardaElTransportistaYPasaARetiroProgramado() {
        despacho.cambiarEstado(EstadoDespacho.LEVANTE_OTORGADO);
        CitaRequest retiro = new CitaRequest(1L, "TK-2026-004999", TipoCita.RETIRO, FECHA_CITA, transportista());

        CitaResponse respuesta = servicio.registrar(DESPACHO, retiro, USUARIO);

        assertThat(respuesta.tipo()).isEqualTo(TipoCita.RETIRO);
        assertThat(respuesta.fechaHoraConfirmada()).isEqualTo(FECHA_CITA);
        assertThat(respuesta.transportista().ruc()).isEqualTo("20123456786");
        assertThat(respuesta.transportista().razonSocial()).isEqualTo("Transportes Demo SAC");
        assertThat(respuesta.transportista().placa()).isEqualTo("ABC-123");
        assertThat(respuesta.transportista().conductor()).isEqualTo("Juan Pérez");
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.RETIRO_PROGRAMADO);
        verify(hitos).registrar(any(), eq(TipoHito.RETIRO_PROGRAMADO), anyString());
        verify(avisosCliente).retiroProgramado(eq(despacho), anyString());
    }

    @Test
    void citaDeInspeccion_noAvisaAlClientePorRetiro() {
        servicio.registrar(DESPACHO, inspeccion(FECHA_CITA), USUARIO);

        verify(avisosCliente, never()).retiroProgramado(any(), anyString());
    }

    @Test
    void citaDeRetiroAntesDelLevante_responde409() {
        CitaRequest retiro = new CitaRequest(1L, "TK-2026-004999", TipoCita.RETIRO, FECHA_CITA, transportista());

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, retiro, USUARIO))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.CITA_RETIRO_SIN_LEVANTE);
                });
        verify(citaRepository, never()).save(any());
    }

    @Test
    void citaDeRetiroSinTransportista_responde400ConLosCamposMarcados() {
        despacho.cambiarEstado(EstadoDespacho.LEVANTE_OTORGADO);
        CitaRequest retiro = new CitaRequest(1L, "TK-2026-004999", TipoCita.RETIRO, FECHA_CITA, null);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, retiro, USUARIO))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsKeys("transportista.ruc", "transportista.razonSocial",
                            "transportista.placa", "transportista.conductor");
                });
        verify(citaRepository, never()).save(any());
    }

    @Test
    void citaDeRetiroConRucInvalido_marcaElCampo() {
        despacho.cambiarEstado(EstadoDespacho.LEVANTE_OTORGADO);
        TransportistaRequest malo = new TransportistaRequest("20123456789", "Transportes Demo SAC", "ABC-123", "Juan");
        CitaRequest retiro = new CitaRequest(1L, "TK-2026-004999", TipoCita.RETIRO, FECHA_CITA, malo);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, retiro, USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsOnlyKeys("transportista.ruc"));
    }

    @Test
    void segundaCitaDeRetiro_noRepiteElHitoDeEstado() {
        despacho.cambiarEstado(EstadoDespacho.RETIRO_PROGRAMADO);
        CitaRequest retiro = new CitaRequest(1L, "TK-2026-005000", TipoCita.RETIRO, FECHA_CITA, transportista());

        servicio.registrar(DESPACHO, retiro, USUARIO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.RETIRO_PROGRAMADO);
        verify(hitos, never()).registrar(any(), eq(TipoHito.RETIRO_PROGRAMADO),
                anyString());
    }

    // CA4: sin fecha y hora confirmada responde 400 y no guarda (el controlador valida @NotNull; el servicio
    // protege además el depósito y el número)

    @Test
    void depositoInexistente_responde400ConElCampo() {
        CitaRequest conDepositoMalo = new CitaRequest(99L, "TK-1", TipoCita.INSPECCION, FECHA_CITA, null);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, conDepositoMalo, USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("depositoId"));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void numeroDeCitaEnBlanco_responde400() {
        CitaRequest sinNumero = new CitaRequest(1L, "   ", TipoCita.INSPECCION, FECHA_CITA, null);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, sinNumero, USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("numeroCita"));
    }

    // Reglas generales

    @Test
    void despachoEnBorrador_responde409() {
        Despacho borrador = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        when(despachoRepository.findById(7L)).thenReturn(Optional.of(borrador));

        assertThatThrownBy(() -> servicio.registrar(7L, inspeccion(FECHA_CITA), USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_EN_BORRADOR));
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.registrar(99L, inspeccion(FECHA_CITA), USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.listar(99L)).isInstanceOf(ApiException.class);
    }

    @Test
    void listar_devuelveLasCitasConElEstadoDelDespacho() {
        Usuario agente = mock(Usuario.class);
        when(agente.getNombre()).thenReturn("Agente Demo");
        CitaDespacho cita = new CitaDespacho(DESPACHO, deposito, "TK-1", TipoCita.INSPECCION, FECHA_CITA, agente,
                AHORA);
        when(citaRepository.listar(DESPACHO)).thenReturn(List.of(cita));

        List<CitaResponse> citas = servicio.listar(DESPACHO);

        assertThat(citas).hasSize(1);
        assertThat(citas.get(0).estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(citas.get(0).estadoDespacho()).isEqualTo(EstadoDespacho.CANAL_ASIGNADO);
        assertThat(citas.get(0).registradaPor()).isEqualTo("Agente Demo");
    }
}
