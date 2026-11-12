package pe.edu.usmp.aduanatrack.acceso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;
import pe.edu.usmp.aduanatrack.acceso.entity.AccesoDenegado;
import pe.edu.usmp.aduanatrack.acceso.repository.AccesoDenegadoRepository;

class AccesoDenegadoServiceTest {

    @Test
    void registrar_guardaElIntentoConLaHoraDelReloj() {
        AccesoDenegadoRepository repositorio = mock(AccesoDenegadoRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-05T20:22:00Z"), ZoneId.of("America/Lima"));
        var servicio = new AccesoDenegadoService(repositorio, reloj);

        servicio.registrar(50L, 20L, 2L, MotivoAccesoDenegado.OTRA_EMPRESA);

        ArgumentCaptor<AccesoDenegado> fila = ArgumentCaptor.forClass(AccesoDenegado.class);
        verify(repositorio).save(fila.capture());
        assertThat(fila.getValue().getUsuarioId()).isEqualTo(50L);
        assertThat(fila.getValue().getDespachoId()).isEqualTo(20L);
        assertThat(fila.getValue().getClienteId()).isEqualTo(2L);
        assertThat(fila.getValue().getResultado()).isEqualTo(MotivoAccesoDenegado.OTRA_EMPRESA);
        assertThat(fila.getValue().getFecha()).isEqualTo(LocalDateTime.of(2026, 11, 5, 15, 22));
    }

    @Test
    void registrar_usaTransaccionPropiaParaQueSobrevivaAlRollbackDeLa404() throws Exception {
        Method metodo = AccesoDenegadoService.class.getMethod("registrar", Long.class, Long.class, Long.class,
                MotivoAccesoDenegado.class);

        assertThat(metodo.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
