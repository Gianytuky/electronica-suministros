package com.electronica.suministros.service;

/**
 * Error de regla de negocio (DNI duplicado, OT que ya no se puede editar, etc.).
 * Su mensaje es seguro de mostrar al usuario en una alerta.
 */
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
