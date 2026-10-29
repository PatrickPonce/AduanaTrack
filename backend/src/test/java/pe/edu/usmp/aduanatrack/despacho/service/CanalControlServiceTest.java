package pe.edu.usmp.aduanatrack.despacho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.dto.CanalResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;

/** Canal de control de HU-10 · T-10.2 (SCRUM-70); los comentarios CAn son los criterios de HU-10. */
class CanalControlServiceTest {

    private static final Long DESPACHO = 5L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 9, 0);

    private HitoDespachoService hitos;
    private Despacho despacho;
    private CanalControlService servicio;

    @BeforeEach
    void preparar() {
        DespachoRepository despachoRepository = mock(DespachoRepository.class);
        hitos = mock(HitoDespachoService.class);
        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        despacho.cambiarEstado(EstadoDespacho.DAM_NUMERADA);
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        servicio = new CanalControlService(despachoRepository, hitos);
    }

    // CA1: el canal naranja se muestra en el despacho

    @Test
    void canalNaranja_quedaGuardadoEnElDespachoYPasaACanalAsignado() {
        CanalResponse respuesta = servicio.guardar(DESPACHO, CanalControl.NARANJA);

        assertThat(despacho.getCanalControl()).isEqualTo("NARANJA");
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CANAL_ASIGNADO);
        assertThat(respuesta.canal()).isEqualTo(CanalControl.NARANJA);
        assertThat(respuesta.estado()).isEqualTo(EstadoDespacho.CANAL_ASIGNADO);
        assertThat(respuesta.levanteHabilitado()).isFalse();
        assertThat(respuesta.hitoSugerido()).isNull();
    }

    // CA2: el canal rojo agrega el hito sugerido de inspección

    @Test
    void canalRojo_agregaElHitoSugeridoDeInspeccionFisica() {
        CanalResponse respuesta = servicio.guardar(DESPACHO, CanalControl.ROJO);

        assertThat(respuesta.hitoSugerido()).isEqualTo("Programar inspección física");
        assertThat(respuesta.levanteHabilitado()).isFalse();
        verify(hitos).registrarSugerido(DESPACHO, "Programar inspección física");
    }

    // CA3: el canal verde habilita el levante

    @Test
    void canalVerde_habilitaElLevanteYNoSugiereNada() {
        CanalResponse respuesta = servicio.guardar(DESPACHO, CanalControl.VERDE);

        assertThat(respuesta.levanteHabilitado()).isTrue();
        assertThat(respuesta.hitoSugerido()).isNull();
        verify(hitos, never()).registrarSugerido(any(), anyString());
    }

    // CA4: cada cambio de canal o de estado agrega un hito

    @Test
    void laPrimeraAsignacionRegistraElHitoCanalAsignado() {
        servicio.guardar(DESPACHO, CanalControl.NARANJA);

        verify(hitos).registrar(DESPACHO, TipoHito.CANAL_ASIGNADO, "Canal de control asignado: NARANJA");
    }

    @Test
    void cadaCambioPosteriorDeCanalRegistraOtroHito() {
        servicio.guardar(DESPACHO, CanalControl.NARANJA);
        servicio.guardar(DESPACHO, CanalControl.ROJO);

        verify(hitos).registrar(DESPACHO, TipoHito.CANAL_ASIGNADO, "Canal de control cambiado de NARANJA a ROJO");
        verify(hitos, times(2)).registrar(any(), any(), anyString());
        assertThat(despacho.getCanalControl()).isEqualTo("ROJO");
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CANAL_ASIGNADO);
    }

    @Test
    void guardarElMismoCanalOtraVez_noRepiteElHito() {
        servicio.guardar(DESPACHO, CanalControl.NARANJA);

        servicio.guardar(DESPACHO, CanalControl.NARANJA);

        verify(hitos, times(1)).registrar(any(), any(), anyString());
    }

    @Test
    void guardarRojoOtraVez_noDuplicaElHitoSugeridoPeroLoSigueInformando() {
        servicio.guardar(DESPACHO, CanalControl.ROJO);

        CanalResponse repetida = servicio.guardar(DESPACHO, CanalControl.ROJO);

        verify(hitos, times(1)).registrarSugerido(any(), anyString());
        assertThat(repetida.hitoSugerido()).isEqualTo("Programar inspección física");
    }

    // Reglas de estado

    @Test
    void sinDamNumerada_responde409DamNoNumerada() {
        despacho.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, CanalControl.VERDE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DAM_NO_NUMERADA);
                });
        assertThat(despacho.getCanalControl()).isNull();
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.TRIBUTOS_PAGADOS);
        verify(hitos, never()).registrar(any(), any(), anyString());
    }

    @Test
    void despachoEnApertura_responde409() {
        despacho.cambiarEstado(EstadoDespacho.APERTURA);

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, CanalControl.VERDE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DAM_NO_NUMERADA));
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.guardar(99L, CanalControl.VERDE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
