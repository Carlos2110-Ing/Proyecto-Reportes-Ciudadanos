/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.safeperu.controller;

import com.safeperu.model.Reporte;
import com.safeperu.model.Usuario;
import com.safeperu.service.ReporteService;
import com.safeperu.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author Usuario
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UsuarioService usuarioService;
    private final ReporteService reporteService;

    @Autowired
    public AdminController(UsuarioService usuarioService, ReporteService reporteService) {
        this.usuarioService = usuarioService;
        this.reporteService = reporteService;
    }
    
    // ── GUARD ──────────────────────────────────────────────────────────

    private boolean noEsAdmin(HttpSession session) {
        return !"ADMIN".equals(session.getAttribute("usuarioRol"));
    }

    // ── DASHBOARD ADMIN ────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public String adminDashboard(HttpSession session, Model model) {
        if (noEsAdmin(session)) return "redirect:/dashboard";

        List<Usuario> usuarios = usuarioService.findAll().stream()
                .filter(u -> !u.isAdmin())
                .collect(Collectors.toList());

        // Enriquecer con estado de verificación
        model.addAttribute("usuarios",         usuarios);
        model.addAttribute("verificaciones",   usuarioService.findAllVerificaciones());
        model.addAttribute("totalUsuarios",    usuarios.size());
        model.addAttribute("totalVerificados",
                usuarios.stream().filter(u -> usuarioService.isVerificado(u.getId())).count());
        model.addAttribute("totalReportes",    reporteService.countTotal());
        model.addAttribute("pendientes",       reporteService.countPendiente());
        model.addAttribute("ultimosReportes",  reporteService.findAll());
        return "admin/admin";
    }

    // ── VERIFICACIÓN ───────────────────────────────────────────────────

    @PostMapping("/verificar/{usuarioId}")
    public String verificar(@PathVariable Integer usuarioId,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (noEsAdmin(session)) return "redirect:/dashboard";
        Integer adminId = (Integer) session.getAttribute("usuarioId");
        usuarioService.verificarUsuario(usuarioId, adminId);
        redirectAttributes.addFlashAttribute("exito", "Usuario verificado.");
        return "redirect:/admin";
    }

    @PostMapping("/revocar/{usuarioId}")
    public String revocar(@PathVariable Integer usuarioId,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (noEsAdmin(session)) return "redirect:/dashboard";
        usuarioService.revocarVerificacion(usuarioId);
        redirectAttributes.addFlashAttribute("exito", "Verificación revocada.");
        return "redirect:/admin";
    }

    // ── CAMBIAR ESTADO REPORTE ─────────────────────────────────────────

    @PostMapping("/reportes/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id,
                                @RequestParam String estado,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (noEsAdmin(session)) return "redirect:/dashboard";
        try {
            reporteService.cambiarEstado(id, Reporte.Estado.valueOf(estado));
            redirectAttributes.addFlashAttribute("exito", "Estado actualizado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Estado inválido.");
        }
        return "redirect:/admin";
    }

    // ── ACERCA DE ──────────────────────────────────────────────────────

    @GetMapping("/acerca-de")
    public String acercaDe(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "user/acerca-de";
    }
}
