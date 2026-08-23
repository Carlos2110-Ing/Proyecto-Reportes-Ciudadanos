package com.utp.reportes_ciudadanos.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.ReporteService;
import com.utp.reportes_ciudadanos.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

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
    public String verPerfil(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer id = (Integer) session.getAttribute("usuarioId");
        Optional<Usuario> opt = usuarioService.findById(id);
        if (opt.isEmpty()) return "redirect:/logout";

        Usuario u = opt.get();
        model.addAttribute("usuario",       u);
        model.addAttribute("verificacion",  usuarioService.findVerificacionByUsuario(id).orElse(null));
        model.addAttribute("totalReportes", reporteService.countByUsuario(id));
        model.addAttribute("resueltos",     reporteService.countResueltosByUsuario(id));
        model.addAttribute("activos",
                reporteService.countByUsuario(id) - reporteService.countResueltosByUsuario(id));
        return "user/perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarPerfil(@RequestParam String nombreCompleto,
                                   @RequestParam String email,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer id = (Integer) session.getAttribute("usuarioId");
        usuarioService.actualizarPerfil(id, nombreCompleto, email);

        // Refrescar sesión
        session.setAttribute("usuarioNombre", nombreCompleto);
        session.setAttribute("usuarioAvatar",
                usuarioService.findById(id).map(Usuario::getAvatarInicial).orElse("??"));

        redirectAttributes.addFlashAttribute("exito", "Datos actualizados correctamente.");
        return "redirect:/perfil";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@RequestParam String passwordActual,
                                  @RequestParam String nuevaPassword,
                                  @RequestParam String confirmPassword,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer id = (Integer) session.getAttribute("usuarioId");
        Optional<Usuario> opt = usuarioService.findById(id);

        if (opt.isEmpty() || !opt.get().getPassword().equals(passwordActual)) {
            redirectAttributes.addFlashAttribute("errorPassword", "La contraseña actual es incorrecta.");
            return "redirect:/perfil";
        }
        if (!nuevaPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Las contraseñas no coinciden.");
            return "redirect:/perfil";
        }
        usuarioService.cambiarPassword(id, nuevaPassword);
        redirectAttributes.addFlashAttribute("exitoPassword", "Contraseña actualizada.");
        return "redirect:/perfil";
    }

}
