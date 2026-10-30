package com.electronica.suministros.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.electronica.suministros.entity.CategoriaMaterial;

public interface CategoriaMaterialRepository extends JpaRepository<CategoriaMaterial, Integer> {

    List<CategoriaMaterial> findAllByOrderByNombreCategoriaAsc();

    List<CategoriaMaterial> findByEstadoOrderByNombreCategoriaAsc(String estado);

    boolean existsByNombreCategoriaIgnoreCase(String nombreCategoria);

    boolean existsByNombreCategoriaIgnoreCaseAndIdCategoriaNot(String nombreCategoria, Integer idCategoria);
}
