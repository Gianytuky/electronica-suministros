package com.electronica.suministros.dto;

import java.util.List;

/** Record con el cuerpo JSON para registrar o editar una OT: el cliente y sus líneas. */
public record OrdenRequest(Integer idCliente, List<LineaRequest> lineas) {
}
