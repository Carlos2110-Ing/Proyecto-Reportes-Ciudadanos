package com.utp.reportes_ciudadanos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.UsuarioService;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    @Autowired
    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ── BIENVENIDA ─────────────────────────────────────────────────────
    @GetMapping("/")
    public String index(Authentication auth) {
        // Si ya está autenticado, redirigir según rol
        if (auth != null && auth.isAuthenticated()) {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            return esAdmin ? "redirect:/admin" : "redirect:/dashboard";
        }
        return "index";
    }

    // ── LOGIN ──────────────────────────────────────────────────────────
    // Spring Security maneja el POST /login automáticamente.
    // Aquí solo mostramos la vista del formulario.
    @GetMapping("/login")
    public String loginForm(Authentication auth,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            org.springframework.ui.Model model) {
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("exito", "Has cerrado sesión correctamente.");
        }
        return "auth/login";
    }

    // ── REGISTRO ───────────────────────────────────────────────────────
    @GetMapping("/registro")
    public String registroForm(Authentication auth, Model model) {
        model.addAttribute("usuario", new Usuario());
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registroSubmit(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult result,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validar confirmación de contraseña
        if (!usuario.getPassword().equals(confirmPassword)) {
            result.rejectValue(
                    "password",
                    "error.password",
                    "Las contraseñas no coinciden");
        }

        // Validar correo duplicado
        if (usuarioService.existeEmail(usuario.getEmail())) {
            result.rejectValue(
                    "email",
                    "error.email",
                    "Ese correo ya está registrado");
        }

        // Si existe algún error vuelve al formulario
        if (result.hasErrors()) {
            return "auth/registro";
        }

        usuarioService.registrar(
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getPassword());

        redirectAttributes.addFlashAttribute(
                "exito",
                "¡Cuenta creada! Ya puedes iniciar sesión.");

        return "redirect:/login";
    }
}

