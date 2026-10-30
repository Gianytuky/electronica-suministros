package com.electronica.suministros.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.electronica.suministros.entity.MensajeContacto;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Integer> {

    List<MensajeContacto> findByFechaEnvioBetweenOrderByFechaEnvioDesc(LocalDateTime desde, LocalDateTime hasta);

    long countByEstado(String estado);
}
