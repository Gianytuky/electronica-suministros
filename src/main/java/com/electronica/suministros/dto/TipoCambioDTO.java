package com.electronica.suministros.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Avance final (integraciones): tipo de cambio referencial USD -> PEN obtenido de ApiPeru.dev.
 * Lo usan la cabecera pública (fragments/header.html) y la boleta en PDF (comprobante-pdf.html).
 * Si ApiPeru.dev nunca respondió, contiene el valor de respaldo configurado y respaldo = true,
 * para que la pantalla y la boleta lo rotulen como tal.
 */
public class TipoCambioDTO {

    private final BigDecimal compra;
    private final BigDecimal venta;
    private final String fecha;          // fecha del tipo de cambio (dd/MM/yyyy)
    private final String consultadoEn;   // fecha y hora en que se obtuvo el valor (dd/MM/yyyy HH:mm)
    private final boolean respaldo;

    public TipoCambioDTO(BigDecimal compra, BigDecimal venta, String fecha, String consultadoEn, boolean respaldo) {
        this.compra = compra;
        this.venta = venta;
        this.fecha = fecha;
        this.consultadoEn = consultadoEn;
        this.respaldo = respaldo;
    }

    /** Convierte un monto en soles a dólares con el precio de venta, redondeado a 2 decimales. */
    public BigDecimal aDolares(BigDecimal soles) {
        if (soles == null || venta == null || venta.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return soles.divide(venta, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCompra() { return compra; }
    public BigDecimal getVenta() { return venta; }
    public String getFecha() { return fecha; }
    public String getConsultadoEn() { return consultadoEn; }
    public boolean isRespaldo() { return respaldo; }
}
