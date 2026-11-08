package com.utp.reportes_ciudadanos.model;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private List<Categoria>    categorias    = new ArrayList<>();
    private List<Verificacion> verificaciones = new ArrayList<>();
    private List<Usuario>      usuarios       = new ArrayList<>();
    private List<Reporte>      reportes       = new ArrayList<>();
    private List<Rol>          roles          = new ArrayList<>();
    private List<Permiso>      permisos       = new ArrayList<>();

    public Database() {}

    public List<Categoria>    getCategorias()    { return categorias; }
    public void setCategorias(List<Categoria> c) { this.categorias = c; }

    public List<Verificacion> getVerificaciones()    { return verificaciones; }
    public void setVerificaciones(List<Verificacion> v) { this.verificaciones = v; }

    public List<Usuario> getUsuarios()           { return usuarios; }
    public void setUsuarios(List<Usuario> u)     { this.usuarios = u; }

    public List<Reporte> getReportes()           { return reportes; }
    public void setReportes(List<Reporte> r)     { this.reportes = r; }

    public List<Rol> getRoles()                  { return roles; }
    public void setRoles(List<Rol> r)            { this.roles = r; }

    public List<Permiso> getPermisos()           { return permisos; }
    public void setPermisos(List<Permiso> p)     { this.permisos = p; }
}

