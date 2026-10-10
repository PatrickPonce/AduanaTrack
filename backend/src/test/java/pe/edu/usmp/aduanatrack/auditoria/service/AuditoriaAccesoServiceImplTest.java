package pe.edu.usmp.aduanatrack.auditoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auditoria.ResultadoAcceso;
import pe.edu.usmp.aduanatrack.auditoria.entity.AuditoriaAcceso;
import pe.edu.usmp.aduanatrack.auditoria.repository.AuditoriaAccesoRepository;

class AuditoriaAccesoServiceImplTest {

    private static final String IP = "190.12.34.56";
    private static final String AGENTE = "Mozilla/5.0 (JUnit)";

    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-06T14:00:00Z"), ZoneId.of("America/Lima"));

    private AuditoriaAccesoRepository repositorio;
    private AuditoriaAccesoServiceImpl servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(AuditoriaAccesoRepository.class);
        servicio = new AuditoriaAccesoServiceImpl(repositorio, reloj);
    }

    @Test
    void tresFallidosYUnBloqueado_generanCuatroFilasConIpYHora() {
        servicio.registrar("agente", 2L, IP, AGENTE, ResultadoAcceso.FALLIDO);
        servicio.registrar("agente", 2L, IP, AGENTE, ResultadoAcceso.FALLIDO);
        servicio.registrar("agente", 2L, IP, AGENTE, ResultadoAcceso.FALLIDO);
        servicio.registrar("agente", 2L, IP, AGENTE, ResultadoAcceso.BLOQUEADO);

        List<AuditoriaAcceso> filas = guardadas(4);
        assertThat(filas).extracting(AuditoriaAcceso::getResultado).containsExactly(ResultadoAcceso.FALLIDO,
                ResultadoAcceso.FALLIDO, ResultadoAcceso.FALLIDO, ResultadoAcceso.BLOQUEADO);
        assertThat(filas).allSatisfy(fila -> {
            assertThat(fila.getUsuarioIngresado()).isEqualTo("agente");
            assertThat(fila.getUsuarioId()).isEqualTo(2L);
            assertThat(fila.getIp()).isEqualTo(IP);
            assertThat(fila.getUserAgent()).isEqualTo(AGENTE);
            // 14:00 UTC = 09:00 en Lima.
            assertThat(fila.getFecha()).isEqualTo(LocalDateTime.of(2026, 10, 6, 9, 0));
        });
    }

    @Test
    void usuarioInexistente_guardaUsuarioIdNulo() {
        servicio.registrar("no-existe@aduanatrack.pe", null, IP, AGENTE, ResultadoAcceso.FALLIDO);

        AuditoriaAcceso fila = guardadas(1).get(0);
        assertThat(fila.getUsuarioId()).isNull();
        assertThat(fila.getUsuarioIngresado()).isEqualTo("no-existe@aduanatrack.pe");
    }

    @Test
    void textosLargosDelCliente_seRecortanAlTamanoDeLaColumna() {
        servicio.registrar("u".repeat(300), null, IP, "x".repeat(2000), ResultadoAcceso.FALLIDO);

        AuditoriaAcceso fila = guardadas(1).get(0);
        assertThat(fila.getUsuarioIngresado()).hasSize(AuditoriaAcceso.MAX_USUARIO_INGRESADO);
        assertThat(fila.getUserAgent()).hasSize(AuditoriaAcceso.MAX_USER_AGENT);
    }

    @Test
    void sinIpNiUserAgent_igualRegistraElIntento() {
        servicio.registrar("agente", 2L, null, null, ResultadoAcceso.EXITO);

        AuditoriaAcceso fila = guardadas(1).get(0);
        assertThat(fila.getIp()).isNull();
        assertThat(fila.getUserAgent()).isNull();
        assertThat(fila.getResultado()).isEqualTo(ResultadoAcceso.EXITO);
    }

    /** El intento debe sobrevivir a la excepción con la que termina un login fallido. */
    @Test
    void registrar_usaTransaccionPropia() throws NoSuchMethodException {
        Method metodo = AuditoriaAccesoServiceImpl.class.getMethod("registrar", String.class, Long.class,
                String.class, String.class, ResultadoAcceso.class);

        assertThat(metodo.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private List<AuditoriaAcceso> guardadas(int cantidad) {
        ArgumentCaptor<AuditoriaAcceso> captor = ArgumentCaptor.forClass(AuditoriaAcceso.class);
        verify(repositorio, times(cantidad)).save(captor.capture());
        return captor.getAllValues();
    }
}
