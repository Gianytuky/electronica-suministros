package com.electronica.suministros.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.electronica.suministros.dto.ConteoDTO;
import com.electronica.suministros.dto.FormatoIngresoDTO;
import com.electronica.suministros.dto.OrdenDetalleDTO;
import com.electronica.suministros.entity.DetalleOrden;

public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Integer> {

    long countByServicio_IdServicio(Integer idServicio);

    /** SQL nativo con JOIN de 6 tablas: todas las líneas de una OT con su captura técnica (si existe). */
    @Query(value = """
            SELECT d.id_detalle          AS idDetalle,
                   s.formato             AS formato,
                   cat.nombre_categoria  AS categoria,
                   s.tipo_cobro          AS tipoCobro,
                   s.imagen              AS imagen,
                   d.cantidad_materiales AS cantidad,
                   d.precio_unitario     AS precioUnitario,
                   d.subtotal            AS subtotal,
                   IF(m.aplica_limpieza = 1, 1, 0) AS limpieza,
                   ct.duracion_horas_reales AS horasReales,
                   ct.peso_gb            AS pesoGb,
                   ct.medio_entrega      AS medioEntrega,
                   t.nombres             AS tecnico
              FROM detalle_orden d
              JOIN servicio_tarifario s    ON s.id_servicio = d.id_servicio
              JOIN categoria_material cat  ON cat.id_categoria = s.id_categoria
              LEFT JOIN mantenimiento_cinta m ON m.id_detalle = d.id_detalle
              LEFT JOIN captura_tecnica ct    ON ct.id_detalle = d.id_detalle
              LEFT JOIN usuario t             ON t.id_usuario = ct.id_tecnico
             WHERE d.id_ot = :idOt
             ORDER BY d.id_detalle
            """, nativeQuery = true)
    List<OrdenDetalleDTO> obtenerDetalle(@Param("idOt") Integer idOt);

    /** Ingresos y unidades por formato, solo de OTs ya cobradas (Finalizado), filtradas por año de cobro. */
    @Query(value = """
            SELECT s.formato                    AS formato,
                   cat.nombre_categoria         AS categoria,
                   COUNT(DISTINCT d.id_ot)      AS ordenes,
                   SUM(d.cantidad_materiales)   AS unidades,
                   SUM(d.subtotal)              AS ingresos
              FROM detalle_orden d
              JOIN orden_trabajo o        ON o.id_ot = d.id_ot
              JOIN servicio_tarifario s   ON s.id_servicio = d.id_servicio
              JOIN categoria_material cat ON cat.id_categoria = s.id_categoria
              JOIN comprobante_pago c     ON c.id_ot = o.id_ot
             WHERE o.estado = 'Finalizado'
               AND YEAR(c.fecha_pago) = :anio
             GROUP BY s.formato, cat.nombre_categoria
             ORDER BY SUM(d.subtotal) DESC
            """, nativeQuery = true)
    List<FormatoIngresoDTO> ingresosPorFormato(@Param("anio") int anio);

    /** Ingresos y unidades por formato, todos los años (sin filtro de año). Conservado para compatibilidad. */
    @Query(value = """
            SELECT s.formato                    AS formato,
                   cat.nombre_categoria         AS categoria,
                   COUNT(DISTINCT d.id_ot)      AS ordenes,
                   SUM(d.cantidad_materiales)   AS unidades,
                   SUM(d.subtotal)              AS ingresos
              FROM detalle_orden d
              JOIN orden_trabajo o        ON o.id_ot = d.id_ot
              JOIN servicio_tarifario s   ON s.id_servicio = d.id_servicio
              JOIN categoria_material cat ON cat.id_categoria = s.id_categoria
             WHERE o.estado = 'Finalizado'
             GROUP BY s.formato, cat.nombre_categoria
             ORDER BY SUM(d.subtotal) DESC
            """, nativeQuery = true)
    List<FormatoIngresoDTO> ingresosPorFormato();

    /** JPQL con record: cuántas líneas (servicios) se pidieron por categoría en OTs no canceladas. */
    @Query("""
            select new com.electronica.suministros.dto.ConteoDTO(c.nombreCategoria, count(d))
              from DetalleOrden d join d.servicio s join s.categoria c join d.ot o
             where o.estado <> 'Cancelado'
             group by c.nombreCategoria
             order by count(d) desc
            """)
    List<ConteoDTO> contarLineasPorCategoria();
}
