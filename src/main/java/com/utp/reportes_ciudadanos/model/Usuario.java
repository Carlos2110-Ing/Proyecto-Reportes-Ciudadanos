package com.utp.reportes_ciudadanos.model;

public class Usuario {
    public enum Rol {
        USUARIO, ADMIN
    }

    private Integer id;
    private String username;
    private String email;
    private String password;
    private String nombreCompleto;
    private Rol rol;
    private String avatarInicial;
    private String fechaRegistro;

    public Usuario() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getAvatarInicial() {
        return avatarInicial;
    }

    public void setAvatarInicial(String avatarInicial) {
        this.avatarInicial = avatarInicial;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    // helpers
    public boolean isAdmin() {
        return Rol.ADMIN.equals(this.rol);
    }
}
