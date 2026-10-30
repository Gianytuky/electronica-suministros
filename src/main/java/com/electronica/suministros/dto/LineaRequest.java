package com.electronica.suministros.dto;

/** Record que recibe una línea de la OT enviada como JSON desde el formulario (fetch POST/PUT). */
public record LineaRequest(Integer idServicio, Integer cantidad, Boolean limpieza) {
}
