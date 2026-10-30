package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Boleta emitida al cobrar y entregar la OT. La inserta el procedimiento SP_Liquidar_Orden. */
@Entity
@Table(name = "comprobante_pago")
public class ComprobantePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Integer idComprobante;

    @OneToOne
    @JoinColumn(name = "id_ot")
    private OrdenTrabajo ot;

    @Column(name = "tipo_comprobante")
    private String tipoComprobante;

    @Column(name = "numero_comprobante")
    private String numeroComprobante;

    @Column(name = "monto_final")
    private BigDecimal montoFinal;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    // get y set
    public Integer getIdComprobante() { return idComprobante; }
    public void setIdComprobante(Integer idComprobante) { this.idComprobante = idComprobante; }
    public OrdenTrabajo getOt() { return ot; }
    public void setOt(OrdenTrabajo ot) { this.ot = ot; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
    public BigDecimal getMontoFinal() { return montoFinal; }
    public void setMontoFinal(BigDecimal montoFinal) { this.montoFinal = montoFinal; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
