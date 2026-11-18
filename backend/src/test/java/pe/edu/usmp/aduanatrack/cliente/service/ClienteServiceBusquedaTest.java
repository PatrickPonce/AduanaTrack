package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** La búsqueda de clientes ignora mayúsculas y tildes («pacifico» encuentra «Pacífico Trade»). */
class ClienteServiceBusquedaTest {

    @Test
    void quitaTildesYPasaAMinusculas() {
        assertThat(ClienteService.sinTildes("Pacífico")).isEqualTo("pacifico");
        assertThat(ClienteService.sinTildes("PERÚ Andina")).isEqualTo("peru andina");
        assertThat(ClienteService.sinTildes("Muñoz")).isEqualTo("munoz");
    }

    @Test
    void noAlteraUnRuc() {
        assertThat(ClienteService.sinTildes("20512345671")).isEqualTo("20512345671");
    }
}
