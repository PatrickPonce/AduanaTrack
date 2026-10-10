package pe.edu.usmp.aduanatrack.catalogo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.catalogo.dto.AduanaResponse;
import pe.edu.usmp.aduanatrack.catalogo.dto.RegimenResponse;
import pe.edu.usmp.aduanatrack.catalogo.repository.AduanaRepository;
import pe.edu.usmp.aduanatrack.catalogo.repository.RegimenAduaneroRepository;

/** Listas maestras del formulario de despacho: solo activos, ordenados por código (HU-03 · T-03.2). */
@Service
public class CatalogoService {

    private final RegimenAduaneroRepository regimenRepository;
    private final AduanaRepository aduanaRepository;

    public CatalogoService(RegimenAduaneroRepository regimenRepository, AduanaRepository aduanaRepository) {
        this.regimenRepository = regimenRepository;
        this.aduanaRepository = aduanaRepository;
    }

    @Transactional(readOnly = true)
    public List<RegimenResponse> regimenes() {
        return regimenRepository.findByActivoTrueOrderByCodigoAsc().stream().map(RegimenResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<AduanaResponse> aduanas() {
        return aduanaRepository.findByActivoTrueOrderByCodigoAsc().stream().map(AduanaResponse::de).toList();
    }
}
