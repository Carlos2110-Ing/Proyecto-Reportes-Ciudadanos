package com.utp.reportes_ciudadanos.model;

import jakarta.validation.constraints.*;

public class PerfilDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 5, max = 100,
          message = "El nombre debe tener entre 5 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingrese un correo válido")
    private String email;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    
}

