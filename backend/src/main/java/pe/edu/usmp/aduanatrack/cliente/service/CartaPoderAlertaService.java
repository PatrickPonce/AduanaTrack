package pe.edu.usmp.aduanatrack.cliente.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;

/**
 * Alerta automática de cartas poder que vencen en DIAS_ALERTA_CARTA_PODER días o menos
 * (HU-04 · T-04.2). Cada carta se alerta una sola vez: queda marcada en
 * {@code alerta_generada_en} y en el log; el envío al usuario llega con HU-14.
 */
@Service
public class CartaPoderAlertaService {

    /** Todos los días a las 7:00. */
    static final String CRON_DIARIO = "0 0 7 * * *";

    private static final Logger log = LoggerFactory.getLogger(CartaPoderAlertaService.class);

    private final CartaPoderRepository repositorio;
    private final ParametroService parametros;
    private final Clock clock;

    public CartaPoderAlertaService(CartaPoderRepository repositorio, ParametroService parametros, Clock clock) {
        this.repositorio = repositorio;
        this.parametros = parametros;
        this.clock = clock;
    }

    /** @return cantidad de cartas alertadas en esta ejecución */
    @Scheduled(cron = "${aduanatrack.cartas-poder.cron-alerta:" + CRON_DIARIO + "}",
            zone = "${aduanatrack.zona-horaria:America/Lima}")
    @Transactional
    public int generarAlertas() {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limite = hoy.plusDays(parametros.entero(ParametroService.DIAS_ALERTA_CARTA_PODER));
        List<CartaPoder> cartas = repositorio.porVencerSinAlerta(limite);
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (CartaPoder carta : cartas) {
            carta.setAlertaGeneradaEn(ahora);
            log.warn("Carta poder {} del cliente {} vence el {} ({} días)", carta.getId(), carta.getClienteId(),
                    carta.getFechaVencimiento(), ChronoUnit.DAYS.between(hoy, carta.getFechaVencimiento()));
        }
        log.info("Alerta de cartas poder: {} carta(s) con vencimiento hasta el {}", cartas.size(), limite);
        return cartas.size();
    }
}
