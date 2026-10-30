package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Record JSON: línea de una OT para el tercer nivel de la consulta en cascada (Cliente -> OT -> Detalle). */
public record LineaDetalleDTO(String formato, String categoria, Integer cantidad, BigDecimal precioUnitario,
                              Boolean limpieza, BigDecimal horasReales, BigDecimal pesoGb, String medioEntrega,
                              String tecnico, BigDecimal subtotal) {
}
