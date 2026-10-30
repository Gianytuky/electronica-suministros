package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Proyección por interfaz del reporte "ingresos por formato" (OTs finalizadas), agrupado con JOIN de 4 tablas. */
public interface FormatoIngresoDTO {

    String getFormato();

    String getCategoria();

    Long getOrdenes();

    Long getUnidades();

    BigDecimal getIngresos();
}
