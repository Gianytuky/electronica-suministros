package com.electronica.suministros.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.ServicioCatalogoDTO;
import com.electronica.suministros.dto.ServicioFormDTO;
import com.electronica.suministros.dto.ServicioListadoDTO;
import com.electronica.suministros.entity.CategoriaMaterial;
import com.electronica.suministros.entity.ServicioTarifario;
import com.electronica.suministros.repository.CategoriaMaterialRepository;
import com.electronica.suministros.repository.DetalleOrdenRepository;
import com.electronica.suministros.repository.ServicioTarifarioRepository;

@Service
public class ServicioService {

    private final ServicioTarifarioRepository servicioRepo;
    private final CategoriaMaterialRepository categoriaRepo;
    private final DetalleOrdenRepository detalleRepo;

    public ServicioService(ServicioTarifarioRepository servicioRepo, CategoriaMaterialRepository categoriaRepo,
                           DetalleOrdenRepository detalleRepo) {
        this.servicioRepo = servicioRepo;
        this.categoriaRepo = categoriaRepo;
        this.detalleRepo = detalleRepo;
    }

    /** Tarifario completo con su categoría (SQL nativo con JOIN). */
    @Transactional(readOnly = true)
    public List<ServicioListadoDTO> listar() {
        return servicioRepo.listarConCategoria();
    }

    /** Solo los servicios ACTIVOS (catálogo público). */
    @Transactional(readOnly = true)
    public List<ServicioListadoDTO> listarActivos() {
        return servicioRepo.listarConCategoria().stream().filter(s -> "ACTIVO".equals(s.getEstado())).toList();
    }

    /** Solo los servicios ACTIVOS de una categoría, como record JSON para el segundo combo de la cascada. */
    @Transactional(readOnly = true)
    public List<ServicioCatalogoDTO> porCategoria(Integer idCategoria) {
        return servicioRepo.findByCategoria_IdCategoriaAndEstadoOrderByFormatoAsc(idCategoria, "ACTIVO").stream()
                .map(s -> new ServicioCatalogoDTO(s.getIdServicio(), s.getCodigo(), s.getFormato(), s.getTipoCobro(),
                        s.getPrecioUnitario(), s.getImagen(), s.isEsVideo(), s.isAdmiteLimpieza()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicioFormDTO obtenerFormulario(Integer idServicio) {
        ServicioTarifario s = servicioRepo.findById(idServicio).orElseThrow(() -> new NegocioException("El servicio no existe"));
        ServicioFormDTO f = new ServicioFormDTO();
        f.setIdServicio(s.getIdServicio());
        f.setIdCategoria(s.getCategoria().getIdCategoria());
        f.setFormato(s.getFormato());
        f.setTipoCobro(s.getTipoCobro());
        f.setPrecioUnitario(s.getPrecioUnitario());
        f.setImagen(s.getImagen());
        f.setEstado(s.getEstado());
        return f;
    }

    /**
     * Alta: se llama a SP_Registrar_Servicio (la base de datos genera el código ST014).
     * Edición: se actualiza con JPA. Los precios históricos no cambian: cada OT guarda la tarifa que tenía al recibirse.
     */
    @Transactional
    public void guardar(ServicioFormDTO f) {
        if (!ServicioTarifario.COBRO_POR_HORA.equals(f.getTipoCobro()) && !ServicioTarifario.COBRO_POR_UNIDAD.equals(f.getTipoCobro())) {
            throw new NegocioException("Tipo de cobro no válido");
        }
        CategoriaMaterial categoria = categoriaRepo.findById(f.getIdCategoria())
                .orElseThrow(() -> new NegocioException("La categoría no existe"));
        String formato = f.getFormato().trim();
        String imagen = (f.getImagen() == null || f.getImagen().isBlank()) ? "banner.jpg" : f.getImagen().trim();

        if (f.getIdServicio() == null) {
            if (servicioRepo.existsByFormatoIgnoreCase(formato)) {
                throw new NegocioException("Ya existe un servicio con el formato " + formato);
            }
            servicioRepo.registrarConProcedimiento(categoria.getIdCategoria(), formato, f.getTipoCobro(),
                    f.getPrecioUnitario(), imagen);
            return;
        }

        ServicioTarifario s = servicioRepo.findById(f.getIdServicio()).orElseThrow(() -> new NegocioException("El servicio no existe"));
        if (servicioRepo.existsByFormatoIgnoreCaseAndIdServicioNot(formato, f.getIdServicio())) {
            throw new NegocioException("Ya existe otro servicio con el formato " + formato);
        }
        s.setCategoria(categoria);
        s.setFormato(formato);
        s.setTipoCobro(f.getTipoCobro());
        s.setPrecioUnitario(f.getPrecioUnitario());
        s.setImagen(imagen);
        s.setEstado("INACTIVO".equals(f.getEstado()) ? "INACTIVO" : "ACTIVO");
        servicioRepo.save(s);
    }

    /** Si el servicio ya se usó en alguna OT solo se desactiva (baja lógica); si no, se elimina. */
    @Transactional
    public String eliminar(Integer idServicio) {
        ServicioTarifario s = servicioRepo.findById(idServicio).orElseThrow(() -> new NegocioException("El servicio no existe"));
        if (detalleRepo.countByServicio_IdServicio(idServicio) > 0) {
            s.setEstado("INACTIVO");
            servicioRepo.save(s);
            return "El servicio ya se usó en órdenes de trabajo, por eso se desactivó en lugar de eliminarse.";
        }
        servicioRepo.delete(s);
        return "Servicio eliminado correctamente.";
    }
}
