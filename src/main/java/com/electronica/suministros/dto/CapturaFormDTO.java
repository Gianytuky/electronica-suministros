package com.electronica.suministros.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Formulario validado del técnico (Bean Validation + BindingResult, como se vio en clase). */
public class CapturaFormDTO {

    @NotNull(message = "Seleccione la línea a capturar")
    private Integer idDetalle;

    @NotNull(message = "Ingrese la duración real en horas")
    @DecimalMin(value = "0.01", message = "La duración debe ser mayor que 0")
    @DecimalMax(value = "999.99", message = "La duración no puede superar 999.99 horas")
    private BigDecimal horasReales;

    @NotNull(message = "Ingrese el peso generado en GB")
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor que 0")
    @DecimalMax(value = "9999.99", message = "El peso no puede superar 9999.99 GB")
    private BigDecimal pesoGb;

    @NotBlank(message = "Seleccione el medio de entrega")
    private String medioEntrega;

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }
    public BigDecimal getHorasReales() { return horasReales; }
    public void setHorasReales(BigDecimal horasReales) { this.horasReales = horasReales; }
    public BigDecimal getPesoGb() { return pesoGb; }
    public void setPesoGb(BigDecimal pesoGb) { this.pesoGb = pesoGb; }
    public String getMedioEntrega() { return medioEntrega; }
    public void setMedioEntrega(String medioEntrega) { this.medioEntrega = medioEntrega; }
}
