package com.electronica.suministros.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repositorio personalizado (mismo patrón que VentaRepository de clase): el procedimiento
 * SP_Obtener_Codigo devuelve una tabla que no corresponde a ninguna entidad, por eso
 * se invoca con EntityManager y "CALL".
 *
 * SP_Obtener_Codigo llama internamente a SP_Generar_Codigo, que incrementa el contador de la
 * tabla correlativo y arma el código: 'OT' -> OT-1042, 'SERVICIO' -> ST014, 'COMPROBANTE' -> B001-00000033.
 * Debe llamarse dentro de la transacción de la OT (OrdenService.registrar): así, si la
 * OT falla, el número consumido también se revierte y no quedan saltos en la numeración.
 */
@Repository
public class CorrelativoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public String obtenerCodigo(String tipo) {
        List<Object> filas = entityManager
                .createNativeQuery("CALL SP_Obtener_Codigo(:tipo)")
                .setParameter("tipo", tipo)
                .getResultList();
        if (filas.isEmpty()) {
            throw new IllegalStateException("SP_Obtener_Codigo no devolvió ningún código para " + tipo);
        }
        return String.valueOf(filas.get(0));
    }
}
