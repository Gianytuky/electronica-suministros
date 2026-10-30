package com.electronica.suministros.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.electronica.suministros.dto.ConteoDTO;
import com.electronica.suministros.dto.OrdenListadoDTO;
import com.electronica.suministros.entity.OrdenTrabajo;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Integer> {

    long countByEjecutivo_IdUsuario(Integer idUsuario);

    long countByCliente_IdCliente(Integer idCliente);

    List<OrdenTrabajo> findByCliente_IdClienteOrderByFechaRecepcionDesc(Integer idCliente);

    /**
     * SQL nativo con JOIN (orden_trabajo + cliente + usuario + comprobante_pago) y subconsultas.
     * Los filtros llegan vacíos ('') cuando no se usan, para no depender de parámetros NULL.
     */
    @Query(value = """
            SELECT o.id_ot            AS idOt,
                   o.codigo_ot        AS codigoOt,
                   c.nombres          AS cliente,
                   c.dni_cliente      AS dniCliente,
                   u.nombres          AS ejecutivo,
                   DATE_FORMAT(o.fecha_recepcion, '%d/%m/%Y %H:%i') AS fechaRecepcion,
                   o.estado           AS estado,
                   o.monto_estimado   AS montoEstimado,
                   (SELECT GROUP_CONCAT(s.formato ORDER BY d.id_detalle SEPARATOR ', ')
                      FROM detalle_orden d
                      JOIN servicio_tarifario s ON s.id_servicio = d.id_servicio
                     WHERE d.id_ot = o.id_ot) AS formatos,
                   (SELECT COUNT(*) FROM detalle_orden d WHERE d.id_ot = o.id_ot) AS lineas,
                   (SELECT COUNT(*) FROM captura_tecnica t
                      JOIN detalle_orden d ON d.id_detalle = t.id_detalle
                     WHERE d.id_ot = o.id_ot) AS capturadas,
                   cp.numero_comprobante AS numeroComprobante
              FROM orden_trabajo o
              JOIN cliente c ON c.id_cliente = o.id_cliente
              JOIN usuario u ON u.id_usuario = o.id_ejecutivo
              LEFT JOIN comprobante_pago cp ON cp.id_ot = o.id_ot
             WHERE (:estado = '' OR o.estado = :estado)
               AND (:texto = '' OR c.nombres LIKE CONCAT('%', :texto, '%')
                                OR c.dni_cliente LIKE CONCAT('%', :texto, '%')
                                OR o.codigo_ot LIKE CONCAT('%', :texto, '%'))
             ORDER BY o.fecha_recepcion DESC, o.id_ot DESC
            """, nativeQuery = true)
    List<OrdenListadoDTO> listar(@Param("estado") String estado, @Param("texto") String texto);

    /** Cobro y entrega: SP_Liquidar_Orden emite la boleta y pasa la OT a Finalizado (todo en una transacción). */
    @Procedure(procedureName = "SP_Liquidar_Orden")
    void liquidarOrden(@Param("p_id_ot") Integer idOt);

    /** JPQL con record: cantidad de OTs por estado (para el gráfico de dona). */
    @Query("select new com.electronica.suministros.dto.ConteoDTO(o.estado, count(o)) from OrdenTrabajo o group by o.estado order by count(o) desc")
    List<ConteoDTO> contarPorEstado();

    long countByEstado(String estado);

    @Query("select sum(o.montoEstimado) from OrdenTrabajo o where o.estado = :estado")
    BigDecimal sumarMontoPorEstado(@Param("estado") String estado);
}
