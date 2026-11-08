package com.utp.reportes_ciudadanos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utp.reportes_ciudadanos.model.Usuario;

@Repository
public interface UsuarioJpaRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
    
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}

