package com.electronica.suministros.dto;

import java.math.BigDecimal;

/**
 * DTO de PROYECCIÓN POR INTERFAZ (técnica de clase) para listados de OTs.
 * Lo llena una consulta SQL nativa con JOIN de 4 tablas: orden_trabajo + cliente + usuario + comprobante_pago,
 * más subconsultas con los formatos de cada OT (GROUP_CONCAT) y el avance de captura técnica.
 * Cada getXxx() debe coincidir con el alias "AS xxx" de la consulta.
 */
public interface OrdenListadoDTO {

    Integer getIdOt();

    String getCodigoOt();

    String getCliente();

    String getDniCliente();

    String getEjecutivo();

    String getFechaRecepcion();

    String getEstado();

    BigDecimal getMontoEstimado();

    String getFormatos();

    Long getLineas();

    Long getCapturadas();

    String getNumeroComprobante();
}
