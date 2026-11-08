package com.utp.reportes_ciudadanos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utp.reportes_ciudadanos.model.Rol;

@Repository
public interface RolJpaRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}

