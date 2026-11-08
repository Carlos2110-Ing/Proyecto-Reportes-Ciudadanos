package com.utp.reportes_ciudadanos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "reportes")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Reporte {
    public enum Estado { PENDIENTE, EN_PROCESO, RESUELTO }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 5, max = 150, message = "El título debe tener entre 5 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String titulo;
    
    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    
    @NotNull
    @Column(name = "categoria_id", nullable = false)
    private Integer categoriaId;
    
    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;
    
    @NotBlank(message = "La ubicación es obligatoria")
    @Size(min = 5, max = 200, message = "La ubicación debe tener entre 5 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String ubicacion;
    
    @NotNull(message = "Debe seleccionar una ubicación en el mapa")
    @Column(nullable = false)
    private Double latitud;
    
    @NotNull(message = "Debe seleccionar una ubicación en el mapa")
    @Column(nullable = false)
    private Double longitud;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Estado estado;
    
    @Column(name = "fecha_creacion", length = 30)
    private String fechaCreacion;

    // Campos enriquecidos (Solo en memoria, no van a la BD)
    @Transient private Categoria categoria;
    @Transient private String usuarioNombre;
    @Transient private String usuarioAvatar;
    @Transient private boolean usuarioVerificado;

    public Reporte() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getUsuarioAvatar() { return usuarioAvatar; }
    public void setUsuarioAvatar(String usuarioAvatar) { this.usuarioAvatar = usuarioAvatar; }

    public boolean isUsuarioVerificado() { return usuarioVerificado; }
    public void setUsuarioVerificado(boolean usuarioVerificado) { this.usuarioVerificado = usuarioVerificado; }

    // helpers para Thymeleaf
    public boolean isPendiente()  { return Estado.PENDIENTE.equals(this.estado); }
    public boolean isEnProceso()  { return Estado.EN_PROCESO.equals(this.estado); }
    public boolean isResuelto()   { return Estado.RESUELTO.equals(this.estado); }
}

