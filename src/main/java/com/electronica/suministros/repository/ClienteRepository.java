package com.electronica.suministros.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.electronica.suministros.dto.ClienteResumenDTO;
import com.electronica.suministros.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    List<Cliente> findAllByOrderByNombresAsc();

    boolean existsByDniCliente(String dniCliente);

    boolean existsByDniClienteAndIdClienteNot(String dniCliente, Integer idCliente);

    /**
     * Consulta JPQL con constructor DTO ("select new"): clientes con OTs vigentes, cuántas y cuánto suman.
     * Une dos entidades (OrdenTrabajo y Cliente) y agrupa, sin escribir SQL.
     */
    @Query("""
            select new com.electronica.suministros.dto.ClienteResumenDTO(
                   c.idCliente, c.nombres, c.dniCliente, count(o), sum(o.montoEstimado))
              from OrdenTrabajo o join o.cliente c
             where o.estado <> 'Cancelado'
             group by c.idCliente, c.nombres, c.dniCliente
             order by c.nombres
            """)
    List<ClienteResumenDTO> resumenConOrdenes();
}
