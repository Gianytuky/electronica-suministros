package com.electronica.suministros.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento que publica CapturaService.registrar() cuando una OT pasa a "Por Notificar".
 * Solo lleva datos simples (no entidades JPA) porque lo atiende otro hilo, ya sin sesión de base de datos.
 */
public record OrdenPorNotificarEvent(String codigoOt, String cliente, String correoCliente,
                                     BigDecimal monto, List<String> medios) {
}
