package com.utp.reportes_ciudadanos.model;

import java.util.List;

public class Database {
    private List<Categoria> categorias;
    private List<Verificacion> verificaciones;
    private List<Usuario> usuarios;
    private List<Reporte> reportes;

    public Database() {}

    public List<Categoria> getCategorias() { return categorias; }
    public void setCategorias(List<Categoria> categorias) { this.categorias = categorias; }

    public List<Verificacion> getVerificaciones() { return verificaciones; }
    public void setVerificaciones(List<Verificacion> verificaciones) { this.verificaciones = verificaciones; }

    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }

    public List<Reporte> getReportes() { return reportes; }
    public void setReportes(List<Reporte> reportes) { this.reportes = reportes; }

}
