package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Record JSON: línea de una OT existente para precargar el formulario en modo edición. */
public record LineaEdicionDTO(Integer idServicio, Integer idCategoria, String formato, String tipoCobro,
                              BigDecimal precio, Integer cantidad, Boolean limpieza, Boolean admiteLimpieza) {
}
