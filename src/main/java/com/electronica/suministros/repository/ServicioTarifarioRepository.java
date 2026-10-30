package com.electronica.suministros.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.electronica.suministros.dto.ServicioListadoDTO;
import com.electronica.suministros.entity.ServicioTarifario;

public interface ServicioTarifarioRepository extends JpaRepository<ServicioTarifario, Integer> {

    List<ServicioTarifario> findByCategoria_IdCategoriaAndEstadoOrderByFormatoAsc(Integer idCategoria, String estado);

    boolean existsByFormatoIgnoreCase(String formato);

    boolean existsByFormatoIgnoreCaseAndIdServicioNot(String formato, Integer idServicio);

    long countByCategoria_IdCategoria(Integer idCategoria);

    /** SQL nativo con JOIN y una subconsulta: el tarifario con su categoría y cuántas veces se usó en OTs. */
    @Query(value = """
            SELECT s.id_servicio     AS idServicio,
                   s.codigo          AS codigo,
                   s.formato         AS formato,
                   c.id_categoria    AS idCategoria,
                   c.nombre_categoria AS categoria,
                   s.tipo_cobro      AS tipoCobro,
                   s.precio_unitario AS precioUnitario,
                   s.imagen          AS imagen,
                   s.estado          AS estado,
                   (SELECT COUNT(*) FROM detalle_orden d WHERE d.id_servicio = s.id_servicio) AS usos
              FROM servicio_tarifario s
              JOIN categoria_material c ON c.id_categoria = s.id_categoria
             ORDER BY c.nombre_categoria, s.formato
            """, nativeQuery = true)
    List<ServicioListadoDTO> listarConCategoria();

    /** Igual que "spadicion" de clase: el código ST014 lo genera SP_Registrar_Servicio dentro de la base de datos. */
    @Procedure(procedureName = "SP_Registrar_Servicio")
    void registrarConProcedimiento(@Param("p_id_categoria") Integer idCategoria,
                                   @Param("p_formato") String formato,
                                   @Param("p_tipo_cobro") String tipoCobro,
                                   @Param("p_precio") BigDecimal precio,
                                   @Param("p_imagen") String imagen);
}
