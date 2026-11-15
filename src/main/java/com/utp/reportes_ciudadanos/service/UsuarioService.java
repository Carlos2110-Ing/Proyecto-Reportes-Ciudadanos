package com.utp.reportes_ciudadanos.service;

import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.model.Verificacion;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.UsuarioJpaRepository;
import com.utp.reportes_ciudadanos.repository.VerificacionJpaRepository;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioJpaRepository usuarioJpa;
    private final VerificacionJpaRepository verificacionJpa;
    private final JsonRepository jsonRepo;
    private final PasswordEncoder passwordEncoder;

    // @Lazy en PasswordEncoder evita dependencia circular con SecurityConfig
    @Autowired
    public UsuarioService(UsuarioJpaRepository usuarioJpa,
            VerificacionJpaRepository verificacionJpa,
            JsonRepository jsonRepo,
            @Lazy PasswordEncoder passwordEncoder) {
        this.usuarioJpa = usuarioJpa;
        this.verificacionJpa = verificacionJpa;
        this.jsonRepo = jsonRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ── CONSULTAS ──────────────────────────────────────────────────────
    public List<Usuario> findAll() {
        try {
            return usuarioJpa.findAll();
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findAll usuarios. Usando JSON.");
            return jsonRepo.findAllUsuarios();
        }
    }

    public Optional<Usuario> findById(Integer id) {
        try {
            return usuarioJpa.findById(id);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findById usuario {}. Usando JSON.", id);
            return jsonRepo.findUsuarioById(id);
        }
    }

    public boolean existeEmail(String email) {
        try {
            return usuarioJpa.existsByEmail(email);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en existeEmail. Usando JSON.");
            return jsonRepo.existeEmail(email);
        }
    }

    // ── REGISTRO ───────────────────────────────────────────────────────
    @Transactional
    public Usuario registrar(String nombreCompleto, String email, String password) {
        Usuario u = new Usuario();
        u.setNombreCompleto(nombreCompleto);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password)); // ← BCrypt
        u.setUsername(email.split("@")[0]);
        u.setRol("USUARIO");
        u.setAvatarInicial(iniciales(nombreCompleto));
        u.setFechaRegistro(LocalDate.now().toString());

        // ── Escritura en MySQL ──
        try {
            u = usuarioJpa.save(u);
            log.debug("Usuario {} guardado en MySQL con id={}", email, u.getId());
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al registrar. Solo JSON.");
        }

        // ── Escritura en JSON (siempre) ──
        try {
            jsonRepo.saveUsuario(u);
        } catch (IOException e) {
            log.error("Error al guardar usuario {} en archivo JSON: {}", email, e.getMessage());
        }

        // ── Verificación pendiente ──
        Verificacion v = new Verificacion();
        v.setUsuarioId(u.getId());
        v.setEstado(Verificacion.Estado.PENDIENTE);
        guardarVerificacion(v);

        return u;
    }

    // ── ACTUALIZAR PERFIL ──────────────────────────────────────────────
    @Transactional
    public void actualizarPerfil(Integer id, String nombreCompleto, String email) {
        findById(id).ifPresent(u -> {
            u.setNombreCompleto(nombreCompleto);
            u.setEmail(email);
            u.setAvatarInicial(iniciales(nombreCompleto));
            guardarUsuario(u);
        });
    }

    /**
     * Verifica que la contraseña actual sea correcta (comparación BCrypt).
     */
    public boolean passwordCorrecta(Integer id, String passwordPlano) {
        return findById(id)
                .map(u -> passwordEncoder.matches(passwordPlano, u.getPassword()))
                .orElse(false);
    }

    @Transactional
    public void cambiarRol(Integer id, String nuevoRol) {
        findById(id).ifPresent(u -> {
            u.setRol(nuevoRol.toUpperCase().trim());
            guardarUsuario(u);
        });
    }

    @Transactional
    public void cambiarPassword(Integer id, String nuevaPassword) {
        findById(id).ifPresent(u -> {
            u.setPassword(passwordEncoder.encode(nuevaPassword)); // ← BCrypt
            guardarUsuario(u);
        });
    }

    // ── VERIFICACIÓN ───────────────────────────────────────────────────
    public boolean isVerificado(Integer usuarioId) {
        try {
            return verificacionJpa.existsByUsuarioIdAndEstado(
                    usuarioId, Verificacion.Estado.VERIFICADO);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en isVerificado. Usando JSON.");
            return jsonRepo.findVerificacionByUsuarioId(usuarioId)
                    .map(Verificacion::isVerificado).orElse(false);
        }
    }

    public Optional<Verificacion> findVerificacionByUsuario(Integer usuarioId) {
        try {
            return verificacionJpa.findByUsuarioId(usuarioId);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findVerificacion. Usando JSON.");
            return jsonRepo.findVerificacionByUsuarioId(usuarioId);
        }
    }

    public List<Verificacion> findAllVerificaciones() {
        try {
            return verificacionJpa.findAll();
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findAllVerificaciones. Usando JSON.");
            return jsonRepo.findAllVerificaciones();
        }
    }

    @Transactional
    public void verificarUsuario(Integer usuarioId, Integer adminId) {
        findVerificacionByUsuario(usuarioId).ifPresent(v -> {
            v.setEstado(Verificacion.Estado.VERIFICADO);
            v.setFechaVerificacion(LocalDate.now().toString());
            v.setAdminId(adminId);
            guardarVerificacion(v);
        });
    }

    @Transactional
    public void revocarVerificacion(Integer usuarioId) {
        findVerificacionByUsuario(usuarioId).ifPresent(v -> {
            v.setEstado(Verificacion.Estado.PENDIENTE);
            v.setFechaVerificacion(null);
            v.setAdminId(null);
            guardarVerificacion(v);
        });
    }

    // ── ESCRITURA DUPLICADA (helpers internos) ─────────────────────────
    private void guardarUsuario(Usuario u) {
        try {
            usuarioJpa.save(u);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al guardar usuario. Solo JSON.");
        }

        try {
            jsonRepo.saveUsuario(u);
        } catch (IOException e) {
            log.error("Error al guardar usuario {} en JSON: {}", u.getEmail(), e.getMessage());
        }
    }

    private void guardarVerificacion(Verificacion v) {
        try {
            verificacionJpa.save(v);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al guardar verificacion. Solo JSON.");
        }
        try {
            jsonRepo.saveVerificacion(v);
        } catch (IOException e) {
            log.error("Error al guardar verificacion {} en archivo JSON: {}", v.getId(), e.getMessage());
        }
    }

    public boolean existeEmailEnOtroUsuario(String email, Integer usuarioId) {
        return usuarioJpa.existsByEmailIgnoreCaseAndIdNot(email.trim(), usuarioId);
    }

    // ── HELPERS ────────────────────────────────────────────────────────
    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "??";
        }
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
    }
}
