package com.electronica.suministros.dto;

import java.math.BigDecimal;

/** Record JSON que usan los combos en cascada (Categoría -> Formato) del catálogo público y del formulario de OT. */
public record ServicioCatalogoDTO(Integer idServicio, String codigo, String formato, String tipoCobro,
                                  BigDecimal precio, String imagen, Boolean esVideo, Boolean admiteLimpieza) {
}
