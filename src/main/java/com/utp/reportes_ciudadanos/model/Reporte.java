package com.utp.reportes_ciudadanos.model;

public class Reporte {
    public enum Estado { PENDIENTE, EN_PROCESO, RESUELTO }

    private Integer id;
    private String titulo;
    private String descripcion;
    private Integer categoriaId;
    private Integer usuarioId;
    private String ubicacion;
    private Double latitud;
    private Double longitud;
    private Estado estado;
    private String fechaCreacion;

    // Campos enriquecidos (no vienen del JSON, se llenan en el service)
    private Categoria categoria;
    private String usuarioNombre;
    private String usuarioAvatar;
    private boolean usuarioVerificado;

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
