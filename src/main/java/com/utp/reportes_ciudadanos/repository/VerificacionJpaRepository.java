package com.utp.reportes_ciudadanos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utp.reportes_ciudadanos.model.Verificacion;

@Repository
public interface VerificacionJpaRepository extends JpaRepository<Verificacion, Integer> {
    Optional<Verificacion> findByUsuarioId(Integer usuarioId);
    boolean existsByUsuarioIdAndEstado(Integer usuarioId, Verificacion.Estado estado);
}


