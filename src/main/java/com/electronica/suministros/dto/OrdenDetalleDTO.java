package com.electronica.suministros.dto;

import java.math.BigDecimal;

/**
 * Proyección por interfaz de cada línea de una OT. JOIN de 6 tablas:
 * detalle_orden + servicio_tarifario + categoria_material + mantenimiento_cinta + captura_tecnica + usuario (técnico).
 * Los campos de captura son null mientras el laboratorio no procese esa línea.
 */
public interface OrdenDetalleDTO {

    Integer getIdDetalle();

    String getFormato();

    String getCategoria();

    String getTipoCobro();

    String getImagen();

    Integer getCantidad();

    BigDecimal getPrecioUnitario();

    BigDecimal getSubtotal();

    Integer getLimpieza();

    BigDecimal getHorasReales();

    BigDecimal getPesoGb();

    String getMedioEntrega();

    String getTecnico();
}
