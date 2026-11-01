package com.utp.reportes_ciudadanos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

@Entity
@Table(name = "verificaciones")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Verificacion {
    public enum Estado { VERIFICADO, PENDIENTE }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Estado estado;
    
    @Column(name = "fecha_verificacion", length = 20)
    private String fechaVerificacion;
    
    @Column(name = "admin_id")
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

