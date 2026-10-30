package com.electronica.suministros.dto;

/** Record (Java 21): par etiqueta/cantidad para gráficos. Se arma con JPQL "select new" y se serializa a JSON. */
public record ConteoDTO(String etiqueta, Long cantidad) {
}
