package com.electronica.suministros.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.electronica.suministros.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombreRol(String nombreRol);
}
