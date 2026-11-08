package pe.edu.usmp.aduanatrack.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import pe.edu.usmp.aduanatrack.config.CorreoProperties;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;

/** Envío por correo de HU-14 · T-14.2 (SCRUM-91); CA3: con la preferencia «Ambos» la alerta llega por correo. */
class CorreoServiceTest {

    private JavaMailSender enviador;
    private ObjectProvider<JavaMailSender> proveedor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        enviador = mock(JavaMailSender.class);
        proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(enviador);
    }

    private CorreoService servicio(boolean habilitado) {
        return new CorreoService(proveedor, new CorreoProperties(habilitado, "no-responder@aduanatrack.local"));
    }

    @Test
    void conLaPreferenciaAmbos_enviaElCorreoAlAgente() {
        boolean enviado = servicio(true).enviarSegunPreferencia(CanalNotificacion.AMBOS, "agente@ejemplo.pe", "Almacenaje por vencer",
                "DESP-2026-0007 vence en 2 días");

        assertThat(enviado).isTrue();
        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(mensaje.capture());
        assertThat(mensaje.getValue().getFrom()).isEqualTo("no-responder@aduanatrack.local");
        assertThat(mensaje.getValue().getTo()).containsExactly("agente@ejemplo.pe");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Almacenaje por vencer");
        assertThat(mensaje.getValue().getText()).isEqualTo("DESP-2026-0007 vence en 2 días");
    }

    @Test
    void conLaPreferenciaCorreo_tambienEnvia() {
        assertThat(servicio(true).enviarSegunPreferencia(CanalNotificacion.CORREO, "agente@ejemplo.pe", "Asunto", "Texto")).isTrue();
    }

    @Test
    void conLaPreferenciaSistema_noEnviaNada() {
        assertThat(servicio(true).enviarSegunPreferencia(CanalNotificacion.SISTEMA, "agente@ejemplo.pe", "Asunto", "Texto")).isFalse();
        assertThat(servicio(true).enviarSegunPreferencia(null, "agente@ejemplo.pe", "Asunto", "Texto")).isFalse();

        verify(enviador, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void conLaPropiedadDesactivada_noEnviaAunqueLaPreferenciaSeaAmbos() {
        assertThat(servicio(false).enviarSegunPreferencia(CanalNotificacion.AMBOS, "agente@ejemplo.pe", "Asunto", "Texto")).isFalse();

        verify(enviador, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sinServidorSmtpConfigurado_noEnviaYNoFalla() {
        when(proveedor.getIfAvailable()).thenReturn(null);

        assertThat(servicio(true).enviar("agente@ejemplo.pe", "Asunto", "Texto")).isFalse();
    }

    @Test
    void sinDestinatario_noEnvia() {
        assertThat(servicio(true).enviar(" ", "Asunto", "Texto")).isFalse();
        assertThat(servicio(true).enviar(null, "Asunto", "Texto")).isFalse();

        verify(enviador, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void siElServidorDeCorreoFalla_devuelveFalseSinLanzarLaExcepcion() {
        doThrow(new MailSendException("SMTP caído")).when(enviador).send(any(SimpleMailMessage.class));

        assertThat(servicio(true).enviarSegunPreferencia(CanalNotificacion.AMBOS, "agente@ejemplo.pe", "Asunto", "Texto")).isFalse();
    }
}
