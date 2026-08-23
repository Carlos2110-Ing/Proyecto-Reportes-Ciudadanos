package com.utp.reportes_ciudadanos.model;

public class Verificacion {
    public enum Estado { VERIFICADO, PENDIENTE }

    private Integer id;
    private Integer usuarioId;
    private Estado estado;
    private String fechaVerificacion;
    private Integer adminId;

    public Verificacion() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getFechaVerificacion() { return fechaVerificacion; }
    public void setFechaVerificacion(String fechaVerificacion) { this.fechaVerificacion = fechaVerificacion; }

    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    // helper
    public boolean isVerificado() {
        return Estado.VERIFICADO.equals(this.estado);
    }

}
