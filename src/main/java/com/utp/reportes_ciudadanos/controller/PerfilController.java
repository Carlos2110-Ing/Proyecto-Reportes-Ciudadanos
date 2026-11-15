package com.utp.reportes_ciudadanos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.utp.reportes_ciudadanos.model.PerfilDTO;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.ReporteService;
import com.utp.reportes_ciudadanos.service.UsuarioService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final ReporteService reporteService;

    @Autowired
    public PerfilController(UsuarioService usuarioService, ReporteService reporteService) {
        this.usuarioService = usuarioService;
        this.reporteService = reporteService;
    }

    @GetMapping
    public String verPerfil(@AuthenticationPrincipal Usuario usuario, Model model) {
        PerfilDTO perfil = new PerfilDTO();
        perfil.setNombreCompleto(usuario.getNombreCompleto());
        perfil.setEmail(usuario.getEmail());
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfil", perfil);
        model.addAttribute("verificacion", usuarioService.findVerificacionByUsuario(usuario.getId()).orElse(null));
        model.addAttribute("totalReportes", reporteService.countByUsuario(usuario.getId()));
        model.addAttribute("resueltos", reporteService.countResueltosByUsuario(usuario.getId()));
        model.addAttribute("activos",
                reporteService.countByUsuario(usuario.getId())
                - reporteService.countResueltosByUsuario(usuario.getId()));
        return "user/perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @Valid @ModelAttribute("perfil") PerfilDTO perfil,
            BindingResult result,
            @AuthenticationPrincipal Usuario usuario,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!result.hasFieldErrors("email")
                && usuarioService.existeEmailEnOtroUsuario(
                        perfil.getEmail(),
                        usuario.getId())) {

            result.rejectValue(
                    "email",
                    "email.duplicado",
                    "Este correo electrónico ya está registrado."
            );
        }

        if (result.hasErrors()) {

            model.addAttribute("usuario", usuario);
            model.addAttribute("verificacion", usuarioService.findVerificacionByUsuario(usuario.getId()).orElse(null));
            model.addAttribute("totalReportes", reporteService.countByUsuario(usuario.getId()));
            model.addAttribute("resueltos", reporteService.countResueltosByUsuario(usuario.getId()));
            model.addAttribute("activos",
                    reporteService.countByUsuario(usuario.getId())
                    - reporteService.countResueltosByUsuario(usuario.getId()));

            return "user/perfil";
        }

        usuarioService.actualizarPerfil(
                usuario.getId(),
                perfil.getNombreCompleto(),
                perfil.getEmail());

        redirectAttributes.addFlashAttribute("exito", "Datos actualizados correctamente.");
        return "redirect:/perfil";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@RequestParam String passwordActual,
            @RequestParam String nuevaPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes redirectAttributes) {
        if (!usuarioService.passwordCorrecta(usuario.getId(), passwordActual)) {
            redirectAttributes.addFlashAttribute("errorPassword", "La contraseña actual es incorrecta.");
            return "redirect:/perfil";
        }
        if (!nuevaPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Las contraseñas no coinciden.");
            return "redirect:/perfil";
        }
        usuarioService.cambiarPassword(usuario.getId(), nuevaPassword);
        redirectAttributes.addFlashAttribute("exitoPassword", "Contraseña actualizada.");
        return "redirect:/perfil";
    }
}
