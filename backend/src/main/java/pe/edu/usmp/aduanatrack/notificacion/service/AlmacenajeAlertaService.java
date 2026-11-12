package pe.edu.usmp.aduanatrack.notificacion.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService;
import pe.edu.usmp.aduanatrack.notificacion.dto.AlmacenajePorVencer;
import pe.edu.usmp.aduanatrack.notificacion.repository.AlmacenajeRepository;

/**
 * Alerta diaria de almacenaje por vencer (HU-14 · T-14.2, CA1), con el mismo patrón que
 * {@code CartaPoderAlertaService}: busca los despachos sin levante cuya fecha límite está dentro de
 * {@code DIAS_ALERTA_ALMACENAJE} días (o ya pasó) y avisa al agente responsable con una alerta «Crítica».
 * Los despachos siguen alertándose cada día hasta que se registra el levante, pero nunca dos veces el mismo
 * día: si la tarea corre otra vez en la misma fecha no vuelve a avisar.
 */
@Service
public class AlmacenajeAlertaService {

    /** Todos los días a las 7:00. */
    static final String CRON_DIARIO = "0 0 7 * * *";

    private static final Logger log = LoggerFactory.getLogger(AlmacenajeAlertaService.class);

    private final AlmacenajeRepository repositorio;
    private final ParametroService parametros;
    private final AvisoDespachoService avisos;
    private final Clock clock;
    private volatile LocalDate ultimaEjecucion;

    public AlmacenajeAlertaService(AlmacenajeRepository repositorio, ParametroService parametros,
            AvisoDespachoService avisos, Clock clock) {
        this.repositorio = repositorio;
        this.parametros = parametros;
        this.avisos = avisos;
        this.clock = clock;
    }

    /** @return cantidad de despachos alertados en esta ejecución */
    @Scheduled(cron = "${aduanatrack.notificaciones.cron-almacenaje:" + CRON_DIARIO + "}",
            zone = "${aduanatrack.zona-horaria:America/Lima}")
    @Transactional
    public int generarAlertas() {
        LocalDate hoy = LocalDate.now(clock);
        if (hoy.equals(ultimaEjecucion)) {
            log.info("Alerta de almacenaje: ya se generó hoy ({})", hoy);
            return 0;
        }
        LocalDate limite = PlazoAlmacenajeService.fechaLimiteEnAlerta(hoy,
                parametros.entero(ParametroService.DIAS_ALERTA_ALMACENAJE));
        List<AlmacenajePorVencer> porVencer = repositorio.sinLevanteConPlazoHasta(limite, EstadoDespacho.BORRADOR);
        for (AlmacenajePorVencer alerta : porVencer) {
            avisos.almacenajePorVencer(alerta.despacho(), alerta.fechaLimite(), hoy);
        }
        ultimaEjecucion = hoy;
        log.info("Alerta de almacenaje: {} despacho(s) con plazo hasta el {}", porVencer.size(), limite);
        return porVencer.size();
    }
}
