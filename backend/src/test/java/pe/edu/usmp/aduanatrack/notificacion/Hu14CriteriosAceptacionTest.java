package pe.edu.usmp.aduanatrack.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.CorreoProperties;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.CanalControlService;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.notificacion.dto.AlmacenajePorVencer;
import pe.edu.usmp.aduanatrack.notificacion.dto.NotificacionResponse;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Notificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.repository.AlmacenajeRepository;
import pe.edu.usmp.aduanatrack.notificacion.repository.NotificacionRepository;
import pe.edu.usmp.aduanatrack.notificacion.service.AlmacenajeAlertaService;
import pe.edu.usmp.aduanatrack.notificacion.service.AvisoDespachoService;
import pe.edu.usmp.aduanatrack.notificacion.service.CorreoService;
import pe.edu.usmp.aduanatrack.notificacion.service.NotificacionService;
import pe.edu.usmp.aduanatrack.support.ServidorSmtpDePrueba;

/**
 * Pruebas de aceptación de HU-14 · T-14.4 (SCRUM-93): una por criterio, con los servicios reales encadenados
 * (tarea de almacenaje, canal de control, avisos, notificaciones y correo). Solo se reemplazan la base de datos,
 * con repositorios en memoria que conservan lo guardado, y Mailpit, con un servidor SMTP local dentro de la
 * prueba que recibe el correo por SMTP de verdad.
 */
