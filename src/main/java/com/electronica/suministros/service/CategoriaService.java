package com.electronica.suministros.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.CategoriaCatalogoDTO;
import com.electronica.suministros.entity.CategoriaMaterial;
import com.electronica.suministros.repository.CategoriaMaterialRepository;
import com.electronica.suministros.repository.ServicioTarifarioRepository;

@Service
public class CategoriaService {

    private final CategoriaMaterialRepository categoriaRepo;
    private final ServicioTarifarioRepository servicioRepo;

    public CategoriaService(CategoriaMaterialRepository categoriaRepo, ServicioTarifarioRepository servicioRepo) {
        this.categoriaRepo = categoriaRepo;
        this.servicioRepo = servicioRepo;
    }

    @Transactional(readOnly = true)
    public List<CategoriaMaterial> listar() {
        return categoriaRepo.findAllByOrderByNombreCategoriaAsc();
    }

    @Transactional(readOnly = true)
    public List<CategoriaMaterial> listarActivas() {
        return categoriaRepo.findByEstadoOrderByNombreCategoriaAsc("ACTIVO");
    }

    /** Record para el JSON del primer combo de las cascadas. */
    @Transactional(readOnly = true)
    public List<CategoriaCatalogoDTO> catalogo() {
        return listarActivas().stream()
                .map(c -> new CategoriaCatalogoDTO(c.getIdCategoria(), c.getNombreCategoria(), c.getDescripcion(), c.getImagen()))
                .toList();
    }

    @Transactional
    public void guardar(Integer idCategoria, String nombre, String descripcion, String imagen, String estado) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Ingrese el nombre de la categoría");
        }
        String nombreFinal = nombre.trim();
        if (nombreFinal.length() > 50) {
            throw new NegocioException("El nombre no puede superar 50 caracteres");
        }
        CategoriaMaterial c;
        if (idCategoria == null) {
            if (categoriaRepo.existsByNombreCategoriaIgnoreCase(nombreFinal)) {
                throw new NegocioException("Ya existe la categoría " + nombreFinal);
            }
            c = new CategoriaMaterial();
        } else {
            c = categoriaRepo.findById(idCategoria).orElseThrow(() -> new NegocioException("La categoría no existe"));
            if (categoriaRepo.existsByNombreCategoriaIgnoreCaseAndIdCategoriaNot(nombreFinal, idCategoria)) {
                throw new NegocioException("Ya existe otra categoría llamada " + nombreFinal);
            }
        }
        c.setNombreCategoria(nombreFinal);
        c.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        c.setImagen(imagen == null || imagen.isBlank() ? "banner.jpg" : imagen.trim());
        c.setEstado("INACTIVO".equals(estado) ? "INACTIVO" : "ACTIVO");
        categoriaRepo.save(c);
    }

    @Transactional
    public void eliminar(Integer idCategoria) {
        if (servicioRepo.countByCategoria_IdCategoria(idCategoria) > 0) {
            throw new NegocioException("No se puede eliminar: la categoría tiene servicios en el tarifario");
        }
        categoriaRepo.deleteById(idCategoria);
    }
}
