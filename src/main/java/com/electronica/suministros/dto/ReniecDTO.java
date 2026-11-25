package com.electronica.suministros.dto;

/**
 * Avance final (integraciones): respuesta JSON de GET /api/reniec/{dni}.
 * nombreCliente es el texto listo para el campo "Nombres o razón social" del cliente
 * (nombres y apellidos en orden natural, con mayúscula inicial y como máximo 100 caracteres).
 */
public record ReniecDTO(String dni, String nombres, String apellidoPaterno, String apellidoMaterno,
                        String nombreCompleto, String nombreCliente) {
}
