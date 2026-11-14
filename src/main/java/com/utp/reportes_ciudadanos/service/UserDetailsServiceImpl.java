package com.utp.reportes_ciudadanos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.UsuarioJpaRepository;

@Service 
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    private final UsuarioJpaRepository usuarioJpa;
    private final JsonRepository jsonRepo;

    @Autowired 
    public UserDetailsServiceImpl(UsuarioJpaRepository usuarioJpa, JsonRepository jsonRepo){
        this.usuarioJpa = usuarioJpa;
        this.jsonRepo = jsonRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario;

        try {
            usuario = usuarioJpa.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuario no encontrado con email: " + email));
            log.debug("Usuario '{}' cargado desde MySQL.", email);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en loadUserByUsername. Usando JSON.");
            usuario = jsonRepo.findAllUsuarios().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst()
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuario no encontrado con email: " + email));
        }

        return usuario;
    }

    
}
