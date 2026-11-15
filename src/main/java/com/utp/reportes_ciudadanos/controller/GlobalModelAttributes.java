package com.utp.reportes_ciudadanos.controller;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UsuarioService usuarioService;

    @Autowired
    public GlobalModelAttributes(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute
    public void cargarAtributosGlobales(
            @AuthenticationPrincipal Usuario usuario,
            Model model) {

        if (usuario != null) {
            model.addAttribute(
                "verificado",
                usuarioService.isVerificado(usuario.getId())
            );
        }
    }
}
