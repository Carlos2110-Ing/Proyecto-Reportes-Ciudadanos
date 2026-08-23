package com.utp.reportes_ciudadanos.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {
    private final UsuarioService usuarioService;

    @Autowired
    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ── BIENVENIDA ─────────────────────────────────────────────────────

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) return "redirect:/dashboard";
        return "index";
    }

    // ── LOGIN ──────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) return "redirect:/dashboard";
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Optional<Usuario> opt = usuarioService.login(email, password);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Correo o contraseña incorrectos.");
            return "redirect:/login";
        }
        Usuario u = opt.get();
        session.setAttribute("usuarioId",     u.getId());
        session.setAttribute("usuarioNombre", u.getNombreCompleto());
        session.setAttribute("usuarioAvatar", u.getAvatarInicial());
        session.setAttribute("usuarioRol",    u.getRol().name());
        session.setAttribute("verificado",    usuarioService.isVerificado(u.getId()));
        
        redirectAttributes.addFlashAttribute("loginExitoso", true);
        
        return u.isAdmin() ? "redirect:/admin" : "redirect:/dashboard";
    }

    // ── REGISTRO ───────────────────────────────────────────────────────

    @GetMapping("/registro")
    public String registroForm(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) return "redirect:/dashboard";
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registroSubmit(@RequestParam String nombreCompleto,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/registro";
        }
        if (usuarioService.existeEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Ese correo ya está registrado.");
            return "redirect:/registro";
        }
        usuarioService.registrar(nombreCompleto, email, password);
        usuarioService.guardar();
        redirectAttributes.addFlashAttribute("exito", "¡Cuenta creada! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    // ── LOGOUT ─────────────────────────────────────────────────────────

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    
}
