package com.utp.reportes_ciudadanos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utp.reportes_ciudadanos.model.Reporte;

@Repository
public interface ReporteJpaRepository extends JpaRepository<Reporte, Integer> {
    List<Reporte> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    List<Reporte> findAllByOrderByFechaCreacionDesc();
    long countByUsuarioId(Integer usuarioId);
    long countByEstado(Reporte.Estado estado);
    long countByUsuarioIdAndEstado(Integer usuarioId, Reporte.Estado estado);
}

