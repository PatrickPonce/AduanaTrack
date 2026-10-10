package pe.edu.usmp.aduanatrack.cliente.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RucValidatorTest {

    /** RUC de prueba del PO (Guía Sprint 1, sección 7). */
    @ParameterizedTest
    @ValueSource(strings = { "20787789324", "20230972151", "20923478826", "10688126342" })
    void rucConDigitoVerificadorCorrecto_esValido(String ruc) {
        assertThat(RucValidator.esValido(ruc)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20787789325", // dígito verificador errado
            "2078778932", // 10 dígitos
            "207877893241", // 12 dígitos
            "30787789324", // prefijo no permitido
            "2078778932A" })
    void rucIncorrecto_esRechazado(String ruc) {
        assertThat(RucValidator.esValido(ruc)).isFalse();
    }
}
