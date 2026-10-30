package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Record JSON: una OT de un cliente (segundo nivel de la consulta en cascada). */
public record OrdenResumenDTO(Integer idOt, String codigoOt, String fecha, String estado, BigDecimal monto, String formatos) {
}
