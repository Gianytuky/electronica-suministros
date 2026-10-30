package com.electronica.suministros.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.electronica.suministros.dto.ReporteVentasDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repositorio personalizado (mismo patrón que el ejemplo "spmes" de clase): SP_Reporte_Ventas devuelve una
 * tabla propia (mes, cantidad_ordenes, total_ingresos) que no es una entidad, así que se llama con CALL
 * y cada fila se convierte a ReporteVentasDTO.
 */
@Repository
public class ReporteVentasRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ReporteVentasDTO> ventasPorMes(int anio) {
        List<Object[]> filas = entityManager
                .createNativeQuery("CALL SP_Reporte_Ventas(:anio)")
                .setParameter("anio", anio)
                .getResultList();

        List<ReporteVentasDTO> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            resultado.add(new ReporteVentasDTO(
                    ((Number) fila[0]).intValue(),
                    ((Number) fila[1]).longValue(),
                    new BigDecimal(String.valueOf(fila[2]))));
        }
        return resultado;
    }
}
