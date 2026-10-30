package com.electronica.suministros.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Formulario validado del mantenimiento del tarifario (alta y edición de servicios). */
public class ServicioFormDTO {

    private Integer idServicio;

    @NotNull(message = "Seleccione la categoría")
    private Integer idCategoria;

    @NotBlank(message = "Ingrese el nombre del formato")
    @Size(max = 50, message = "El formato no puede superar 50 caracteres")
    private String formato;

    @NotBlank(message = "Seleccione el tipo de cobro")
    private String tipoCobro;

    @NotNull(message = "Ingrese el precio unitario")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
    @DecimalMax(value = "9999.99", message = "El precio no puede superar 9999.99")
    private BigDecimal precioUnitario;

    @Size(max = 80, message = "El nombre de la imagen no puede superar 80 caracteres")
    private String imagen;

    private String estado = "ACTIVO";

    public Integer getIdServicio() { return idServicio; }
    public void setIdServicio(Integer idServicio) { this.idServicio = idServicio; }
    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public String getTipoCobro() { return tipoCobro; }
    public void setTipoCobro(String tipoCobro) { this.tipoCobro = tipoCobro; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
