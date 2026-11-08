package com.utp.reportes_ciudadanos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utp.reportes_ciudadanos.model.Permiso;

@Repository
public interface PermisoJpaRepository extends JpaRepository<Permiso, Integer> {
    
    boolean existsByUrlPatternIgnoreCaseAndHttpMethodIgnoreCase(
            String urlPattern,
            String httpMethod
    );
}

