package pe.edu.usmp.aduanatrack.embarque.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContenedorIso6346ValidatorTest {

    private final ContenedorIso6346Validator validador = new ContenedorIso6346Validator();

    @ParameterizedTest
    @ValueSource(strings = { "CSQU3054383", "MSKU1234565", "TGHU9876542" })
    void contenedorConDigitoDeControlCorrecto_esValido(String numero) {
        assertThat(ContenedorIso6346Validator.esValido(numero)).isTrue();
        assertThat(validador.isValid(numero, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CSQU3054384", // dígito de control errado
            "CSQ3054383", // falta la categoría
            "CSQA3054383", // categoría distinta de U, J o Z
            "CSQU305438", // 10 caracteres
            "CSQU30543833", // 12 caracteres
            "" })
    void contenedorIncorrecto_esRechazado(String numero) {
        assertThat(ContenedorIso6346Validator.esValido(numero)).isFalse();
    }

    @Test
    void aceptaMinusculasYEspaciosAlrededor() {
        assertThat(ContenedorIso6346Validator.esValido("  csqu3054383 ")).isTrue();
        assertThat(validador.isValid(" csqu3054384 ", null)).isFalse();
    }

    /** Si el resto de la suma es 10, el dígito de control es 0. */
    @Test
    void restoDiez_usaDigitoCero() {
        assertThat(ContenedorIso6346Validator.digitoDeControl("AAAU0000060")).isZero();
        assertThat(ContenedorIso6346Validator.esValido("AAAU0000060")).isTrue();
    }

    /** La forma la reporta @Pattern: aquí no se duplica el mensaje de la fila. */
    @Test
    void formaIncorrectaONulo_noLosReportaEstaAnotacion() {
        assertThat(validador.isValid("CSQ3054383", null)).isTrue();
        assertThat(validador.isValid(null, null)).isTrue();
        assertThat(ContenedorIso6346Validator.esValido(null)).isFalse();
    }
}
