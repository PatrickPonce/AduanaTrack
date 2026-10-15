package pe.edu.usmp.aduanatrack.embarque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.embarque.dto.ContenedorRequest;
import pe.edu.usmp.aduanatrack.embarque.dto.EmbarqueRequest;
import pe.edu.usmp.aduanatrack.embarque.dto.EmbarqueResponse;
import pe.edu.usmp.aduanatrack.embarque.entity.Embarque;
import pe.edu.usmp.aduanatrack.embarque.entity.TipoBl;
import pe.edu.usmp.aduanatrack.embarque.repository.EmbarqueRepository;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService.Plazo;

class EmbarqueServiceTest {

    private static final LocalDate ETA = LocalDate.of(2026, 10, 20);

    private EmbarqueRepository embarqueRepository;
    private DespachoRepository despachoRepository;
    private EmbarqueService servicio;
    private Despacho despacho;

    @BeforeEach
    void preparar() {
        embarqueRepository = mock(EmbarqueRepository.class);
        despachoRepository = mock(DespachoRepository.class);
        PlazoAlmacenajeService plazo = mock(PlazoAlmacenajeService.class);
        when(plazo.fechaLimite(any())).thenAnswer(i -> ((LocalDate) i.getArgument(0)).plusDays(30));
        when(plazo.plazo(any())).thenReturn(new Plazo(12L, false));

        despacho = new Despacho(new Cliente("20787789324", LocalDateTime.now()), null, LocalDateTime.now());
        ReflectionTestUtils.setField(despacho, "id", 1L);
        despacho.abrir("DESP-2026-0001");
        when(despachoRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(embarqueRepository.buscarPorDespacho(1L)).thenReturn(Optional.empty());
        when(embarqueRepository.saveAndFlush(any(Embarque.class))).thenAnswer(i -> i.getArgument(0));

        servicio = new EmbarqueService(embarqueRepository, despachoRepository, plazo);
    }

    @Test
    void guardar_asociaElBlAlDespachoConDosContenedores() {
        EmbarqueResponse r = servicio.guardar(1L, solicitud(ETA,
                new ContenedorRequest("csqu3054383", "40HC", "PR-1", new BigDecimal("18540.00")),
                new ContenedorRequest("MSKU1234565", "20GP", null, null)));

        assertThat(r.despachoId()).isEqualTo(1L);
        assertThat(r.numeroDespacho()).isEqualTo("DESP-2026-0001");
        assertThat(r.numeroBl()).isEqualTo("HBL-2026-00125");
        assertThat(r.fechaLimiteAlmacenaje()).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(r.numeroViaje()).isEqualTo("V.226E");
        assertThat(r.contenedores().get(0).pesoBrutoKg()).isEqualByComparingTo("18540");
        assertThat(r.contenedores()).extracting(EmbarqueResponse.ContenedorResponse::numero)
                .containsExactly("CSQU3054383", "MSKU1234565");
        assertThat(r.etaModificada()).isFalse();
    }

    @Test
    void cambiarLaEta_recalculaElLimiteYAvisaElCambio() {
        Embarque existente = new Embarque(despacho);
        ReflectionTestUtils.setField(existente, "id", 3L);
        existente.setFechaEta(ETA);
        existente.sincronizarContenedores(List.of(new Embarque.DatosContenedor("CSQU3054383", "40HC", null, null)));
        when(embarqueRepository.buscarPorDespacho(1L)).thenReturn(Optional.of(existente));

        EmbarqueResponse r = servicio.guardar(1L, solicitud(LocalDate.of(2026, 10, 25),
                new ContenedorRequest("CSQU3054383", "40HC", null, null)));

        assertThat(r.fechaLimiteAlmacenaje()).isEqualTo(LocalDate.of(2026, 11, 24));
        assertThat(r.etaModificada()).isTrue();
        assertThat(r.etaAnterior()).isEqualTo(ETA);
    }

    @Test
    void contenedorRepetido_responde400IndicandoLaFila() {
        assertThatThrownBy(() -> servicio.guardar(1L, solicitud(ETA,
                new ContenedorRequest("CSQU3054383", null, null, null),
                new ContenedorRequest("csqu3054383 ", null, null, null))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsOnlyKeys("contenedores[1].numero"));
        verify(embarqueRepository, never()).saveAndFlush(any());
    }

    @Test
    void fechaEmbarquePosteriorALaEta_responde400() {
        EmbarqueRequest s = new EmbarqueRequest("HBL-1", TipoBl.HOUSE, "Maersk", null, null, "Shanghai", null,
                ETA.plusDays(1), ETA, List.of(new ContenedorRequest("CSQU3054383", null, null, null)));

        assertThatThrownBy(() -> servicio.guardar(1L, s)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCampos()).containsKey("fechaEmbarque"));
    }

    @Test
    void despachoEnBorrador_noAdmiteEmbarque() {
        Despacho borrador = new Despacho(new Cliente("20787789324", LocalDateTime.now()), null, LocalDateTime.now());
        when(despachoRepository.findById(2L)).thenReturn(Optional.of(borrador));

        assertThatThrownBy(() -> servicio.guardar(2L, solicitud(ETA, new ContenedorRequest("CSQU3054383", null, null, null))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_EN_BORRADOR);
                });
    }

    @Test
    void despachoInexistente_responde404() {
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.guardar(99L, solicitud(ETA, new ContenedorRequest("CSQU3054383", null, null, null))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void sincronizar_conservaLosQueSiguenQuitaLosDemasYAgregaNuevos() {
        Embarque e = new Embarque(despacho);
        e.sincronizarContenedores(List.of(
                new Embarque.DatosContenedor("CSQU3054383", "40HC", null, null),
                new Embarque.DatosContenedor("MSKU1234565", "20GP", null, null)));
        var original = e.getContenedores().get(0);

        e.sincronizarContenedores(List.of(
                new Embarque.DatosContenedor("CSQU3054383", "40GP", "P-9", null),
                new Embarque.DatosContenedor("TGHU9876542", "40HC", null, null)));

        assertThat(e.getContenedores()).extracting(c -> c.getNumero())
                .containsExactly("CSQU3054383", "TGHU9876542");
        assertThat(e.getContenedores().get(0)).isSameAs(original);
        assertThat(original.getTipo()).isEqualTo("40GP");
    }

    private static EmbarqueRequest solicitud(LocalDate eta, ContenedorRequest... contenedores) {
        return new EmbarqueRequest("HBL-2026-00125", TipoBl.HOUSE, "Maersk Line", "MSC Aurora", "V.226E", "Shanghai",
                "Callao", eta.minusDays(25), eta, List.of(contenedores));
    }
}
