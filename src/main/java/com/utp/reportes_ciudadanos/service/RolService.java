package com.utp.reportes_ciudadanos.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.utp.reportes_ciudadanos.model.Permiso;
import com.utp.reportes_ciudadanos.model.Rol;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.PermisoJpaRepository;
import com.utp.reportes_ciudadanos.repository.RolJpaRepository;

@Service
public class RolService {
    private static final Logger log = LoggerFactory.getLogger(RolService.class);

    private final RolJpaRepository rolJpa;
    private final PermisoJpaRepository permisoJpa;
    private final JsonRepository jsonRepo;

    @Autowired
    public RolService(RolJpaRepository rolJpa, PermisoJpaRepository permisoJpa, JsonRepository jsonRepo) {
        this.rolJpa = rolJpa;
        this.permisoJpa = permisoJpa;
        this.jsonRepo = jsonRepo;
    }

    public List<Rol> findAllRoles() {
        try {
            return rolJpa.findAll();
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findAllRoles. Usando JSON.");
            return jsonRepo.findAllRoles();
        }
    }

    public Optional<Rol> findRolById(Integer id) {
        try {
            return rolJpa.findById(id);
        } catch (Exception e) {
            log.warn("[FAILOVER] findRolById desde JSON.");
            return jsonRepo.findRolById(id);
        }
    }

    public Optional<Rol> findRolByNombre(String nombre) {
        try {
            return rolJpa.findByNombre(nombre);
        } catch (Exception e) {
            log.warn("[FAILOVER] findRolByNombre desde JSON.");
            return jsonRepo.findRolByNombre(nombre);
        }
    }

    public boolean existeRol(String nombre) {
        try {
            return rolJpa.existsByNombre(nombre);
        } catch (Exception e) {
            return jsonRepo.findRolByNombre(nombre).isPresent();
        }
    }

    // ROLES -ESCRITURA
    public Rol crearRol(String nombre, String descripcion) {
        Rol rol = new Rol(nombre.toUpperCase().trim(), descripcion);

        try {
            rol = rolJpa.save(rol);
            log.debug("Rol '{}' guardado en MySQL con id={}", nombre, rol.getId());
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al crear rol. Solo JSON.");
        }
        try {//AGREGADO PARA MANEJAR LA EXCEPCION DEL JSONREPO
             jsonRepo.saveRol(rol);
        } catch (IOException e){
            log.error("Error al guardar rol en Json: {}", e.getMessage());
        }
        return rol;
    }

    public void actualizarRol(Integer id, String nombre, String descripcion) {
        findRolById(id).ifPresent(rol -> {
            rol.setNombre(nombre.toUpperCase().trim());
            rol.setDescripcion(descripcion);
            guardarRol(rol);
        });
    }

    public void asignarPermisos(Integer rolId, List<Integer> permisoIds) {
        findRolById(rolId).ifPresent(rol -> {
            rol.setPermisoIds(permisoIds);
            guardarRol(rol);
        });
    }

    public boolean eliminarRol(Integer id) {
        boolean eliminado = false;
        try {
            if (rolJpa.existsById(id)) {
                rolJpa.deleteById(id);
                eliminado = true;
            }
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al eliminar rol. Solo JSON.");
        }
        try { //AGREGADO POR EL MISMO ERROR
            return jsonRepo.deleteRol(id) || eliminado;
        } catch (IOException e){
            log.error("Error al eliminar rol en json: {}", e.getMessage());
            return eliminado;
        }
        
    }

    // PERMISOS - CONSULTAS
    public List<Permiso> findAllPermisos() {
        try {
            return permisoJpa.findAll();
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findAllPermisos. Usando JSON.");
            return jsonRepo.findAllPermisos();
        }
    }

    public Optional<Permiso> findPermisoById(Integer id) {
        try {
            return permisoJpa.findById(id);
        } catch (DataAccessException e) {
            return jsonRepo.findPermisoById(id);
        }
    }

    // PERMISOS - ESCRITURA
    public Permiso crearPermiso(String urlPattern, String descripcion, String httpMethod) {
        Permiso permiso = new Permiso(urlPattern, descripcion, httpMethod);
        try {
            permiso = permisoJpa.save(permiso);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al crear permiso. Solo JSON.");
        }
        try {
            jsonRepo.savePermiso(permiso);
        } catch (IOException e) {
            log.error("Error al guardar permiso en JSON: {}", e.getMessage());
        }
        return permiso;
    }

    public boolean eliminarPermiso(Integer id) {
        boolean eliminado = false;
        try {
            if (permisoJpa.existsById(id)) {
                permisoJpa.deleteById(id);
                eliminado = true;
            }
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al eliminar permiso. Solo JSON.");
        }
        try {
            return jsonRepo.deletePermiso(id) || eliminado;
        } catch (IOException e) {
            log.error("Error al eliminar permiso en JSON: {}", e.getMessage());
            return eliminado;
        }
    }

    public boolean existePermiso(String urlPattern, String httpMethod) {
        String metodo = httpMethod == null ? "*" : httpMethod.trim().toUpperCase();
        return permisoJpa.existsByUrlPatternIgnoreCaseAndHttpMethodIgnoreCase(
                urlPattern.trim(), metodo);
    }

    public List<Permiso> findPermisosByRolNombre(String rolNombre) {
        try {
            return findRolByNombre(rolNombre)
                    .map(rol -> rol.getPermisoIds().stream()
                            .map(this::findPermisoById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList())
                    .orElse(List.of());

        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findPermisosByRolNombre. Usando JSON.");
            return jsonRepo.findPermisosByRolNombre(rolNombre);
        }
    }

    // HELPER PRIVADO
    private void guardarRol(Rol rol) {
        try {
            rolJpa.save(rol);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al guardar rol. Solo JSON.");
        }
        try {
            jsonRepo.saveRol(rol);
        } catch (IOException e) {
            log.error("Error al guardar rol en JSON: {}", e.getMessage());
        }
    }

}
