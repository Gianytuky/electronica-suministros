package com.electronica.suministros.dto;

/**
 * Avance final (integraciones): respuesta JSON de GET /api/reniec/ruc/{ruc} (padrón de SUNAT vía ApiPeru.dev).
 * nombreCliente es la razón social lista para el campo "Nombres o razón social" del cliente (máximo 100 caracteres).
 */
public record RucDTO(String ruc, String razonSocial, String estado, String condicion, String direccion,
                     String departamento, String nombreCliente) {
}
