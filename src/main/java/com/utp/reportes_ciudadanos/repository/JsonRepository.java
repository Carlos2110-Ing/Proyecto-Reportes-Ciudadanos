package com.utp.reportes_ciudadanos.repository;
import com.utp.reportes_ciudadanos.model.*;

import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JsonRepository {

    private final ObjectMapper objectMapper;
    private final Database database;
    private final File dbFile;

    @Autowired
    public JsonRepository(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        try {
            URL resourceUrl = getClass().getClassLoader().getResource("data/db.json");
            if (resourceUrl == null) {
                throw new RuntimeException("No se encontró data/db.json");
            }

            Path targetClasses = Paths.get(resourceUrl.toURI()).getParent().getParent();
            Path projectRoot = targetClasses.getParent().getParent();
            this.dbFile = projectRoot.resolve("src/main/resources/data/db.json").toFile();

            this.database = objectMapper.readValue(this.dbFile, Database.class);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error al cargar db.json: " + e.getMessage(), e);
        }
    }

    // ── PERSISTENCIA ───────────────────────────────────────────────────
    public void persistir() throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dbFile, database);
    }

    public Database getDatabase() {
        return database;
    }

    // ── USUARIOS ───────────────────────────────────────────────────────
    public List<Usuario> findAllUsuarios() {
        return database.getUsuarios();
    }

    public Optional<Usuario> findUsuarioById(Integer id) {
        return database.getUsuarios().stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    public Optional<Usuario> findUsuarioByEmail(String email) {
        return database.getUsuarios().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    public boolean existeEmail(String email) {
        return database.getUsuarios().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    public void saveUsuario(Usuario usuario) throws IOException {
        if (usuario.getId() == null) {
            int next = database.getUsuarios().stream()
                    .mapToInt(Usuario::getId).max().orElse(0) + 1;
            usuario.setId(next);
        }
        database.getUsuarios().removeIf(u -> u.getId().equals(usuario.getId()));
        database.getUsuarios().add(usuario);
        persistir();
    }

    // ── VERIFICACIONES ─────────────────────────────────────────────────
    public List<Verificacion> findAllVerificaciones() {
        return database.getVerificaciones();
    }

    public Optional<Verificacion> findVerificacionByUsuarioId(Integer usuarioId) {
        return database.getVerificaciones().stream()
                .filter(v -> v.getUsuarioId().equals(usuarioId)).findFirst();
    }

    public void saveVerificacion(Verificacion verificacion) throws IOException {
        if (verificacion.getId() == null) {
            int next = database.getVerificaciones().stream()
                    .mapToInt(Verificacion::getId).max().orElse(0) + 1;
            verificacion.setId(next);
        }
        database.getVerificaciones().removeIf(v -> v.getId().equals(verificacion.getId()));
        database.getVerificaciones().add(verificacion);
        persistir();
    }

    // ── CATEGORÍAS ─────────────────────────────────────────────────────
    public List<Categoria> findAllCategorias() {
        return database.getCategorias();
    }

    public Optional<Categoria> findCategoriaById(Integer id) {
        return database.getCategorias().stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public void saveCategoria(Categoria categoria) throws IOException {
        if (categoria.getId() == null) {
            int next = database.getCategorias().stream()
                    .mapToInt(Categoria::getId).max().orElse(0) + 1;
            categoria.setId(next);
        }
        database.getCategorias().removeIf(c -> c.getId().equals(categoria.getId()));
        database.getCategorias().add(categoria);
        persistir();
    }

    // ── REPORTES ───────────────────────────────────────────────────────
    public List<Reporte> findAllReportes() {
        return database.getReportes();
    }

    public Optional<Reporte> findReporteById(Integer id) {
        return database.getReportes().stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    public List<Reporte> findReportesByUsuarioId(Integer usuarioId) {
        return database.getReportes().stream()
                .filter(r -> r.getUsuarioId().equals(usuarioId)).toList();
    }

    public void saveReporte(Reporte reporte) throws IOException {
        if (reporte.getId() == null) {
            int next = database.getReportes().stream()
                    .mapToInt(Reporte::getId).max().orElse(0) + 1;
            reporte.setId(next);
        }
        database.getReportes().removeIf(r -> r.getId().equals(reporte.getId()));
        database.getReportes().add(reporte);
        persistir();
    }

    public boolean deleteReporte(Integer id) throws IOException {
        boolean ok = database.getReportes().removeIf(r -> r.getId().equals(id));
        if (ok) {
            persistir();
        }
        return ok;
    }

    public boolean deleteReporteSiEsDueno(Integer id, Integer usuarioId) throws IOException {
        boolean ok = database.getReportes()
                .removeIf(r -> r.getId().equals(id) && r.getUsuarioId().equals(usuarioId));
        if (ok) {
            persistir();
        }
        return ok;
    }

    // ── ROLES ──────────────────────────────────────────────────────────
    public List<Rol> findAllRoles() {
        return database.getRoles();
    }

    public Optional<Rol> findRolById(Integer id) {
        return database.getRoles().stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    public Optional<Rol> findRolByNombre(String nombre) {
        return database.getRoles().stream()
                .filter(r -> r.getNombre().equalsIgnoreCase(nombre)).findFirst();
    }

    public void saveRol(Rol rol) throws IOException {
        if (rol.getId() == null) {
            int next = database.getRoles().stream()
                    .mapToInt(Rol::getId).max().orElse(0) + 1;
            rol.setId(next);
        }
        database.getRoles().removeIf(r -> r.getId().equals(rol.getId()));
        database.getRoles().add(rol);
        persistir();
    }

    public boolean deleteRol(Integer id) throws IOException {
        boolean ok = database.getRoles().removeIf(r -> r.getId().equals(id));
        if (ok) {
            persistir();
        }
        return ok;
    }

    // ── PERMISOS ───────────────────────────────────────────────────────
    public List<Permiso> findAllPermisos() {
        return database.getPermisos();
    }

    public Optional<Permiso> findPermisoById(Integer id) {
        return database.getPermisos().stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public List<Permiso> findPermisosByRolNombre(String rolNombre) {
        Optional<Rol> rolOpt = findRolByNombre(rolNombre);

        if (rolOpt.isEmpty()) {
            return List.of();
        }

        Rol rol = rolOpt.get();

        return rol.getPermisoIds().stream()
                .map(this::findPermisoById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public void savePermiso(Permiso permiso) throws IOException {
        if (permiso.getId() == null) {
            int next = database.getPermisos().stream()
                    .mapToInt(Permiso::getId).max().orElse(0) + 1;
            permiso.setId(next);
        }
        database.getPermisos().removeIf(p -> p.getId().equals(permiso.getId()));
        database.getPermisos().add(permiso);
        persistir();
    }

    public boolean deletePermiso(Integer id) throws IOException {
        boolean ok = database.getPermisos().removeIf(p -> p.getId().equals(id));
        if (ok) {
            persistir();
        }
        return ok;
    }
}

