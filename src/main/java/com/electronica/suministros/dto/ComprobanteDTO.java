package com.electronica.suministros.dto;

import java.math.BigDecimal;
import java.util.List;

/** Datos que se pasan a la plantilla pdf/comprobante-pdf.html (boleta de venta electrónica). */
public class ComprobanteDTO {

    private String numero;
    private String fechaEmision;
    private String codigoOt;
    private String cliente;
    private String documento;
    private String ejecutivo;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    private List<OrdenDetalleDTO> lineas;
    // Avance final (integraciones): monto referencial en dólares con el tipo de cambio de ApiPeru.dev
    private BigDecimal montoDolares;
    private BigDecimal tipoCambio;
    private String tipoCambioNota;
    // Avance final: correo del cliente para enviarle la boleta (no se imprime en el PDF)
    private String correoCliente;

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getCodigoOt() { return codigoOt; }
    public void setCodigoOt(String codigoOt) { this.codigoOt = codigoOt; }
    public String getCorreoCliente() { return correoCliente; }
    public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getEjecutivo() { return ejecutivo; }
    public void setEjecutivo(String ejecutivo) { this.ejecutivo = ejecutivo; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<OrdenDetalleDTO> getLineas() { return lineas; }
    public void setLineas(List<OrdenDetalleDTO> lineas) { this.lineas = lineas; }
    public BigDecimal getMontoDolares() { return montoDolares; }
    public void setMontoDolares(BigDecimal montoDolares) { this.montoDolares = montoDolares; }
    public BigDecimal getTipoCambio() { return tipoCambio; }
    public void setTipoCambio(BigDecimal tipoCambio) { this.tipoCambio = tipoCambio; }
    public String getTipoCambioNota() { return tipoCambioNota; }
    public void setTipoCambioNota(String tipoCambioNota) { this.tipoCambioNota = tipoCambioNota; }
}
