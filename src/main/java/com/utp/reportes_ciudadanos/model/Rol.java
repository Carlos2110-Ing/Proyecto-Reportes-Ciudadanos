package com.utp.reportes_ciudadanos.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "roles")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre del rol es obligatorio.")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres.")
    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚáéíóúÑñ_ ]+$", message = "El rol solo puede contener letras, espacios o guion bajo.")
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(min = 5, max = 150, message = "La descripción debe tener entre 5 y 150 caracteres.")
    @Column(length = 150)
    private String descripcion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "rol_permisos",
            joinColumns = @JoinColumn(name = "rol_id")
    )
    @Column(name = "permiso_id")
    private List<Integer> permisoIds = new ArrayList<>();

    public Rol() {
    }

    public Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<Integer> getPermisoIds() {
        return permisoIds;
    }

    public void setPermisoIds(List<Integer> permisoIds) {
        this.permisoIds = permisoIds;
    }
}

