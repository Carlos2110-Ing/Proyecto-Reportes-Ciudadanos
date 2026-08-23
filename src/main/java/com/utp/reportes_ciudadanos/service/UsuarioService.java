package com.utp.reportes_ciudadanos.service;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.utp.reportes_ciudadanos.model.Database;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.model.Verificacion;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;

@Service
public class UsuarioService {
    private final ObjectMapper objectMapper;
    private Database database;
 
    private File dbFile;
    @Value("${app.db.path}")
    private String dbPath;

    private final AtomicInteger userIdSeq  = new AtomicInteger(100);
    private final AtomicInteger verifIdSeq = new AtomicInteger(100);
    
    @Autowired
    public UsuarioService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
 
    // ── CARGA INICIAL ──────────────────────────────────────────────────
 
    @PostConstruct
    public void init() {
        try {
            dbFile = new File(dbPath);

            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs(); // crea carpeta si no existe
                database = new Database();
                persistir();
            } else {
                database = objectMapper.readValue(dbFile, Database.class);
            }

            // Inicializar secuencias
            database.getUsuarios().stream()
                    .mapToInt(Usuario::getId).max()
                    .ifPresent(m -> userIdSeq.set(m + 1));

            database.getVerificaciones().stream()
                    .mapToInt(Verificacion::getId).max()
                    .ifPresent(m -> verifIdSeq.set(m + 1));

        } catch (Exception e) {
            throw new RuntimeException("Error al cargar db.json: " + e.getMessage(), e);
        }
    }
 
    /**
     * Persiste el estado actual del Database en memoria al archivo db.json.
     * Se llama después de cada operación de escritura.
     */
    private void persistir() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(dbFile, database);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo persistir db.json: " + e.getMessage(), e);
        }
    }
 
    /**
     * Expone el Database en memoria para que ReporteService
     * trabaje sobre la misma instancia sin duplicar la carga del JSON.
     */
    public Database getDatabase() {
        return database;
    }
 
    /**
     * Permite que ReporteService dispare la persistencia después
     * de sus propias operaciones de escritura.
     */
    public void guardar() {
        persistir();
    }
 
    // ── AUTENTICACIÓN ──────────────────────────────────────────────────
 
    public Optional<Usuario> login(String email, String password) {
        return database.getUsuarios().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)
                          && u.getPassword().equals(password))
                .findFirst();
    }
 
    // ── CONSULTAS ──────────────────────────────────────────────────────
 
    public List<Usuario> findAll() {
        return database.getUsuarios();
    }
 
    public Optional<Usuario> findById(Integer id) {
        return database.getUsuarios().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }
 
    public boolean existeEmail(String email) {
        return database.getUsuarios().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }
 
    // ── REGISTRO ───────────────────────────────────────────────────────
 
    public Usuario registrar(String nombreCompleto, String email, String password) {
        Usuario u = new Usuario();
        u.setId(userIdSeq.getAndIncrement());
        u.setNombreCompleto(nombreCompleto);
        u.setEmail(email);
        u.setPassword(password);
        u.setUsername(email.split("@")[0]);
        u.setRol(Usuario.Rol.USUARIO);
        u.setAvatarInicial(iniciales(nombreCompleto));
        u.setFechaRegistro(LocalDate.now().toString());
        database.getUsuarios().add(u);
 
        // Crear verificación pendiente automáticamente
        Verificacion v = new Verificacion();
        v.setId(verifIdSeq.getAndIncrement());
        v.setUsuarioId(u.getId());
        v.setEstado(Verificacion.Estado.PENDIENTE);
        database.getVerificaciones().add(v);
 
        persistir(); // ← guardar en disco
        return u;
    }
 
    // ── ACTUALIZAR PERFIL ──────────────────────────────────────────────
 
    public void actualizarPerfil(Integer id, String nombreCompleto, String email) {
        findById(id).ifPresent(u -> {
            u.setNombreCompleto(nombreCompleto);
            u.setEmail(email);
            u.setAvatarInicial(iniciales(nombreCompleto));
        });
        persistir(); // ← guardar en disco
    }
 
    public void cambiarPassword(Integer id, String nuevaPassword) {
        findById(id).ifPresent(u -> u.setPassword(nuevaPassword));
        persistir(); // ← guardar en disco
    }
 
    // ── VERIFICACIÓN ───────────────────────────────────────────────────
 
    public boolean isVerificado(Integer usuarioId) {
        return database.getVerificaciones().stream()
                .filter(v -> v.getUsuarioId().equals(usuarioId))
                .anyMatch(Verificacion::isVerificado);
    }
 
    public Optional<Verificacion> findVerificacionByUsuario(Integer usuarioId) {
        return database.getVerificaciones().stream()
                .filter(v -> v.getUsuarioId().equals(usuarioId))
                .findFirst();
    }
 
    public List<Verificacion> findAllVerificaciones() {
        return database.getVerificaciones();
    }
 
    public void verificarUsuario(Integer usuarioId, Integer adminId) {
        findVerificacionByUsuario(usuarioId).ifPresent(v -> {
            v.setEstado(Verificacion.Estado.VERIFICADO);
            v.setFechaVerificacion(LocalDate.now().toString());
            v.setAdminId(adminId);
        });
        persistir(); // ← guardar en disco
    }
 
    public void revocarVerificacion(Integer usuarioId) {
        findVerificacionByUsuario(usuarioId).ifPresent(v -> {
            v.setEstado(Verificacion.Estado.PENDIENTE);
            v.setFechaVerificacion(null);
            v.setAdminId(null);
        });
        persistir(); // ← guardar en disco
    }
 
    // ── HELPERS ────────────────────────────────────────────────────────
 
    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "??";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1)
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
    }

}
