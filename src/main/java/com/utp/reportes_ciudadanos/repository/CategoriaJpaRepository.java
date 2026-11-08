package com.utp.reportes_ciudadanos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utp.reportes_ciudadanos.model.Categoria;

@Repository
public interface CategoriaJpaRepository extends JpaRepository<Categoria, Integer> {
    
}

