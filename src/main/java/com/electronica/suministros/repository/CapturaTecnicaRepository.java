package com.electronica.suministros.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.electronica.suministros.dto.CapturaRecienteDTO;
import com.electronica.suministros.entity.CapturaTecnica;

public interface CapturaTecnicaRepository extends JpaRepository<CapturaTecnica, Integer> {

    long countByTecnico_IdUsuario(Integer idUsuario);

    @Query("select sum(c.duracionHorasReales) from CapturaTecnica c")
    BigDecimal sumarHoras();

    @Query("select sum(c.pesoGb) from CapturaTecnica c")
    BigDecimal sumarGb();

    /** SQL nativo con JOIN de 5 tablas: las últimas capturas registradas por el laboratorio. */
    @Query(value = """
            SELECT ct.id_captura  AS idCaptura,
                   o.id_ot        AS idOt,
                   o.codigo_ot    AS codigoOt,
                   s.formato      AS formato,
                   ct.duracion_horas_reales AS horas,
                   ct.peso_gb     AS pesoGb,
                   ct.medio_entrega AS medioEntrega,
                   DATE_FORMAT(ct.fecha_captura, '%d/%m/%Y %H:%i') AS fecha,
                   u.nombres      AS tecnico,
                   o.estado       AS estadoOt
              FROM captura_tecnica ct
              JOIN detalle_orden d      ON d.id_detalle = ct.id_detalle
              JOIN orden_trabajo o      ON o.id_ot = d.id_ot
              JOIN servicio_tarifario s ON s.id_servicio = d.id_servicio
              JOIN usuario u            ON u.id_usuario = ct.id_tecnico
             ORDER BY ct.fecha_captura DESC, ct.id_captura DESC
             LIMIT 25
            """, nativeQuery = true)
    List<CapturaRecienteDTO> ultimas();
}
