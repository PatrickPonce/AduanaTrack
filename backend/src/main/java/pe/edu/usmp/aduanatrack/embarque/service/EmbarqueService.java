package pe.edu.usmp.aduanatrack.embarque.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.embarque.dto.ContenedorRequest;
import pe.edu.usmp.aduanatrack.embarque.dto.EmbarqueRequest;
import pe.edu.usmp.aduanatrack.embarque.dto.EmbarqueResponse;
import pe.edu.usmp.aduanatrack.embarque.entity.Embarque;
import pe.edu.usmp.aduanatrack.embarque.entity.Embarque.DatosContenedor;
import pe.edu.usmp.aduanatrack.embarque.repository.EmbarqueRepository;

/** Registro del embarque/BL del despacho y su plazo de almacenaje (HU-05 · T-05.1 y T-05.3). */
@Service
public class EmbarqueService {

    private final EmbarqueRepository embarqueRepository;
    private final DespachoRepository despachoRepository;
    private final PlazoAlmacenajeService plazoAlmacenaje;

    public EmbarqueService(EmbarqueRepository embarqueRepository, DespachoRepository despachoRepository,
            PlazoAlmacenajeService plazoAlmacenaje) {
        this.embarqueRepository = embarqueRepository;
        this.despachoRepository = despachoRepository;
        this.plazoAlmacenaje = plazoAlmacenaje;
    }

    @Transactional(readOnly = true)
    public EmbarqueResponse obtener(Long despachoId) {
        buscarDespacho(despachoId);
        Embarque embarque = embarqueRepository.buscarPorDespacho(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("El despacho aún no tiene embarque registrado."));
        return respuesta(embarque, null);
    }

    /** Crea o actualiza el embarque del despacho; la fecha límite se recalcula en cada guardado (CA3). */
    @Transactional
    public EmbarqueResponse guardar(Long despachoId, EmbarqueRequest solicitud) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.esBorrador()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_EN_BORRADOR,
                    "Confirme el despacho con \"Guardar y continuar\" antes de registrar el embarque.");
        }
        List<DatosContenedor> contenedores = validar(solicitud);

        Embarque embarque = embarqueRepository.buscarPorDespacho(despachoId).orElseGet(() -> new Embarque(despacho));
        LocalDate etaAnterior = embarque.getId() != null && !solicitud.fechaEta().equals(embarque.getFechaEta())
                ? embarque.getFechaEta()
                : null;

        embarque.setNumeroBl(solicitud.numeroBl().trim());
        embarque.setTipoBl(solicitud.tipoBl());
        embarque.setNaviera(solicitud.naviera().trim());
        embarque.setBuque(limpiar(solicitud.buque()));
        embarque.setPuertoEmbarque(solicitud.puertoEmbarque().trim());
        embarque.setPuertoDescarga(limpiar(solicitud.puertoDescarga()));
        embarque.setFechaEmbarque(solicitud.fechaEmbarque());
        embarque.setFechaEta(solicitud.fechaEta());
        embarque.setFechaLimiteAlmacenaje(plazoAlmacenaje.fechaLimite(solicitud.fechaEta()));
        embarque.sincronizarContenedores(contenedores);

        return respuesta(embarqueRepository.saveAndFlush(embarque), etaAnterior);
    }

    /**
     * Reglas que no cubren las anotaciones: contenedores sin repetir en el embarque (se
     * indica la fila) y fecha de embarque no posterior a la ETA.
     */
    private static List<DatosContenedor> validar(EmbarqueRequest solicitud) {
        Map<String, String> campos = new LinkedHashMap<>();
        Set<String> vistos = new HashSet<>();
        List<ContenedorRequest> filas = solicitud.contenedores();
        List<DatosContenedor> datos = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            ContenedorRequest fila = filas.get(i);
            String numero = fila.numero().trim().toUpperCase(Locale.ROOT);
            if (!vistos.add(numero)) {
                campos.put("contenedores[" + i + "].numero", "Contenedor repetido en este embarque");
            }
            datos.add(new DatosContenedor(numero, limpiar(fila.tipo()), limpiar(fila.precinto())));
        }
        if (solicitud.fechaEmbarque() != null && solicitud.fechaEmbarque().isAfter(solicitud.fechaEta())) {
            campos.put("fechaEmbarque", "La fecha de embarque debe ser anterior o igual a la ETA");
        }
        if (!campos.isEmpty()) {
            throw ApiException.validacion(campos);
        }
        return datos;
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    private EmbarqueResponse respuesta(Embarque embarque, LocalDate etaAnterior) {
        return EmbarqueResponse.de(embarque, plazoAlmacenaje.plazo(embarque.getFechaLimiteAlmacenaje()), etaAnterior);
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
