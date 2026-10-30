package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Proyección por interfaz del tarifario: servicio_tarifario + categoria_material (+ subconsulta de usos en OTs). */
public interface ServicioListadoDTO {

    Integer getIdServicio();

    String getCodigo();

    String getFormato();

    Integer getIdCategoria();

    String getCategoria();

    String getTipoCobro();

    BigDecimal getPrecioUnitario();

    String getImagen();

    String getEstado();

    Long getUsos();
}
