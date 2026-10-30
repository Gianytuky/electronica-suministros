package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Record de respuesta JSON después de guardar una OT. */
public record OrdenGuardadaDTO(Integer idOt, String codigoOt, BigDecimal monto, String mensaje) {
}
