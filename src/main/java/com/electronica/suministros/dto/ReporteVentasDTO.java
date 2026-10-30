package com.electronica.suministros.dto;

import java.math.BigDecimal;

/**
 * DTO de clase para el resultado del procedimiento SP_Reporte_Ventas (una fila por mes).
 * No es entidad ni proyección: el procedimiento devuelve una tabla propia y se mapea a mano.
 */
public class ReporteVentasDTO {

    private Integer mes;
    private Long cantidadOrdenes;
    private BigDecimal totalIngresos;

    public ReporteVentasDTO() {
    }

    public ReporteVentasDTO(Integer mes, Long cantidadOrdenes, BigDecimal totalIngresos) {
        this.mes = mes;
        this.cantidadOrdenes = cantidadOrdenes;
        this.totalIngresos = totalIngresos;
    }

    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public Long getCantidadOrdenes() { return cantidadOrdenes; }
    public void setCantidadOrdenes(Long cantidadOrdenes) { this.cantidadOrdenes = cantidadOrdenes; }
    public BigDecimal getTotalIngresos() { return totalIngresos; }
    public void setTotalIngresos(BigDecimal totalIngresos) { this.totalIngresos = totalIngresos; }
}
