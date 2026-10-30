package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Proyección por interfaz del historial de capturas: captura_tecnica + detalle_orden + orden_trabajo + servicio_tarifario + usuario. */
public interface CapturaRecienteDTO {

    Integer getIdCaptura();

    Integer getIdOt();

    String getCodigoOt();

    String getFormato();

    BigDecimal getHoras();

    BigDecimal getPesoGb();

    String getMedioEntrega();

    String getFecha();

    String getTecnico();

    String getEstadoOt();
}
