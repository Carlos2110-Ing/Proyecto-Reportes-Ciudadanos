/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.reportes_ciudadanos.controller;

import com.utp.reportes_ciudadanos.model.Reporte;
import com.utp.reportes_ciudadanos.service.ReporteService;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
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
@RequestMapping("/reportes")
public class ReporteController {
    private final ReporteService reporteService;

    @Autowired
    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // ── NUEVO REPORTE ──────────────────────────────────────────────────

    @GetMapping("/nuevo")
    public String nuevoForm(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        model.addAttribute("categorias", reporteService.findAllCategorias());
        return "user/nuevo-reporte";
    }

    @PostMapping("/nuevo")
    public String nuevoSubmit(@RequestParam String titulo,
                              @RequestParam String descripcion,
                              @RequestParam Integer categoriaId,
                              @RequestParam String ubicacion,
                              @RequestParam(required = false) Double latitud,
                              @RequestParam(required = false) Double longitud,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer usuarioId = (Integer) session.getAttribute("usuarioId");
        reporteService.crear(titulo, descripcion, categoriaId, ubicacion,
                latitud, longitud, usuarioId);

        redirectAttributes.addFlashAttribute("exito", "¡Reporte enviado correctamente!");
        return "redirect:/dashboard";
    }

    // ── MIS REPORTES ───────────────────────────────────────────────────

    @GetMapping("/mis-reportes")
    public String misReportes(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer usuarioId = (Integer) session.getAttribute("usuarioId");
        model.addAttribute("reportes",    reporteService.findByUsuario(usuarioId));
        model.addAttribute("categorias",  reporteService.findAllCategorias());
        return "user/mis-reportes";
    }

    // ── EDITAR ─────────────────────────────────────────────────────────

    @PostMapping("/editar/{id}")
    public String editar(@PathVariable Integer id,
                         @RequestParam String titulo,
                         @RequestParam String descripcion,
                         @RequestParam Integer categoriaId,
                         @RequestParam String ubicacion,
                         @RequestParam(required = false) Double latitud,
                         @RequestParam(required = false) Double longitud,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer usuarioId = (Integer) session.getAttribute("usuarioId");

        // Verificar que el reporte pertenece al usuario (o es admin)
        Optional<Reporte> opt = reporteService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Reporte no encontrado.");
            return "redirect:/reportes/mis-reportes";
        }
        boolean esAdmin = "ADMIN".equals(session.getAttribute("usuarioRol"));
        if (!opt.get().getUsuarioId().equals(usuarioId) && !esAdmin) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para editar este reporte.");
            return "redirect:/reportes/mis-reportes";
        }

        reporteService.actualizar(id, titulo, descripcion, categoriaId, ubicacion, latitud, longitud);
        redirectAttributes.addFlashAttribute("exito", "Reporte actualizado correctamente.");
        return "redirect:/reportes/mis-reportes";
    }

    // ── ELIMINAR ───────────────────────────────────────────────────────

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Integer usuarioId = (Integer) session.getAttribute("usuarioId");
        boolean esAdmin   = "ADMIN".equals(session.getAttribute("usuarioRol"));

        boolean eliminado = esAdmin
                ? reporteService.eliminar(id)
                : reporteService.eliminarSiEsDueno(id, usuarioId);

        if (eliminado) redirectAttributes.addFlashAttribute("exito", "Reporte eliminado.");
        else           redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el reporte.");

        String redirect = esAdmin ? "/admin" : "/reportes/mis-reportes";
        return "redirect:" + redirect;
    }
}
