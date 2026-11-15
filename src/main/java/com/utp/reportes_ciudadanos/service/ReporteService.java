package com.utp.reportes_ciudadanos.service;

import com.utp.reportes_ciudadanos.model.Categoria;
import com.utp.reportes_ciudadanos.model.Reporte;
import com.utp.reportes_ciudadanos.repository.CategoriaJpaRepository;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.ReporteJpaRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {
    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);
    private final AtomicInteger idSeq = new AtomicInteger(100);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ReporteJpaRepository reporteJpa;
    private final CategoriaJpaRepository categoriaJpa;
    private final UsuarioService usuarioService;
    private final JsonRepository jsonRepo;

    @Autowired
    public ReporteService(ReporteJpaRepository reporteJpa,
            CategoriaJpaRepository categoriaJpa,
            UsuarioService usuarioService,
            JsonRepository jsonRepo) {
        this.reporteJpa = reporteJpa;
        this.categoriaJpa = categoriaJpa;
        this.usuarioService = usuarioService;
        this.jsonRepo = jsonRepo;
    }

    // ── CATEGORÍAS ─────────────────────────────────────────────────────

    public List<Categoria> findAllCategorias() {
        try {
            return categoriaJpa.findAll();
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findAllCategorias. Usando JSON.");
            return jsonRepo.findAllCategorias();
        }
    }

    public Optional<Categoria> findCategoriaById(Integer id) {
        try {
            return categoriaJpa.findById(id);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findCategoriaById. Usando JSON.");
            return jsonRepo.findCategoriaById(id);
        }
    }

    // ── CONSULTAS ──────────────────────────────────────────────────────

    public List<Reporte> findAll() {
        List<Reporte> reportes;
        try {
            reportes = reporteJpa.findAllByOrderByFechaCreacionDesc();
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findAll reportes. Usando JSON.");
            reportes = jsonRepo.findAllReportes().stream()
                    .sorted(Comparator.comparing(Reporte::getFechaCreacion).reversed())
                    .collect(Collectors.toList());
        }
        return reportes.stream().map(this::enriquecer).collect(Collectors.toList());
    }

    public List<Reporte> findUltimos(int n) {
        return findAll().stream().limit(n).collect(Collectors.toList());
    }

    public List<Reporte> findByUsuario(Integer usuarioId) {
        List<Reporte> reportes;
        try {
            reportes = reporteJpa.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findByUsuario. Usando JSON.");
            reportes = jsonRepo.findReportesByUsuarioId(usuarioId).stream()
                    .sorted(Comparator.comparing(Reporte::getFechaCreacion).reversed())
                    .collect(Collectors.toList());
        }
        return reportes.stream().map(this::enriquecer).collect(Collectors.toList());
    }

    public Optional<Reporte> findById(Integer id) {
        try {
            return reporteJpa.findById(id).map(this::enriquecer);
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible en findById reporte. Usando JSON.");
            return jsonRepo.findReporteById(id).map(this::enriquecer);
        }
    }

    // ── ESTADÍSTICAS ───────────────────────────────────────────────────

    public long countTotal() {
        try {
            return reporteJpa.count();
        } catch (Exception e) {
            log.warn("[FAILOVER] countTotal desde JSON.");
            return jsonRepo.findAllReportes().size();
        }
    }

    public long countPendiente() {
        try {
            return reporteJpa.countByEstado(Reporte.Estado.PENDIENTE);
        } catch (Exception e) {
            return jsonRepo.findAllReportes().stream().filter(Reporte::isPendiente).count();
        }
    }

    public long countEnProceso() {
        try {
            return reporteJpa.countByEstado(Reporte.Estado.EN_PROCESO);
        } catch (Exception e) {
            return jsonRepo.findAllReportes().stream().filter(Reporte::isEnProceso).count();
        }
    }

    public long countResuelto() {
        try {
            return reporteJpa.countByEstado(Reporte.Estado.RESUELTO);
        } catch (Exception e) {
            return jsonRepo.findAllReportes().stream().filter(Reporte::isResuelto).count();
        }
    }

    public long countByUsuario(Integer usuarioId) {
        try {
            return reporteJpa.countByUsuarioId(usuarioId);
        } catch (Exception e) {
            return jsonRepo.findReportesByUsuarioId(usuarioId).size();
        }
    }

    public long countResueltosByUsuario(Integer usuarioId) {
        try {
            return reporteJpa.countByUsuarioIdAndEstado(usuarioId, Reporte.Estado.RESUELTO);
        } catch (Exception e) {
            return jsonRepo.findReportesByUsuarioId(usuarioId).stream()
                    .filter(Reporte::isResuelto).count();
        }
    }

    // ── CREACIÓN ───────────────────────────────────────────────────────

    public Reporte crear(String titulo, String descripcion, Integer categoriaId,
            String ubicacion, Double latitud, Double longitud,
            Integer usuarioId) {
        Reporte r = new Reporte();
        r.setTitulo(titulo);
        r.setDescripcion(descripcion);
        r.setCategoriaId(categoriaId);
        r.setUbicacion(ubicacion);
        r.setLatitud(latitud);
        r.setLongitud(longitud);
        r.setUsuarioId(usuarioId);
        r.setEstado(Reporte.Estado.PENDIENTE);
        r.setFechaCreacion(LocalDateTime.now().format(FORMATTER));

        // ── Escritura duplicada ──
        try {
            r = reporteJpa.save(r);
            log.debug("Reporte '{}' guardado en MySQL con id={}", titulo, r.getId());
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al crear reporte. Solo JSON.");
        }

        try {
            jsonRepo.saveReporte(r);
        } catch (IOException e) {
            log.error("Error al guardar reporte '{}' en JSON: {}", titulo, e.getMessage());
        }
        return r;
    }

    // ── EDICIÓN ────────────────────────────────────────────────────────

    public void actualizar(Integer id, String titulo, String descripcion,
            Integer categoriaId, String ubicacion, Double latitud,
            Double longitud) {
        findById(id).ifPresent(r -> {
            r.setTitulo(titulo);
            r.setDescripcion(descripcion);
            r.setCategoriaId(categoriaId);
            r.setUbicacion(ubicacion);
            r.setLatitud(latitud);
            r.setLongitud(longitud);
            guardarReporte(r);
        });
    }

    public void cambiarEstado(Integer id, Reporte.Estado nuevoEstado) {
        findById(id).ifPresent(r -> {
            r.setEstado(nuevoEstado);
            guardarReporte(r);
        });
    }

    // ── ELIMINACIÓN ────────────────────────────────────────────────────

    public boolean eliminar(Integer id) {
        boolean eliminado = false;
        try {
            if (reporteJpa.existsById(id)) {
                reporteJpa.deleteById(id);
                eliminado = true;
                log.debug("Reporte id={} eliminado de MySQL.", id);
            }
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al eliminar reporte. Solo JSON.");
        }
        
        boolean eliminadoJson = false;
        try {
            eliminadoJson = jsonRepo.deleteReporte(id);
        } catch (IOException e) {
            log.error("Error al eliminar reporte id={} en JSON: {}", id, e.getMessage());
        }
        return eliminado || eliminadoJson;
    }

    public boolean eliminarSiEsDueno(Integer id, Integer usuarioId) {
        boolean eliminado = false;
        try {
            Optional<Reporte> opt = reporteJpa.findById(id);
            if (opt.isPresent() && opt.get().getUsuarioId().equals(usuarioId)) {
                reporteJpa.deleteById(id);
                eliminado = true;
            }
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al eliminar reporte del dueño. Solo JSON.");
        }
        boolean eliminadoJson = false;
        try {
            eliminadoJson = jsonRepo.deleteReporteSiEsDueno(id, usuarioId);
        } catch (IOException e) {
            log.error("Error al eliminar reporte id={} en JSON: {}", id, e.getMessage());
        }
        return eliminado || eliminadoJson;
    }

    // ── ESCRITURA DUPLICADA (helper interno) ───────────────────────────

    private void guardarReporte(Reporte r) {
        // Limpiar campos @Transient antes de persistir en JPA
        r.setCategoria(null);
        r.setUsuarioNombre(null);
        r.setUsuarioAvatar(null);
        try {
            reporteJpa.save(r);
            log.debug("Reporte id={} actualizado en MySQL.", r.getId());
        } catch (Exception e) {
            log.warn("[FAILOVER] MySQL no disponible al guardar reporte. Solo JSON.");
        }

        try {
            jsonRepo.saveReporte(r);
        } catch (IOException e) {
            log.error("Error al guardar reporte id={} en JSON: {}", r.getId(), e.getMessage());
        }
    }

    // ── ENRIQUECIMIENTO ────────────────────────────────────────────────

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
