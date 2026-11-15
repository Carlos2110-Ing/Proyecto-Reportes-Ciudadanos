package com.utp.reportes_ciudadanos.config;

import com.utp.reportes_ciudadanos.model.*;
import com.utp.reportes_ciudadanos.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermisoJpaRepository permisoJpa;
    private final RolJpaRepository rolJpa;
    private final CategoriaJpaRepository categoriaJpa;
    private final UsuarioJpaRepository usuarioJpa;
    private final VerificacionJpaRepository verificacionJpa;
    private final JsonRepository jsonRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(PermisoJpaRepository permisoJpa,
            RolJpaRepository rolJpa,
            CategoriaJpaRepository categoriaJpa,
            UsuarioJpaRepository usuarioJpa,
            VerificacionJpaRepository verificacionJpa,
            JsonRepository jsonRepo,
            @Lazy PasswordEncoder passwordEncoder) {
        this.permisoJpa = permisoJpa;
        this.rolJpa = rolJpa;
        this.categoriaJpa = categoriaJpa;
        this.usuarioJpa = usuarioJpa;
        this.verificacionJpa = verificacionJpa;
        this.jsonRepo = jsonRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("=== DataInitializer: verificando datos base ===");
        try {
            inicializarPermisos();
            inicializarRoles();
            inicializarCategorias();
            inicializarAdmin();
            log.info("=== DataInitializer: completado ===");
        } catch (Exception e) {
            log.error("Error en DataInitializer: {}", e.getMessage());
        }
    }

    // ── PERMISOS ───────────────────────────────────────────────────────

    private void inicializarPermisos() {
        if (permisoJpa.count() > 0) {
            log.info("Permisos ya existen, omitiendo.");
            return;
        }

        List<Permiso> permisos = List.of(
                new Permiso("/dashboard", "Ver dashboard", "*"),
                new Permiso("/reportes/nuevo", "Crear nuevo reporte", "*"),
                new Permiso("/reportes/mis-reportes", "Ver mis reportes", "*"),
                new Permiso("/reportes/editar/**", "Editar reporte propio", "POST"),
                new Permiso("/reportes/eliminar/**", "Eliminar reporte propio", "POST"),
                new Permiso("/perfil", "Ver perfil", "*"),
                new Permiso("/perfil/actualizar", "Actualizar datos de perfil", "POST"),
                new Permiso("/perfil/cambiar-password", "Cambiar contraseña", "POST"),
                new Permiso("/acerca-de", "Ver acerca de", "*"),
                new Permiso("/admin/**", "Acceso total al panel admin", "*"));

        permisoJpa.saveAll(permisos);
        for (Permiso p : permisos) {
            try {
                jsonRepo.savePermiso(p);
            } catch (IOException e) {
                log.error("Error al guardar permiso {} en JSON: {}", p.getUrlPattern(), e.getMessage());
            }
        }
        log.info("Permisos base creados: {}", permisos.size());
    }

    // ── ROLES ──────────────────────────────────────────────────────────

    private void inicializarRoles() {
        if (rolJpa.count() > 0) {
            log.info("Roles ya existen, omitiendo.");
            return;
        }

        // IDs de permisos (asumiendo que se insertaron en orden desde 1)
        // USUARIO: todo excepto admin
        Rol usuario = new Rol("USUARIO", "Ciudadano registrado que puede reportar incidentes");
        usuario.setPermisoIds(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9)); // sin permiso 10 (admin)
        rolJpa.save(usuario);

        // VISUALIZADOR: solo puede ver, sin crear ni editar
        Rol visualizador = new Rol("VISUALIZADOR", "Solo puede ver reportes y el dashboard");
        visualizador.setPermisoIds(List.of(1, 3, 9)); // dashboard, mis-reportes, acerca-de
        rolJpa.save(visualizador);

        // ADMIN: acceso total
        Rol admin = new Rol("ADMIN", "Administrador con acceso total al sistema");
        admin.setPermisoIds(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        rolJpa.save(admin);

        try {
            jsonRepo.saveRol(usuario);
            jsonRepo.saveRol(visualizador);
            jsonRepo.saveRol(admin);
        } catch (IOException e) {
            log.error("Error al guardar roles base en JSON: {}", e.getMessage());
        }

        log.info("Roles base creados: USUARIO, VISUALIZADOR, ADMIN");
    }

    // ── CATEGORÍAS ─────────────────────────────────────────────────────

    private void inicializarCategorias() {
        if (categoriaJpa.count() > 0) {
            log.info("Categorías ya existen, omitiendo.");
            return;
        }

        List<Categoria> cats = List.of(
                crearCategoria("Emergencia", "bi bi-exclamation-triangle-fill", "#ef4444"),
                crearCategoria("Infraestructura", "bi bi-building", "#3b82f6"),
                crearCategoria("Seguridad", "bi bi-shield-exclamation", "#f57c00"),
                crearCategoria("Medio Ambiente", "bi bi-tree-fill", "#16a34a"),
                crearCategoria("Tránsito", "bi bi-car-front", "#6b7280"),
                crearCategoria("Otro", "bi bi-three-dots", "#94a3b8"));

        categoriaJpa.saveAll(cats);
        for (Categoria c : cats) {
            try {
                jsonRepo.saveCategoria(c);
            } catch (IOException e) {
                log.error("Error al guardar categoría {} en JSON: {}", c.getNombre(), e.getMessage());
            }
        }
        log.info("Categorías base creadas: {}", cats.size());
    }

    private Categoria crearCategoria(String nombre, String icono, String color) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setIcono(icono);
        c.setColor(color);
        return c;
    }

    // ── ADMIN ──────────────────────────────────────────────────────────

    private void inicializarAdmin() {
        if (usuarioJpa.existsByEmail("admin@safeperu.pe")) {
            log.info("Admin ya existe, omitiendo.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setEmail("admin@safeperu.pe");
        admin.setPassword(passwordEncoder.encode("admin2026"));
        admin.setNombreCompleto("Administrador SafePeru");
        admin.setRol("ADMIN");
        admin.setAvatarInicial("AD");
        admin.setFechaRegistro(LocalDate.now().toString());

        admin = usuarioJpa.save(admin);

        // Verificación automática del admin
        Verificacion v = new Verificacion();
        v.setUsuarioId(admin.getId());
        v.setEstado(Verificacion.Estado.VERIFICADO);
        v.setFechaVerificacion(LocalDate.now().toString());
        v.setAdminId(admin.getId());

        verificacionJpa.save(v);

        try {
            jsonRepo.saveUsuario(admin);
            jsonRepo.saveVerificacion(v);
        } catch (IOException e) {
            log.error("Error al guardar admin o su verificación en JSON: {}", e.getMessage());
        }

        log.info("Admin creado con email: admin@safeperu.pe");
    }
}
