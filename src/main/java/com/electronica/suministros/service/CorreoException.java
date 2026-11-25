package com.electronica.suministros.service;

/**
 * Fallo al enviar un correo (SMTP caído, credenciales incorrectas, correo sin configurar...).
 * Es una excepción propia para que los servicios decidan qué hacer: la captura técnica la ignora
 * (solo la registra en el log) y la bandeja de consultas la convierte en un mensaje para el usuario.
 */
public class CorreoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CorreoException(String mensaje) {
        super(mensaje);
    }

    public CorreoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
