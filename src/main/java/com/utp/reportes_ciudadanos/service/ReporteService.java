package com.utp.reportes_ciudadanos.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.utp.reportes_ciudadanos.model.Categoria;
import com.utp.reportes_ciudadanos.model.Reporte;

@Service
public class ReporteService {
    private final UsuarioService usuarioService;
    private final AtomicInteger idSeq = new AtomicInteger(100);
 
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    @Autowired        
    public ReporteService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        // Inicializar secuencia por encima del max actual del JSON
        usuarioService.getDatabase().getReportes().stream()
                .mapToInt(Reporte::getId).max()
                .ifPresent(m -> idSeq.set(m + 1));
    }
 
    // ── CATEGORÍAS ─────────────────────────────────────────────────────
 
    public List<Categoria> findAllCategorias() {
        return usuarioService.getDatabase().getCategorias();
    }
 
    public Optional<Categoria> findCategoriaById(Integer id) {
        return usuarioService.getDatabase().getCategorias().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }
 
    // ── CONSULTAS ──────────────────────────────────────────────────────
 
    /** Todos los reportes ordenados por fecha desc, con datos enriquecidos */
    public List<Reporte> findAll() {
        return usuarioService.getDatabase().getReportes().stream()
                .sorted(Comparator.comparing(Reporte::getFechaCreacion).reversed())
                .map(this::enriquecer)
                .collect(Collectors.toList());
    }
 
    /** Últimos N reportes para el feed del dashboard */
    public List<Reporte> findUltimos(int n) {
        return findAll().stream().limit(n).collect(Collectors.toList());
    }
 
    /** Reportes de un usuario específico */
    public List<Reporte> findByUsuario(Integer usuarioId) {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(r -> r.getUsuarioId().equals(usuarioId))
                .sorted(Comparator.comparing(Reporte::getFechaCreacion).reversed())
                .map(this::enriquecer)
                .collect(Collectors.toList());
    }
 
    public Optional<Reporte> findById(Integer id) {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(r -> r.getId().equals(id))
                .map(this::enriquecer)
                .findFirst();
    }
 
    // ── ESTADÍSTICAS ───────────────────────────────────────────────────
 
    public long countTotal() {
        return usuarioService.getDatabase().getReportes().size();
    }
 
    public long countPendiente() {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(Reporte::isPendiente).count();
    }
 
    public long countEnProceso() {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(Reporte::isEnProceso).count();
    }
 
    public long countResuelto() {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(Reporte::isResuelto).count();
    }
 
    public long countByUsuario(Integer usuarioId) {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(r -> r.getUsuarioId().equals(usuarioId)).count();
    }
 
    public long countResueltosByUsuario(Integer usuarioId) {
        return usuarioService.getDatabase().getReportes().stream()
                .filter(r -> r.getUsuarioId().equals(usuarioId) && r.isResuelto()).count();
    }
 
    // ── CREACIÓN ───────────────────────────────────────────────────────
 
    public Reporte crear(String titulo, String descripcion, Integer categoriaId,
                         String ubicacion, Double latitud, Double longitud,
                         Integer usuarioId) {
        Reporte r = new Reporte();
        r.setId(idSeq.getAndIncrement());
        r.setTitulo(titulo);
        r.setDescripcion(descripcion);
        r.setCategoriaId(categoriaId);
        r.setUbicacion(ubicacion);
        r.setLatitud(latitud);
        r.setLongitud(longitud);
        r.setUsuarioId(usuarioId);
        r.setEstado(Reporte.Estado.PENDIENTE);
        r.setFechaCreacion(LocalDateTime.now().format(FORMATTER));
        usuarioService.getDatabase().getReportes().add(r);
 
        usuarioService.guardar(); // ← persistir en db.json
        return r;
    }
 
    // ── EDICIÓN ────────────────────────────────────────────────────────
 
    public void actualizar(Integer id, String titulo, String descripcion,
                           Integer categoriaId, String ubicacion,
                           Double latitud, Double longitud) {
        usuarioService.getDatabase().getReportes().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .ifPresent(r -> {
                    r.setTitulo(titulo);
                    r.setDescripcion(descripcion);
                    r.setCategoriaId(categoriaId);
                    r.setUbicacion(ubicacion);
                    if (latitud  != null) r.setLatitud(latitud);
                    if (longitud != null) r.setLongitud(longitud);
                });
 
        usuarioService.guardar(); // ← persistir en db.json
    }
 
    public void cambiarEstado(Integer id, Reporte.Estado nuevoEstado) {
        usuarioService.getDatabase().getReportes().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .ifPresent(r -> r.setEstado(nuevoEstado));
 
        usuarioService.guardar(); // ← persistir en db.json
    }
 
    // ── ELIMINACIÓN ────────────────────────────────────────────────────
 
    public boolean eliminar(Integer id) {
        boolean eliminado = usuarioService.getDatabase().getReportes()
                .removeIf(r -> r.getId().equals(id));
        if (eliminado) usuarioService.guardar(); // ← persistir en db.json
        return eliminado;
    }
 
    /** Eliminar solo si el reporte pertenece al usuario (guard básico) */
    public boolean eliminarSiEsDueno(Integer id, Integer usuarioId) {
        boolean eliminado = usuarioService.getDatabase().getReportes()
                .removeIf(r -> r.getId().equals(id)
                            && r.getUsuarioId().equals(usuarioId));
        if (eliminado) usuarioService.guardar(); // ← persistir en db.json
        return eliminado;
    }
 
    // ── ENRIQUECIMIENTO ────────────────────────────────────────────────
 
    /**
     * Completa los campos transitorios del Reporte (categoría, nombre
     * de usuario y estado de verificación) antes de enviarlo a la vista.
     * Opera sobre una copia enriquecida sin tocar el objeto original en memoria.
     */
    private Reporte enriquecer(Reporte r) {
        findCategoriaById(r.getCategoriaId()).ifPresent(r::setCategoria);
        usuarioService.findById(r.getUsuarioId()).ifPresent(u -> {
            r.setUsuarioNombre(u.getNombreCompleto());
            r.setUsuarioAvatar(u.getAvatarInicial());
        });
        r.setUsuarioVerificado(usuarioService.isVerificado(r.getUsuarioId()));
        return r;
    }
}
