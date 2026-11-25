package com.electronica.suministros.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.electronica.suministros.entity.ComprobantePago;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Integer> {

    Optional<ComprobantePago> findByOt_IdOt(Integer idOt);

    @Query("select sum(c.montoFinal) from ComprobantePago c")
    BigDecimal sumarIngresos();

    /** Avance final: años que tienen boletas emitidas (selector de año de Reportes). */
    @Query(value = "SELECT DISTINCT YEAR(fecha_pago) FROM comprobante_pago ORDER BY 1 DESC", nativeQuery = true)
    List<Number> aniosConIngresos();
}
