package com.utp.reportes_ciudadanos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "permisos")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "La URL del permiso es obligatoria.")
    @Size(min = 3, max = 150, message = "La URL debe tener entre 2 y 150 caracteres.")
    @Pattern(
            regexp = "^/.*",
            message = "La URL debe empezar con /."
    )
    @Column(name = "url_pattern", nullable = false, length = 150)
    private String urlPattern;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(min = 5, max = 100, message = "La descripción debe tener entre 5 y 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String descripcion;

    @NotBlank(message = "Debe seleccionar un método HTTP.")
    @Pattern(
            regexp = "GET|POST|\\*",
            message = "Método HTTP inválido."
    )
    @Column(name = "http_method", length = 10)
    private String httpMethod; // GET, POST, * (cualquiera)

    public Permiso() {
    }

    public Permiso(String urlPattern, String descripcion, String httpMethod) {
        this.urlPattern = urlPattern;
        this.descripcion = descripcion;
        this.httpMethod = httpMethod;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}