class Hu14CriteriosAceptacionTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    /** 07:00 del martes 10/11/2026 en Lima: la hora a la que corre la tarea diaria. */
    private static final Instant AHORA = Instant.parse("2026-11-10T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final Long AGENTE = 9L;
    private static final Long OTRO_USUARIO = 10L;
    private static final Long DESPACHO = 7L;

    private final List<Notificacion> bandejas = new ArrayList<>();
    private ServidorSmtpDePrueba mailpit;
    private Usuario agente;
    private Despacho despacho;
    private AlmacenajeRepository almacenaje;
    private HitoDespachoService hitos;
    private NotificacionService notificaciones;
    private AlmacenajeAlertaService tareaAlmacenaje;
    private CanalControlService canales;

    @BeforeEach
    void preparar() {
        mailpit = new ServidorSmtpDePrueba();
        Clock reloj = Clock.fixed(AHORA, LIMA);

        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        ReflectionTestUtils.setField(agente, "id", AGENTE);
        despacho = new Despacho(new Cliente("20787789324", LocalDateTime.of(2026, 10, 20, 9, 0)), agente,
                LocalDateTime.of(2026, 10, 20, 9, 0));
        ReflectionTestUtils.setField(despacho, "id", DESPACHO);
        despacho.abrir("DESP-2026-0007");

        DespachoRepository despachoRepository = mock(DespachoRepository.class);
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(despachoRepository.getReferenceById(DESPACHO)).thenReturn(despacho);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        when(usuarioRepository.findById(AGENTE)).thenReturn(Optional.of(agente));

        notificaciones = new NotificacionService(notificacionesEnMemoria(), usuarioRepository, despachoRepository,
                reloj);
        AvisoDespachoService avisos = new AvisoDespachoService(notificaciones, correoHaciaMailpit());

        almacenaje = mock(AlmacenajeRepository.class);
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.entero(ParametroService.DIAS_ALERTA_ALMACENAJE)).thenReturn(3);
        tareaAlmacenaje = new AlmacenajeAlertaService(almacenaje, parametros, avisos, reloj);

        hitos = mock(HitoDespachoService.class);
        canales = new CanalControlService(despachoRepository, hitos, avisos);
    }

    @AfterEach
    void cerrar() throws Exception {
        mailpit.close();
    }

    @Test
    void criterio1_unDespachoConDosDiasDeAlmacenajeRestantesGeneraUnaAlertaCriticaAlCorrerLaTarea() {
        conAlmacenajeQueVenceEl(HOY.plusDays(2));

        int alertados = tareaAlmacenaje.generarAlertas();

        assertThat(alertados).isEqualTo(1);
        List<NotificacionResponse> bandeja = bandejaDelAgente("NO_LEIDAS");
        assertThat(bandeja).singleElement().satisfies(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.PLAZO_ALMACENAJE_POR_VENCER);
            assertThat(n.severidad()).isEqualTo(Severidad.CRITICA);
            assertThat(n.titulo()).isEqualTo("Almacenaje por vencer");
            assertThat(n.mensaje()).isEqualTo("DESP-2026-0007 vence en 2 días");
            assertThat(n.despachoId()).isEqualTo(DESPACHO);
            assertThat(n.leida()).isFalse();
        });
        assertThat(notificaciones.cantidadNoLeidas(AGENTE).cantidad()).isEqualTo(1);

        // La tarea no repite la alerta si vuelve a correr el mismo día.
        assertThat(tareaAlmacenaje.generarAlertas()).isZero();
        assertThat(bandejaDelAgente("TODAS")).hasSize(1);
    }

    @Test
    void criterio2_guardarUnCanalCreaUnaNotificacionParaElAgenteDelDespacho() {
        despacho.registrarDam("118-2026-10-000321", HOY.minusDays(1), null, null);

        canales.guardar(DESPACHO, CanalControl.ROJO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CANAL_ASIGNADO);
        verify(hitos).registrar(eq(DESPACHO), eq(TipoHito.CANAL_ASIGNADO), any());
        assertThat(bandejaDelAgente("NO_LEIDAS")).singleElement().satisfies(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.CAMBIO_ESTADO);
            assertThat(n.severidad()).isEqualTo(Severidad.ALTA);
            assertThat(n.titulo()).isEqualTo("Canal de control asignado");
            assertThat(n.mensaje()).isEqualTo("DESP-2026-0007: canal rojo, programe la inspección en el depósito");
            assertThat(n.numeroDespacho()).isEqualTo("DESP-2026-0007");
        });

        // Volver a guardar el mismo canal no avisa otra vez; corregirlo sí.
        canales.guardar(DESPACHO, CanalControl.ROJO);
        assertThat(bandejaDelAgente("TODAS")).hasSize(1);
        canales.guardar(DESPACHO, CanalControl.VERDE);
        assertThat(bandejaDelAgente("TODAS")).hasSize(2);
        assertThat(bandejaDelAgente("TODAS").get(0).mensaje())
                .isEqualTo("DESP-2026-0007: canal verde, ya puede registrar el levante");
    }

    @Test
    void criterio3_conLaPreferenciaAmbosLaAlertaLlegaPorCorreoYApareceEnElSistema() throws Exception {
        notificaciones.actualizarPreferencias(AGENTE, CanalNotificacion.AMBOS);
        conAlmacenajeQueVenceEl(HOY.plusDays(2));

        tareaAlmacenaje.generarAlertas();

        assertThat(bandejaDelAgente("NO_LEIDAS")).singleElement()
                .satisfies(n -> assertThat(n.severidad()).isEqualTo(Severidad.CRITICA));
        assertThat(mailpit.recibidos()).hasSize(1);
        MimeMessage correo = mailpit.recibidos().get(0);
        assertThat(correo.getAllRecipients()).extracting(Object::toString).containsExactly("agente@aduanatrack.pe");
        assertThat(correo.getFrom()).extracting(Object::toString).containsExactly("no-responder@aduanatrack.local");
        assertThat(correo.getSubject()).isEqualTo("Almacenaje por vencer");
        assertThat(correo.getContent().toString()).contains("DESP-2026-0007 vence en 2 días");
    }

    @Test
    void criterio3_conLaPreferenciaSistemaQuedaEnElSistemaYNoSaleNingunCorreo() {
        assertThat(notificaciones.preferencias(AGENTE).canalNotificacion()).isEqualTo(CanalNotificacion.SISTEMA);
        conAlmacenajeQueVenceEl(HOY.plusDays(2));

        tareaAlmacenaje.generarAlertas();

        assertThat(bandejaDelAgente("NO_LEIDAS")).hasSize(1);
        assertThat(mailpit.recibidos()).isEmpty();
    }

    @Test
    void criterio4_marcarComoLeidaLaPasaDeNoLeidaALeidaYBajaElContador() {
        conAlmacenajeQueVenceEl(HOY.plusDays(2));
        tareaAlmacenaje.generarAlertas();
        despacho.registrarDam("118-2026-10-000321", HOY.minusDays(1), null, null);
        canales.guardar(DESPACHO, CanalControl.NARANJA);
        assertThat(notificaciones.cantidadNoLeidas(AGENTE).cantidad()).isEqualTo(2);
        Long alerta = bandejaDelAgente("NO_LEIDAS").stream()
                .filter(n -> n.tipo() == TipoNotificacion.PLAZO_ALMACENAJE_POR_VENCER)
                .findFirst().orElseThrow().id();

        NotificacionResponse leida = notificaciones.marcarLeida(alerta, AGENTE);

        assertThat(leida.leida()).isTrue();
        assertThat(leida.leidaEn()).isEqualTo(LocalDateTime.ofInstant(AHORA, LIMA));
        assertThat(notificaciones.cantidadNoLeidas(AGENTE).cantidad()).isEqualTo(1);
        assertThat(bandejaDelAgente("NO_LEIDAS")).extracting(NotificacionResponse::id).doesNotContain(alerta);
        assertThat(bandejaDelAgente("TODAS")).hasSize(2);
    }

    @Test
    void criterio4_otroUsuarioNoPuedeMarcarLaNotificacionDelAgenteYElContadorNoCambia() {
        conAlmacenajeQueVenceEl(HOY.plusDays(2));
        tareaAlmacenaje.generarAlertas();
        Long alerta = bandejaDelAgente("NO_LEIDAS").get(0).id();

        assertThatThrownBy(() -> notificaciones.marcarLeida(alerta, OTRO_USUARIO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.NO_ENCONTRADO));
        assertThat(notificaciones.cantidadNoLeidas(AGENTE).cantidad()).isEqualTo(1);
        assertThat(notificaciones.cantidadNoLeidas(OTRO_USUARIO).cantidad()).isZero();
    }

    private void conAlmacenajeQueVenceEl(LocalDate fechaLimite) {
        // DIAS_ALERTA_ALMACENAJE = 3: la tarea busca hasta hoy + 3 días.
        when(almacenaje.sinLevanteConPlazoHasta(HOY.plusDays(3), EstadoDespacho.BORRADOR))
                .thenReturn(List.of(new AlmacenajePorVencer(despacho, fechaLimite)));
    }

    private List<NotificacionResponse> bandejaDelAgente(String estado) {
        return notificaciones.listar(AGENTE, estado, 0, 20).contenido();
    }

    /** Repositorio en memoria: lo que se guarda es lo que después se lista, se cuenta y se marca. */
    private NotificacionRepository notificacionesEnMemoria() {
        NotificacionRepository repositorio = mock(NotificacionRepository.class);
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> {
            Notificacion n = i.getArgument(0);
            if (n.getId() == null) {
                ReflectionTestUtils.setField(n, "id", 100L + bandejas.size());
                bandejas.add(n);
            }
            return n;
        });
        when(repositorio.countByUsuarioIdAndLeidaFalse(anyLong())).thenAnswer(i -> bandejas.stream()
                .filter(n -> n.getUsuarioId().equals(i.getArgument(0)) && !n.isLeida()).count());
        when(repositorio.buscarDelUsuario(anyLong(), anyLong())).thenAnswer(i -> bandejas.stream()
                .filter(n -> n.getId().equals(i.getArgument(0)) && n.getUsuarioId().equals(i.getArgument(1)))
                .findFirst());
        when(repositorio.bandeja(anyLong(), anyBoolean(), any(Pageable.class))).thenAnswer(i -> {
            Long usuario = i.getArgument(0);
            boolean soloNoLeidas = i.getArgument(1);
            List<Notificacion> filas = bandejas.stream()
                    .filter(n -> n.getUsuarioId().equals(usuario) && (!soloNoLeidas || !n.isLeida()))
                    .sorted(Comparator.comparing(Notificacion::getCreadaEn).thenComparing(Notificacion::getId)
                            .reversed())
                    .toList();
            return new PageImpl<>(filas, i.getArgument(2), filas.size());
        });
        return repositorio;
    }

    /** CorreoService real con la propiedad activa y un JavaMailSender que entrega al SMTP de la prueba. */
    @SuppressWarnings("unchecked")
    private CorreoService correoHaciaMailpit() {
        JavaMailSenderImpl enviador = new JavaMailSenderImpl();
        enviador.setHost(mailpit.host());
        enviador.setPort(mailpit.puerto());
        enviador.setDefaultEncoding("UTF-8");
        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.connectiontimeout", "5000");
        propiedades.put("mail.smtp.timeout", "5000");
        enviador.setJavaMailProperties(propiedades);
        ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(enviador);
        return new CorreoService(proveedor, new CorreoProperties(true, "no-responder@aduanatrack.local"));
    }
}
