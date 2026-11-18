package pe.edu.usmp.aduanatrack.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

/** Los errores del cliente HTTP se devuelven como 4xx y no como 500. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void metodoNoPermitidoEs405() {
        var respuesta = handler.handleMetodoNoPermitido(new HttpRequestMethodNotSupportedException("PUT"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().codigo()).isEqualTo(CodigoError.METODO_NO_PERMITIDO);
    }

    @Test
    void parametroAusenteEs400() {
        var respuesta = handler.handleParametro(new MissingServletRequestParameterException("q", "String"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
