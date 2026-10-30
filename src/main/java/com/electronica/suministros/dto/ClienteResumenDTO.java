package com.electronica.suministros.dto;

import java.math.BigDecimal;

/**
 * DTO de clase que se construye desde JPQL con "select new ..." (otra técnica vista en clase).
 * Resume cuántas OTs vigentes (no canceladas) tiene cada cliente y cuánto suman.
 * Alimenta el primer combo de la consulta en cascada (Cliente -> OT -> Detalle).
 */
public class ClienteResumenDTO {

    private Integer idCliente;
    private String nombres;
    private String dniCliente;
    private Long cantidadOrdenes;
    private BigDecimal montoTotal;

    public ClienteResumenDTO(Integer idCliente, String nombres, String dniCliente, Long cantidadOrdenes, BigDecimal montoTotal) {
        this.idCliente = idCliente;
        this.nombres = nombres;
        this.dniCliente = dniCliente;
        this.cantidadOrdenes = cantidadOrdenes;
        this.montoTotal = montoTotal;
    }

    public Integer getIdCliente() { return idCliente; }
    public String getNombres() { return nombres; }
    public String getDniCliente() { return dniCliente; }
    public Long getCantidadOrdenes() { return cantidadOrdenes; }
    public BigDecimal getMontoTotal() { return montoTotal; }
}
