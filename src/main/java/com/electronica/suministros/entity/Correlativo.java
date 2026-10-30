package com.electronica.suministros.entity;

import jakarta.persistence.*;

/** Contador de códigos. Lo usa el procedimiento SP_Generar_Codigo (OT-1042, ST014, B001-00000033). */
@Entity
@Table(name = "correlativo")
public class Correlativo {

    @Id
    @Column(name = "tipo")
    private String tipo;

    @Column(name = "prefijo")
    private String prefijo;

    @Column(name = "separador")
    private String separador;

    @Column(name = "longitud")
    private Integer longitud;

    @Column(name = "ultimo_numero")
    private Integer ultimoNumero;

    // get y set
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getPrefijo() { return prefijo; }
    public void setPrefijo(String prefijo) { this.prefijo = prefijo; }
    public String getSeparador() { return separador; }
    public void setSeparador(String separador) { this.separador = separador; }
    public Integer getLongitud() { return longitud; }
    public void setLongitud(Integer longitud) { this.longitud = longitud; }
    public Integer getUltimoNumero() { return ultimoNumero; }
    public void setUltimoNumero(Integer ultimoNumero) { this.ultimoNumero = ultimoNumero; }
}
